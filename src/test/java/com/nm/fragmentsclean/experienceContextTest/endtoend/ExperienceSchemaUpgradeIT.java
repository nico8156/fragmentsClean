package com.nm.fragmentsclean.experienceContextTest.endtoend;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers.AbstractBaseE2E;
import java.sql.Connection;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class ExperienceSchemaUpgradeIT extends AbstractBaseE2E {
  @Autowired DataSource dataSource;

  @Test
  void additive_migration_is_idempotent_and_unblocks_legacy_deletions() throws Exception {
    String schema = "experience_upgrade_" + UUID.randomUUID().toString().replace("-", "");
    try (Connection connection = dataSource.getConnection();
        var statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA " + schema);
      statement.execute("SET search_path TO " + schema);
      statement.execute(
          "CREATE TABLE coffees(id UUID PRIMARY KEY, archived_at TIMESTAMPTZ, publication_status TEXT NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL)");
      statement.execute(
          "CREATE TABLE app_users(id UUID PRIMARY KEY, display_name TEXT NOT NULL, avatar_url TEXT, updated_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL)");
      statement.execute(
          "CREATE TABLE account_deletion_processes(request_id UUID PRIMARY KEY, status TEXT NOT NULL, acknowledgements TEXT NOT NULL)");
      statement.execute(
          "INSERT INTO account_deletion_processes VALUES('11111111-1111-4111-8111-111111111111','IN_PROGRESS','')");

      var migration = new ClassPathResource("db/release/2026-09-11-experience-text.sql");
      ScriptUtils.executeSqlScript(connection, migration);
      ScriptUtils.executeSqlScript(connection, migration);

      assertThat(tableExists(statement, "experiences")).isTrue();
      assertThat(tableExists(statement, "experience_views")).isTrue();
      assertThat(columnExists(statement, "experience_reports_projection", "moderation_version"))
          .isTrue();
      assertThat(value(statement, "SELECT acknowledgements FROM account_deletion_processes"))
          .isEqualTo("EXPERIENCE");
    } finally {
      try (Connection connection = dataSource.getConnection();
          var statement = connection.createStatement()) {
        statement.execute("SET search_path TO public");
        statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
      }
    }
  }

  private static boolean tableExists(java.sql.Statement statement, String table) throws Exception {
    return value(
            statement,
            "SELECT count(*) FROM information_schema.tables WHERE table_schema=current_schema() AND table_name='"
                + table
                + "'")
        .equals("1");
  }

  private static boolean columnExists(java.sql.Statement statement, String table, String column)
      throws Exception {
    return value(
            statement,
            "SELECT count(*) FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='"
                + table
                + "' AND column_name='"
                + column
                + "'")
        .equals("1");
  }

  private static String value(java.sql.Statement statement, String query) throws Exception {
    try (var result = statement.executeQuery(query)) {
      assertThat(result.next()).isTrue();
      return result.getString(1);
    }
  }
}
