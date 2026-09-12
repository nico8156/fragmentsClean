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
                .doesNotContain("dropdb --username \"$POSTGRES_USER\" \"$POSTGRES_DB\"");
    }
}
