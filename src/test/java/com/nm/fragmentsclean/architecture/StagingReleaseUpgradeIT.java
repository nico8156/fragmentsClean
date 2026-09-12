package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

/** Uses schema metadata from staging, but exclusively synthetic application rows. */
class StagingReleaseUpgradeIT {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");
    private static final String USER = "11111111-1111-4111-8111-111111111111";
    private static final String COFFEE = "22222222-2222-4222-8222-222222222222";
    private static final String COMMAND = "33333333-3333-4333-8333-333333333333";
    private static final Path RELEASE = Path.of("src/main/resources/db/release");

    @BeforeAll static void start() { POSTGRES.start(); }
    @AfterAll static void stop() { POSTGRES.stop(); }

    @Test void upgrades_observed_schema_preserves_legacy_and_replays_without_resetting_new_state() throws Exception {
        String database = database();
        try (var connection = connect(database)) {
            baseline(connection);
            seed(connection);
            var beforeColumns = columns(connection);
            var beforeConstraints = constraints(connection);
            var beforeIndexes = indexes(connection);
            var result = upgrade(database, false);
            assertThat(result.getExitCode()).as(result.getStderr()).isZero();

            assertThat(rows(connection, "SELECT display_name FROM app_users")).containsExactly("Existing visitor");
            assertThat(rows(connection, "SELECT payload_json FROM admin_studio_articles")).containsExactly("{\"legacy\":true}");
            assertThat(rows(connection, "SELECT requester_id::text FROM command_status WHERE command_id='" + COMMAND + "'"))
                    .containsExactly(USER);
            assertThat(rows(connection, "SELECT count(*)::text FROM command_status WHERE requester_id IS NULL"))
                    .containsExactly("2"); // No evidence, or conflicting retained event owners.
            assertThat(rows(connection, "SELECT count(*)::text FROM outbox_events")).containsExactly("3");
            assertThat(rows(connection, "SELECT count(*)::text FROM ticket_verification_jobs")).containsExactly("0");
            assertThat(rows(connection, "SELECT count(*)::text FROM tickets")).containsExactly("11");
            assertThat(rows(connection, "SELECT validated_tickets::text, acquired_levels FROM user_pass_projection"))
                    .containsExactly("10|COFFEE_TASTER,URBAN_EXPLORER,SOCIAL_BEAN,FRAGMENTS_MASTER");
            assertThat(rows(connection, "SELECT count(*)::text FROM ticket_submission_fingerprints")).containsExactly("10");
            assertThat(rows(connection, "SELECT active::text FROM experience_coffee_references")).containsExactly("true");
            assertThat(rows(connection, "SELECT display_name FROM experience_user_profiles")).containsExactly("Existing visitor");
            assertThat(rows(connection, "SELECT count(DISTINCT history_position)::text FROM ticket_status_projection"))
                    .containsExactly("11");
            var receipt = rows(connection, "SELECT version,checksum,source_revision,applied_at::text FROM release_schema_history");
            assertThat(receipt).hasSize(1);
            assertThat(receipt.getFirst()).startsWith("app-store-2026-09|");

            String fresh = database();
            try (var target = connect(fresh)) {
                ScriptUtils.executeSqlScript(target, new ClassPathResource("schema.sql"));
                assertThat(columns(connection)).containsAll(columns(target));
                // pg_dump reparsing can render existing text-array casts differently.
                // Preserve every existing constraint/access path exactly, and compare
                // modified/new tables against bootstrap. Unique constraints are covered
                // by the unique indexes, including history_position's explicit index.
                assertThat(constraints(connection)).containsAll(beforeConstraints);
                assertThat(indexes(connection)).containsAll(beforeIndexes);
                var changedTables = columns(target).stream().filter(row -> !beforeColumns.contains(row))
                        .map(row -> row.split("\\|", 2)[0]).collect(Collectors.toSet());
                assertThat(constraints(connection)).containsAll(constraints(target).stream()
                        .filter(row -> changedTables.contains(row.split("\\|", 2)[0]))
                        .filter(row -> !row.contains("|UNIQUE (")).toList());
                assertThat(indexes(connection)).containsAll(indexes(target).stream()
                        .filter(row -> changedTables.contains(row.split("\\|", 2)[0])).toList());
            }

            try (var statement = connection.createStatement()) {
                statement.execute("UPDATE experience_user_profiles SET display_name='New profile', version=42");
                statement.execute("UPDATE user_pass_projection SET version=42, published_experiences=7");
                // A replay must skip the one-time receipt backfill, not merely rely on upserts.
                statement.execute("UPDATE command_status SET requester_id=NULL WHERE command_id='" + COMMAND + "'");
            }
            result = upgrade(database, false);
            assertThat(result.getExitCode()).as(result.getStderr()).isZero();
            assertThat(rows(connection, "SELECT display_name, version::text FROM experience_user_profiles"))
                    .containsExactly("New profile|42");
            assertThat(rows(connection, "SELECT published_experiences::text, version::text FROM user_pass_projection"))
                    .containsExactly("7|42");
            assertThat(rows(connection, "SELECT count(*)::text FROM tickets")).containsExactly("11");
            assertThat(rows(connection, "SELECT version,checksum,source_revision,applied_at::text FROM release_schema_history"))
                    .isEqualTo(receipt);
            assertThat(rows(connection, "SELECT count(*)::text FROM command_status WHERE requester_id IS NOT NULL"))
                    .containsExactly("0");
        }
    }

