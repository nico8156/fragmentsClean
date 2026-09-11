package com.nm.fragmentsclean.userApplicationContextTest.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers.AbstractBaseE2E;
import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class AccountLifecycleSchemaUpgradeIT extends AbstractBaseE2E {
  @Autowired DataSource dataSource;

  @Test
  void additive_migration_preserves_existing_accounts_and_is_idempotent() throws Exception {
    String schema = "account_lifecycle_" + UUID.randomUUID().toString().replace("-", "");
    UUID userId = UUID.randomUUID();
    try (Connection connection = dataSource.getConnection();
        var statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA " + schema);
      statement.execute("SET search_path TO " + schema);
      statement.execute("CREATE TABLE auth_users(id UUID PRIMARY KEY, email TEXT NOT NULL)");
      statement.execute("CREATE TABLE app_users(id UUID PRIMARY KEY, display_name TEXT NOT NULL)");
      try (var auth = connection.prepareStatement("INSERT INTO auth_users(id,email) VALUES (?,?)");
          var app =
              connection.prepareStatement("INSERT INTO app_users(id,display_name) VALUES (?,?)")) {
        auth.setObject(1, userId);
        auth.setString(2, "existing@example.test");
        auth.executeUpdate();
        app.setObject(1, userId);
        app.setString(2, "Existing User");
        app.executeUpdate();
      }

      var migration = new ClassPathResource("db/release/2026-09-11-account-lifecycle.sql");
      ScriptUtils.executeSqlScript(connection, migration);
      ScriptUtils.executeSqlScript(connection, migration);

      assertThat(value(statement, "SELECT lifecycle_status FROM auth_users")).isEqualTo("ACTIVE");
      assertThat(value(statement, "SELECT lifecycle_status FROM app_users")).isEqualTo("ACTIVE");
      assertThat(count(statement, "account_deletion_processes")).isZero();
      assertThat(count(statement, "auth_provider_credentials")).isZero();
      assertThat(
              value(
                  statement,
                  "SELECT data_type FROM information_schema.columns "
                      + "WHERE table_schema=current_schema() "
                      + "AND table_name='account_deletion_processes' "
                      + "AND column_name='request_id'"))
          .isEqualTo("uuid");
    } finally {
      try (Connection connection = dataSource.getConnection();
          var statement = connection.createStatement()) {
        statement.execute("SET search_path TO public");
        statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
      }
    }
  }

  private static String value(java.sql.Statement statement, String query) throws Exception {
    try (var result = statement.executeQuery(query)) {
      assertThat(result.next()).isTrue();
      return result.getString(1);
    }
  }

  private static int count(java.sql.Statement statement, String table) throws Exception {
    try (var result = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
      assertThat(result.next()).isTrue();
      return result.getInt(1);
    }
  }
}
