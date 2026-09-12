package com.nm.fragmentsclean.ticketContext.integration.adapters.secondary.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;

class TicketVerificationSchemaUpgradeIT {
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.1");

    @BeforeAll static void database() { postgres.start(); }
    @AfterAll static void stop() { postgres.stop(); }

    @Test void additive_migration_upgrades_an_existing_inbox_and_is_reentrant() throws Exception {
        try (var connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                var statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE inbox_messages (
                      id bigserial primary key,
                      destination varchar(255) not null,
                      event_id varchar(50) not null,
                      event_type varchar(255) not null,
                      event_version integer not null,
                      received_at timestamptz not null,
                      processed_at timestamptz null,
                      status varchar(32) not null,
                      error_message text null,
                      unique(destination,event_id)
                    )
                    """);
            var migration = new ClassPathResource("db/release/2026-09-12-ticket-verification-jobs.sql");

            ScriptUtils.executeSqlScript(connection, migration);
            ScriptUtils.executeSqlScript(connection, migration);

            try (var columns = statement.executeQuery("""
                    SELECT count(*)
                    FROM information_schema.columns
                    WHERE table_schema='public'
                      AND table_name='inbox_messages'
                      AND column_name='lease_until'
                    """)) {
                assertThat(columns.next()).isTrue();
                assertThat(columns.getInt(1)).isEqualTo(1);
            }
            try (var tables = statement.executeQuery("SELECT to_regclass('public.ticket_verification_jobs')")) {
                assertThat(tables.next()).isTrue();
                assertThat(tables.getString(1)).isEqualTo("ticket_verification_jobs");
            }
            try (var indexes = statement.executeQuery("""
                    SELECT count(*)
                    FROM pg_indexes
                    WHERE schemaname='public'
                      AND indexname IN (
                        'uq_ticket_verification_job_command',
                        'idx_ticket_verification_job_due',
                        'idx_ticket_verification_job_user',
                        'idx_inbox_messages_claim_lease'
                      )
                    """)) {
                assertThat(indexes.next()).isTrue();
                assertThat(indexes.getInt(1)).isEqualTo(4);
            }
        }
    }
}
