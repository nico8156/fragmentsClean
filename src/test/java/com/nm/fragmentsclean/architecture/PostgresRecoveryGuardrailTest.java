package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PostgresRecoveryGuardrailTest {

    private static final Path RUNTIME =
            Path.of("infra/aws/compose/platform/staging/fragments");

    @Test
    void release_runner_installs_json_and_search_tools_before_the_shared_ci_gate() throws IOException {
        String workflow = Files.readString(Path.of(".github/workflows/deploy-staging-backend.yml"));
        String prerequisites = "sudo apt-get install --yes --no-install-recommends jq ripgrep";
        assertThat(workflow).contains(prerequisites, "bash scripts/verify-backend-ci.sh");
        assertThat(workflow.indexOf(prerequisites))
                .isLessThan(workflow.indexOf("bash scripts/verify-backend-ci.sh"));
        assertThat(Files.readString(Path.of("scripts/test-release.sh")))
                .contains(
                        "ripgrep (rg) is required for release verification.",
                        "jq is required for release verification.");
    }

    @Test
    void deployment_is_manual_approved_and_never_cancelled_by_a_new_push() throws IOException {
        String workflow = Files.readString(Path.of(".github/workflows/deploy-staging-backend.yml"));
        assertThat(workflow).contains("workflow_dispatch:", "approve_staging_release:", "default: false",
                "inputs.approve_staging_release", "cancel-in-progress: false", "--approved-staging-release")
                .doesNotContain("  push:");
    }

    @Test
    void deployment_installs_a_daily_backup_and_runs_it_before_schema_mutation() throws IOException {
        String deployment = Files.readString(RUNTIME.resolve("deploy-via-ssm.sh"));
        String timer = Files.readString(RUNTIME.resolve("fragments-postgres-backup.timer"));

        assertThat(deployment)
                .contains("systemctl enable --now fragments-postgres-backup.timer")
                .contains("systemctl start fragments-postgres-backup.service")
                .contains("< \"$deployment_tmp/migration.psql\"")
                .doesNotContain("< \"$runtime_root/db/schema.sql\"");
        assertThat(deployment.indexOf("systemctl start fragments-postgres-backup.service"))
                .isLessThan(deployment.indexOf("< \"$deployment_tmp/migration.psql\""));
        assertThat(timer)
                .contains("OnCalendar=*-*-* 03:15:00 UTC")
                .contains("Persistent=true")
                .contains("RandomizedDelaySec=15m");
    }

    @Test
    void backup_is_encrypted_and_restore_drill_cannot_target_the_live_database() throws IOException {
        String backup = Files.readString(RUNTIME.resolve("backup-postgres.sh"));
        String restore = Files.readString(RUNTIME.resolve("restore-postgres-drill.sh"));

        assertThat(backup)
                .contains("pg_dump --format=custom")
                .contains("--sse AES256")
                .contains("sha256sum")
                .contains("read_environment_value")
                .contains("label=com.docker.compose.service=fragments-postgres")
                .contains("docker exec -i \"$postgres_container\"")
                .doesNotContain("docker compose exec")
                .doesNotContain("source \"$environment_file\"");
        assertThat(Files.readString(RUNTIME.resolve("deploy-via-ssm.sh")))
                .doesNotContain("journalctl -u fragments-postgres-backup.service")
                .contains("Pre-deployment PostgreSQL backup failed");
        assertThat(restore)
                .contains("expected_prefix=")
                .contains("pg_restore")
                .contains("fragments_restore_drill_")
                .contains("drop_drill_database")
                .contains("replay-account-erasures.sh")
                .contains("read_environment_value")
                .doesNotContain("source \"$environment_file\"")
                .doesNotContain("dropdb --username \"$POSTGRES_USER\" \"$POSTGRES_DB\"");
    }

    @Test
    void account_erasure_replay_is_isolated_idempotent_and_includes_private_objects() throws IOException {
        String replay = Files.readString(RUNTIME.resolve("replay-account-erasures.sh"));
        String sql = Files.readString(RUNTIME.resolve("replay-account-erasures.sql"));

        assertThat(replay)
                .contains("fragments_restore_drill_")
                .contains("ACCOUNT_ERASURE_JOURNAL_S3_BUCKET")
                .contains("aws s3 sync")
                .contains("aws s3api delete-object")
                .contains("aws s3api list-object-versions")
                .contains("user_avatar_media")
                .contains("experience_media")
                .contains("--set ON_ERROR_STOP=1");
        assertThat(sql)
                .contains("BEGIN;", "COMMIT;")
                .contains("DELETE FROM tickets", "DELETE FROM comments", "DELETE FROM experiences")
                .contains("DELETE FROM auth_provider_credentials")
                .contains("UPDATE refresh_tokens SET revoked = true")
                .contains("INSERT INTO account_erasure_barriers")
                .contains("ON CONFLICT(context_name, user_id) DO UPDATE");
    }
}