    @Test void failure_in_the_last_fragment_rolls_back_all_previous_ddl_and_backfills() throws Exception {
        String database = database();
        try (var connection = connect(database)) {
            baseline(connection);
            seed(connection);
            List<String> before = columns(connection);
            var result = upgrade(database, true);
            assertThat(result.getExitCode()).as(result.getStderr()).isNotZero();
            assertThat(result.getStderr()).contains("intentional_release_failure");
            assertThat(columns(connection)).isEqualTo(before);
            assertThat(rows(connection, "SELECT count(*)::text FROM tickets")).containsExactly("11");
            assertThat(rows(connection, "SELECT payload_json FROM admin_studio_articles")).containsExactly("{\"legacy\":true}");
        }
    }

    @Test void missing_baseline_is_rejected_without_leaving_partial_schema() throws Exception {
        String database = database();
        var result = upgrade(database, false);
        assertThat(result.getExitCode()).as(result.getStderr()).isNotZero();
        try (var connection = connect(database)) {
            assertThat(columns(connection)).isEmpty();
        }
    }

    @Test void changed_checksum_is_rejected_before_replaying_any_sql() throws Exception {
        String database = database();
        try (var connection = connect(database)) {
            baseline(connection);
            assertThat(upgrade(database, false).getExitCode()).isZero();
            var before = rows(connection, "SELECT checksum,applied_at::text FROM release_schema_history");
            var result = upgrade(database, true);
            assertThat(result.getExitCode()).isNotZero();
            assertThat(result.getStdout()).contains("Migration checksum mismatch");
            assertThat(result.getStderr()).doesNotContain("intentional_release_failure");
            assertThat(rows(connection, "SELECT checksum,applied_at::text FROM release_schema_history")).isEqualTo(before);
        }
    }

