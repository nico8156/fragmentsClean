package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

/** Real deployment shell; external AWS/Docker/systemd commands are technical-boundary fakes. */
class DeploymentSafetyIT {
    private static final PostgreSQLContainer<?> SANDBOX = new PostgreSQLContainer<>("postgres:15-alpine");
    private static final Path RUNTIME = Path.of("infra/aws/compose/platform/staging/fragments");
    private static final String REVISION = "a".repeat(40);
    private static final String IMAGE = "851725375299.dkr.ecr.eu-west-3.amazonaws.com/fragments/staging/backend:sha-" + REVISION;
    @TempDir Path temporary;

    @BeforeAll static void start() throws Exception {
        SANDBOX.start();
        SANDBOX.copyFileToContainer(MountableFile.forHostPath(RUNTIME), "/fixture/" + RUNTIME);
        SANDBOX.copyFileToContainer(MountableFile.forHostPath(Path.of("src/main/resources/db/release")), "/fixture/src/main/resources/db/release");
        SANDBOX.copyFileToContainer(MountableFile.forClasspathResource("deployment/fake-boundary.sh"), "/tmp/fake-boundary.sh");
        var setup = SANDBOX.execInContainer("bash", "-c", """
                mkdir -p /tmp/fake-bin /srv/fragments/staging /etc/systemd/system
                chmod 700 /tmp/fake-boundary.sh
                for tool in aws curl docker systemctl flock; do ln -s /tmp/fake-boundary.sh /tmp/fake-bin/$tool; done
                """);
        assertThat(setup.getExitCode()).as(setup.getStderr()).isZero();
    }
    @AfterAll static void stop() { SANDBOX.stop(); }

    @Test void missing_ssm_keeps_live_environment_and_backend_untouched() throws Exception {
        var result = deploy("missing_ssm");
        assertThat(result.getExitCode()).isNotZero();
        assertThat(trace()).doesNotContain("docker stop", "docker exec", "systemctl start");
        assertThat(environment()).isEqualTo("ORIGINAL_ENV=true\n");
    }

    @Test void backup_failure_restarts_only_the_previous_backend_without_migration() throws Exception {
        var result = deploy("backup_failure");
        assertThat(result.getExitCode()).isNotZero();
        var trace = trace();
        assertThat(trace).contains("docker stop --time 60 staging-fragments-backend-1",
                "docker start staging-fragments-backend-1").doesNotContain("docker exec", "compose up");
        assertThat(trace.indexOf("docker stop")).isLessThan(trace.indexOf("systemctl start fragments-postgres-backup.service"));
        assertThat(environment()).isEqualTo("ORIGINAL_ENV=true\n");
    }

    @Test void uncertain_migration_never_restarts_old_image_or_overwrites_live_configuration() throws Exception {
        var result = deploy("migration_failure");
        assertThat(result.getExitCode()).isNotZero();
        assertThat(trace()).contains("docker exec").doesNotContain("docker start", "compose up");
        assertThat(result.getStderr()).contains("no automatic image rollback");
        assertThat(environment()).isEqualTo("ORIGINAL_ENV=true\n");
    }

    @Test void a_retry_never_revives_a_previously_stopped_backend_when_backup_fails() throws Exception {
        var result = deploy("backup_failure_stopped");
        assertThat(result.getExitCode()).isNotZero();
        assertThat(trace()).doesNotContain("docker start", "docker exec", "compose up");
        assertThat(result.getStderr()).contains("Previously stopped backend remains stopped");
    }

    @Test void success_orders_stop_backup_migration_then_candidate_start() throws Exception {
        var result = deploy("success");
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        var trace = trace();
        assertThat(trace.indexOf("docker stop")).isLessThan(trace.indexOf("systemctl start fragments-postgres-backup.service"));
        assertThat(trace.indexOf("systemctl start fragments-postgres-backup.service")).isLessThan(trace.indexOf("docker exec"));
        assertThat(trace.indexOf("docker exec")).isLessThan(trace.indexOf("docker compose up"));
        assertThat(trace).doesNotContain("docker rm", "compose down", "schema.sql");
        assertThat(environment()).contains("BACKEND_IMAGE=" + IMAGE);
        var sql = SANDBOX.execInContainer("cat", "/tmp/received-migration.psql").getStdout();
        assertThat(sql).contains("release_schema_history", "Migration checksum mismatch").doesNotContain("\\ir ");
    }

