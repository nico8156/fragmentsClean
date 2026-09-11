package com.nm.fragmentsclean.sharedKernelTest.integration;

import com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers.AbstractBaseE2E;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CommandReceiptSchemaUpgradeIT extends AbstractBaseE2E {
    @Autowired DataSource dataSource;

    @Test
    void additive_release_script_upgrades_a_legacy_command_status_table() throws Exception {
        String schema = "receipt_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET search_path TO " + schema);
            statement.execute("""
                    CREATE TABLE command_status (
                        command_id UUID PRIMARY KEY,
                        status VARCHAR(32) NOT NULL,
                        updated_at TIMESTAMPTZ NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE TABLE outbox_events (
                        id BIGSERIAL PRIMARY KEY,
                        payload_json TEXT NOT NULL
                    )
                    """);
            UUID commandId = UUID.fromString("11111111-1111-4111-8111-111111111111");
            UUID requesterId = UUID.fromString("22222222-2222-4222-8222-222222222222");
            try (var insertReceipt = connection.prepareStatement(
                    "INSERT INTO command_status(command_id,status,updated_at) VALUES (?,'APPLIED',now())");
                 var insertEvidence = connection.prepareStatement(
                    "INSERT INTO outbox_events(payload_json) VALUES (?)")) {
                insertReceipt.setObject(1, commandId);
                insertReceipt.executeUpdate();
                insertEvidence.setString(1, "{\"commandId\":\"" + commandId + "\",\"userId\":\"" + requesterId + "\"}");
                insertEvidence.executeUpdate();
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/release/2026-09-11-command-receipts.sql"));

            var columns = new java.util.HashSet<String>();
            try (var result = statement.executeQuery("""
                    SELECT column_name FROM information_schema.columns
                    WHERE table_schema = current_schema() AND table_name = 'command_status'
                    """)) {
                while (result.next()) columns.add(result.getString(1));
            }
            assertThat(columns).contains("requester_id", "command_type", "fingerprint", "rejection_code");
            try (var verify = connection.prepareStatement(
                    "SELECT requester_id, command_type, fingerprint FROM command_status WHERE command_id = ?")) {
                verify.setObject(1, commandId);
                try (var result = verify.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getObject("requester_id", UUID.class)).isEqualTo(requesterId);
                    assertThat(result.getString("command_type")).isNull();
                    assertThat(result.getString("fingerprint")).isNull();
                }
            }
        } finally {
            try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("SET search_path TO public");
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }
}
