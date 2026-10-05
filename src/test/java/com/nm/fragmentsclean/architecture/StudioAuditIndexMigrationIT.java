package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

class StudioAuditIndexMigrationIT {
    @Test void applies_and_replays_indexes_without_changing_existing_rows() throws Exception {
        try (var postgres=new PostgreSQLContainer<>("postgres:15-alpine")) {
            postgres.start();
            postgres.copyFileToContainer(MountableFile.forHostPath(Path.of("src/main/resources/db/release")),"/release");
            postgres.copyFileToContainer(MountableFile.forHostPath(Path.of("infra/aws/compose/platform/staging/fragments/render-release-migration.sh")),"/render.sh");
            var setup=postgres.execInContainer("psql","-U",postgres.getUsername(),"-d",postgres.getDatabaseName(),"-v","ON_ERROR_STOP=1","-c","""
                CREATE TABLE release_schema_history(version TEXT PRIMARY KEY, checksum TEXT, source_revision TEXT, applied_at TIMESTAMPTZ DEFAULT now());
                INSERT INTO release_schema_history(version,checksum,source_revision) VALUES('outbox-delivery-2026-09',repeat('0',64),repeat('a',40));
                CREATE TABLE admin_audit_log(id UUID PRIMARY KEY,actor_user_id UUID NOT NULL,command_id UUID,occurred_at TIMESTAMPTZ NOT NULL);
                INSERT INTO admin_audit_log VALUES('11111111-1111-4111-8111-111111111111','22222222-2222-4222-8222-222222222222',NULL,'2026-10-05T00:00:00Z');
                """);
            assertThat(setup.getExitCode()).as(setup.getStderr()).isZero();
            for(int replay=0;replay<2;replay++) {
                var result=postgres.execInContainer("bash","-c",
                    """
                    bash /render.sh /release "$1" studio-admin-audit-search-2026-10.psql >/migration.psql &&
                    psql -U "$2" -d "$3" -v ON_ERROR_STOP=1 -f /migration.psql
                    """, "render","a".repeat(40),postgres.getUsername(),postgres.getDatabaseName());
                assertThat(result.getExitCode()).as(result.getStderr()).isZero();
            }
            var verified=postgres.execInContainer("psql","-U",postgres.getUsername(),"-d",postgres.getDatabaseName(),"-At","-c","""
                SELECT count(*) FROM admin_audit_log;
                SELECT count(*) FROM pg_indexes WHERE tablename='admin_audit_log' AND indexname IN('ix_admin_audit_actor_cursor','ix_admin_audit_command_cursor');
                SELECT count(*) FROM release_schema_history WHERE version='studio-admin-audit-search-2026-10';
                """);
            assertThat(verified.getExitCode()).isZero();assertThat(verified.getStdout().trim()).isEqualTo("1\n2\n1");
        }
    }
}
