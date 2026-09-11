package com.nm.fragmentsclean.ticketContext.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers.AbstractBaseE2E;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketHistorySchemaUpgradeIT extends AbstractBaseE2E {
    @Autowired DataSource dataSource;

    @Test
    void additive_release_script_assigns_stable_positions_to_legacy_projection_rows() throws Exception {
        String schema = "ticket_history_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET search_path TO " + schema);
            statement.execute("""
                    CREATE TABLE ticket_status_projection (
                        ticket_id UUID PRIMARY KEY,
                        user_id UUID NOT NULL,
                        status VARCHAR(32) NOT NULL
                    )
                    """);
            UUID userId = UUID.fromString("11111111-1111-4111-8111-111111111111");
            try (var insert = connection.prepareStatement("""
                    INSERT INTO ticket_status_projection(ticket_id, user_id, status)
                    VALUES (?, ?, 'CONFIRMED'), (?, ?, 'REJECTED')
                    """)) {
                insert.setObject(1, UUID.fromString("22222222-2222-4222-8222-222222222222"));
                insert.setObject(2, userId);
                insert.setObject(3, UUID.fromString("33333333-3333-4333-8333-333333333333"));
                insert.setObject(4, userId);
                insert.executeUpdate();
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/release/2026-09-11-ticket-history.sql"));

            var positions = new HashSet<Long>();
            try (var result = statement.executeQuery("""
                    SELECT history_position
                    FROM ticket_status_projection
                    ORDER BY history_position DESC
                    """)) {
                while (result.next()) positions.add(result.getLong(1));
            }
            assertThat(positions).hasSize(2).doesNotContain(0L);

            try (var indexes = statement.executeQuery("""
                    SELECT indexname
                    FROM pg_indexes
                    WHERE schemaname = current_schema()
                      AND tablename = 'ticket_status_projection'
                    """)) {
                var names = new HashSet<String>();
                while (indexes.next()) names.add(indexes.getString(1));
                assertThat(names).contains(
                        "idx_ticket_status_history_position",
                        "idx_ticket_status_user_history"
                );
            }
        } finally {
            try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("SET search_path TO public");
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
