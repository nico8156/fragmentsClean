package com.nm.fragmentsclean.userApplicationContextTest.integration;

import com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers.AbstractBaseE2E;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PassPolicySchemaUpgradeIT extends AbstractBaseE2E {
    @Autowired DataSource dataSource;

    @Test
    void migration_preserves_legacy_achievement_and_deduplicates_exact_ocr_receipts() throws Exception {
        String schema = "pass_v2_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        UUID user = UUID.randomUUID();
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET search_path TO " + schema);
            statement.execute("""
                    CREATE TABLE tickets(
                      ticket_id uuid primary key,user_id uuid not null,status varchar(32) not null,
                      ocr_text text,created_at timestamptz not null,updated_at timestamptz not null,version bigint not null)
                    """);
            statement.execute("CREATE TABLE social_comments_projection(author_id uuid,deleted_at timestamptz,moderation text)");
            statement.execute("CREATE TABLE social_likes_projection(user_id uuid,active boolean)");
            try (var insert = connection.prepareStatement("""
                    INSERT INTO tickets(ticket_id,user_id,status,ocr_text,created_at,updated_at,version)
                    VALUES (?,?, 'CONFIRMED',?,now(),now(),1)
                    """)) {
                for (String ocr : new String[]{"A TOTAL 4", "B TOTAL 5", "C TOTAL 6", "  a  total 4  "}) {
                    insert.setObject(1, UUID.randomUUID());
                    insert.setObject(2, user);
                    insert.setString(3, ocr);
                    insert.addBatch();
                }
                insert.executeBatch();
            }

            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/release/2026-09-11-pass-policy-v2.sql"));

            assertThat(count(statement, "ticket_submission_fingerprints")).isEqualTo(3);
            assertThat(count(statement, "pass_ticket_contributions")).isEqualTo(3);
            try (var result = statement.executeQuery("""
                    SELECT policy_version,validated_tickets,acquired_levels
                    FROM user_pass_projection
                    """)) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt("policy_version")).isEqualTo(2);
                assertThat(result.getInt("validated_tickets")).isEqualTo(3);
                assertThat(result.getString("acquired_levels")).isEqualTo("COFFEE_TASTER");
            }
        } finally {
            try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
                statement.execute("SET search_path TO public");
                statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            }
        }
    }

    private int count(java.sql.Statement statement, String table) throws Exception {
        try (var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            result.next();
            return result.getInt(1);
        }
    }
}
