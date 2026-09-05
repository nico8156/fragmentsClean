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
    void deployment_installs_a_daily_backup_and_runs_it_before_schema_mutation() throws IOException {
        String deployment = Files.readString(RUNTIME.resolve("deploy-via-ssm.sh"));
        String timer = Files.readString(RUNTIME.resolve("fragments-postgres-backup.timer"));

        assertThat(deployment)
                .contains("systemctl enable --now fragments-postgres-backup.timer")
                .contains("systemctl start fragments-postgres-backup.service")
                .contains("< \"$runtime_root/db/schema.sql\"");
        assertThat(deployment.indexOf("systemctl start fragments-postgres-backup.service"))
                .isLessThan(deployment.indexOf("< \"$runtime_root/db/schema.sql\""));
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
                .contains("sha256sum");
        assertThat(restore)
                .contains("expected_prefix=")
                .contains("pg_restore")
                .contains("fragments_restore_drill_")
                .contains("drop_drill_database")
                .doesNotContain("dropdb --username \"$POSTGRES_USER\" \"$POSTGRES_DB\"");
    }
}