    @Test void approval_and_matching_image_revision_are_required_before_external_calls() throws Exception {
        String script = RUNTIME.resolve("deploy-via-ssm.sh").toAbsolutePath().toString();
        for (var args : List.of(List.of("bash", script, IMAGE, REVISION),
                List.of("bash", script, IMAGE, "b".repeat(40), "--approved-staging-release"))) {
            var process = new ProcessBuilder(args).redirectErrorStream(true).start();
            var output = new String(process.getInputStream().readAllBytes());
            assertThat(process.waitFor()).as(output).isEqualTo(2);
        }
    }

    @Test void compose_preserves_multiline_pem_and_literal_secret_characters() throws Exception {
        var fakeAws = temporary.resolve("aws");
        Files.writeString(fakeAws, """
                #!/bin/sh
                case "$*" in
                  *APPLE_PRIVATE_KEY*) printf '%s\\n' '-----BEGIN PRIVATE KEY-----' 'synthetic$payload#only' '-----END PRIVATE KEY-----' ;;
                  *AUTH_JWT_SECRET*) printf "synthetic's\\044literal#value" ;;
                  *) printf synthetic ;;
                esac
                """);
        assertThat(fakeAws.toFile().setExecutable(true)).isTrue();
        var process = new ProcessBuilder("bash", RUNTIME.resolve("bootstrap-runtime.sh").toAbsolutePath().toString(), IMAGE, temporary.toString());
        process.environment().put("PATH", temporary + ":" + System.getenv("PATH"));
        var rendered = process.redirectErrorStream(true).start();
        String renderOutput = new String(rendered.getInputStream().readAllBytes());
        assertThat(rendered.waitFor()).as(renderOutput).isZero();
        Files.copy(RUNTIME.resolve("docker-compose.yml"), temporary.resolve("docker-compose.yml"));
        var compose = new ProcessBuilder("docker", "compose", "--env-file", temporary.resolve(".env").toString(),
                "-f", temporary.resolve("docker-compose.yml").toString(), "config", "--format", "json")
                .redirectErrorStream(true).start();
        String output = new String(compose.getInputStream().readAllBytes());
        assertThat(compose.waitFor()).as(output).isZero();
        // `compose config` escapes dollars for serialisation. Inspect the actual
        // environment of a never-started, networkless container to verify bytes.
        String name = "fragments-env-test-" + UUID.randomUUID();
        Path probe = temporary.resolve("probe.yml");
        Files.writeString(probe, "services:\n  probe:\n    image: postgres:15-alpine\n    container_name: " + name
                + "\n    network_mode: none\n    read_only: true\n    env_file: .env\n");
        try {
            var create = new ProcessBuilder("docker", "compose", "-f", probe.toString(), "create", "--pull", "never")
                    .redirectErrorStream(true).start();
            String createOutput = new String(create.getInputStream().readAllBytes());
            assertThat(create.waitFor()).as(createOutput).isZero();
            var inspect = new ProcessBuilder("docker", "inspect", name, "--format", "{{json .Config.Env}}")
                    .redirectErrorStream(true).start();
            var env = new com.fasterxml.jackson.databind.ObjectMapper().readTree(inspect.getInputStream());
            assertThat(inspect.waitFor()).isZero();
            var values = new java.util.ArrayList<String>();
            env.forEach(value -> values.add(value.asText()));
            assertThat(values).contains(
                    "APPLE_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nsynthetic$payload#only\n-----END PRIVATE KEY-----",
                    "AUTH_JWT_SECRET=synthetic's$literal#value");
        } finally {
            var remove = new ProcessBuilder("docker", "rm", "-v", name).redirectErrorStream(true).start();
            remove.getInputStream().readAllBytes();
            assertThat(remove.waitFor()).isZero();
        }
    }

    private static Container.ExecResult deploy(String scenario) throws Exception {
        SANDBOX.copyFileToContainer(Transferable.of("ORIGINAL_ENV=true\n"), "/srv/fragments/staging/.env");
        SANDBOX.copyFileToContainer(Transferable.of(""), "/tmp/deploy-trace");
        return SANDBOX.execInContainer("env", "PATH=/tmp/fake-bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
                "SCENARIO=" + scenario, "bash", "/fixture/" + RUNTIME + "/deploy-via-ssm.sh", IMAGE, REVISION, "--approved-staging-release");
    }

    private static String trace() throws Exception { return SANDBOX.execInContainer("cat", "/tmp/deploy-trace").getStdout(); }
    private static String environment() throws Exception { return SANDBOX.execInContainer("cat", "/srv/fragments/staging/.env").getStdout(); }
}