    @Test void concurrent_deliveries_commit_one_history_entry_and_skip_the_other() throws Exception {
        String database = database();
        try (var connection = connect(database)) { baseline(connection); }
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> upgrade(database, false));
            var second = executor.submit(() -> upgrade(database, false));
            var a = first.get(30, java.util.concurrent.TimeUnit.SECONDS);
            var b = second.get(30, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(a.getExitCode()).as(a.getStderr()).isZero();
            assertThat(b.getExitCode()).as(b.getStderr()).isZero();
            assertThat(a.getStdout() + b.getStdout()).containsOnlyOnce("Migration already applied; backfills skipped");
        }
        try (var connection = connect(database)) {
            assertThat(rows(connection, "SELECT count(*)::text FROM release_schema_history")).containsExactly("1");
        }
    }

    private static String database() throws Exception {
        String name = "release_" + UUID.randomUUID().toString().replace("-", "");
        var result = POSTGRES.execInContainer("createdb", "-U", POSTGRES.getUsername(), name);
        assertThat(result.getExitCode()).as(result.getStderr()).isZero();
        return name;
    }

    private static Connection connect(String database) throws Exception {
        return DriverManager.getConnection("jdbc:postgresql://" + POSTGRES.getHost() + ":"
                + POSTGRES.getMappedPort(5432) + "/" + database, POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static void baseline(Connection connection) throws Exception {
        ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/staging-2026-09-12-schema-only.sql"));
        try (var statement = connection.createStatement()) { statement.execute("SET search_path TO public"); }
    }

    private static Container.ExecResult upgrade(String database, boolean failAtEnd) throws Exception {
        String directory = "/tmp/" + database + "_" + UUID.randomUUID();
        POSTGRES.copyFileToContainer(MountableFile.forHostPath(RELEASE), directory);
        if (failAtEnd) {
            String file = "2026-09-12-ticket-verification-jobs.sql";
            byte[] sql = (Files.readString(RELEASE.resolve(file)) + "\nSELECT intentional_release_failure;\n")
                    .getBytes(StandardCharsets.UTF_8);
            POSTGRES.copyFileToContainer(Transferable.of(sql), directory + "/" + file);
        }
        POSTGRES.copyFileToContainer(MountableFile.forHostPath(Path.of(
                "infra/aws/compose/platform/staging/fragments/render-release-migration.sh")), directory + "/render.sh");
        var rendered = POSTGRES.execInContainer("bash", "-c", "bash \"$1/render.sh\" \"$1\" \"$2\" > \"$1/bundle.psql\"",
                "render", directory, "a".repeat(40));
        assertThat(rendered.getExitCode()).as(rendered.getStderr()).isZero();
        return POSTGRES.execInContainer("psql", "-X", "-v", "ON_ERROR_STOP=1", "-U", POSTGRES.getUsername(),
                "-d", database, "-f", directory + "/bundle.psql");
    }

    private static List<String> columns(Connection connection) throws Exception {
        return rows(connection, """
                SELECT table_name, column_name, udt_name, is_nullable,
                       coalesce(character_maximum_length::text,''), is_identity
                FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name,column_name
                """);
    }

    private static List<String> constraints(Connection connection) throws Exception {
        return rows(connection, """
                SELECT rel.relname, pg_get_constraintdef(c.oid)
                FROM pg_constraint c JOIN pg_class rel ON rel.oid=c.conrelid
                JOIN pg_namespace ns ON ns.oid=rel.relnamespace
                WHERE ns.nspname='public' ORDER BY 1,2
                """);
    }

    private static List<String> indexes(Connection connection) throws Exception {
        // Compare access paths, not auto-generated names of equivalent uniqueness constraints.
        return rows(connection, """
                SELECT tablename, (indexdef LIKE 'CREATE UNIQUE%')::text, substring(indexdef from ' USING .*')
                FROM pg_indexes WHERE schemaname='public' ORDER BY 1,2
                """);
    }

    private static List<String> rows(Connection connection, String query) throws Exception {
        try (var statement = connection.createStatement(); var result = statement.executeQuery(query)) {
            var rows = new ArrayList<String>();
            while (result.next()) {
                var columns = new ArrayList<String>();
                for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) columns.add(result.getString(i));
                rows.add(String.join("|", columns));
            }
            return rows;
        }
    }

    private static void seed(Connection connection) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO auth_users(id,provider,provider_user_id,email,email_verified,last_login_at)
                    VALUES ('%1$s','GOOGLE','synthetic','existing@example.test',true,now());
                    INSERT INTO app_users(id,auth_user_id,display_name,created_at) VALUES ('%1$s','%1$s','Existing visitor',now());
                    INSERT INTO coffees(id,name,lat,lon,version,updated_at) VALUES ('%2$s','Synthetic cafe',48,2,1,now());
                    INSERT INTO admin_studio_articles(article_id,status,payload_json,created_at,updated_at)
                    VALUES ('%2$s','DRAFT','{"legacy":true}',now(),now());
                    INSERT INTO tickets(ticket_id,user_id,status,ocr_text,currency,created_at,updated_at,version)
                    SELECT md5('ticket-' || n)::uuid, '%1$s', 'CONFIRMED', 'receipt-' || least(n,10), 'EUR',
                           now() + n * interval '1 second', now(), 1 FROM generate_series(1,11) n;
                    INSERT INTO ticket_status_projection(ticket_id,user_id,status,version,occurred_at)
                    SELECT ticket_id,user_id,status,version,updated_at FROM tickets;
                    INSERT INTO social_comments_projection(id,target_id,author_id,body,created_at,moderation,version)
                    SELECT md5('comment-' || n)::uuid,'%2$s','%1$s','Synthetic comment',now(),'PUBLISHED',1
                    FROM generate_series(1,5) n;
                    INSERT INTO social_likes_projection(like_id,target_id,user_id,active,updated_at,version)
                    SELECT md5('like-' || n)::uuid,'%2$s','%1$s',true,now(),1 FROM generate_series(1,5) n;
                    INSERT INTO command_status(command_id,status,updated_at)
                    VALUES ('%3$s','APPLIED',now()), (md5('unknown')::uuid,'APPLIED',now()),
                           ('44444444-4444-4444-8444-444444444444','APPLIED',now());
                    INSERT INTO outbox_events(event_id,event_type,aggregate_type,aggregate_id,stream_key,payload_json,
                                              occurred_at,created_at,status)
                    VALUES ('synthetic-1','app.user.profile_updated','AppUser','%1$s','user:synthetic',
                            '{"commandId":"%3$s","userId":"%1$s"}',now(),now(),'SENT'),
                           ('synthetic-2','app.user.profile_updated','AppUser','%1$s','user:synthetic',
                            '{"commandId":"44444444-4444-4444-8444-444444444444","userId":"%1$s"}',now(),now(),'SENT'),
                           ('synthetic-3','app.user.profile_updated','AppUser','%2$s','user:synthetic',
                            '{"commandId":"44444444-4444-4444-8444-444444444444","userId":"%2$s"}',now(),now(),'SENT');
                    """.formatted(USER, COFFEE, COMMAND));
        }
    }
}
