package com.nm.fragmentsclean.sharedKernel.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.JdbcAccountErasureBarrier;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class AccountErasureBarrierIT {
  private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.1");
  private static JdbcTemplate jdbc;
  private static JdbcAccountErasureBarrier barrier;

  @BeforeAll
  static void database() {
    postgres.start();
    var dataSource =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    jdbc = new JdbcTemplate(dataSource);
    barrier = new JdbcAccountErasureBarrier(jdbc, new DataSourceTransactionManager(dataSource));
    jdbc.execute(
        """
        CREATE TABLE account_erasure_barriers (
          context_name varchar(32) NOT NULL, user_id uuid NOT NULL, status varchar(16) NOT NULL,
          request_id uuid, erased_at timestamptz, created_at timestamptz NOT NULL,
          updated_at timestamptz NOT NULL, PRIMARY KEY(context_name,user_id))
        """);
    jdbc.execute("CREATE TABLE protected_user_projection(user_id uuid PRIMARY KEY, value text)");
  }

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM protected_user_projection");
    jdbc.update("DELETE FROM account_erasure_barriers");
  }

  @AfterAll
  static void stop() {
    postgres.stop();
  }

  @Test
  void delayed_and_duplicate_events_cannot_recreate_data_after_erasure() {
    var userId = UUID.randomUUID();
    erase(userId);

    assertThat(insertIfActive(userId, "delayed")).isFalse();
    assertThat(insertIfActive(userId, "duplicate")).isFalse();
    assertThat(count(userId)).isZero();
  }

  @Test
  void event_that_wins_the_lock_is_removed_before_erasure_commits() throws Exception {
    var userId = UUID.randomUUID();
    var mutationStarted = new CountDownLatch(1);
    var releaseMutation = new CountDownLatch(1);
    try (var workers = Executors.newFixedThreadPool(2)) {
      var mutation =
          workers.submit(
              () ->
                  barrier.ifActive(
                      AccountErasureBarrier.Scope.SOCIAL,
                      userId,
                      () -> {
                        mutationStarted.countDown();
                        await(releaseMutation);
                        jdbc.update(
                            "INSERT INTO protected_user_projection(user_id,value) VALUES (?,?)",
                            userId,
                            "before-erasure");
                      }));
      assertThat(mutationStarted.await(5, TimeUnit.SECONDS)).isTrue();
      var erasure = workers.submit(() -> erase(userId));

      Thread.sleep(100);
      assertThat(erasure.isDone()).isFalse();
      releaseMutation.countDown();
      assertThat(mutation.get(5, TimeUnit.SECONDS)).isTrue();
      erasure.get(5, TimeUnit.SECONDS);
    }

    assertThat(count(userId)).isZero();
    assertThat(insertIfActive(userId, "late")).isFalse();
  }

  private static boolean insertIfActive(UUID userId, String value) {
    return barrier.ifActive(
        AccountErasureBarrier.Scope.SOCIAL,
        userId,
        () ->
            jdbc.update(
                "INSERT INTO protected_user_projection(user_id,value) VALUES (?,?) ON CONFLICT(user_id) DO UPDATE SET value=excluded.value",
                userId,
                value));
  }

  private static void erase(UUID userId) {
    barrier.erase(
        AccountErasureBarrier.Scope.SOCIAL,
        userId,
        UUID.randomUUID(),
        Instant.parse("2026-09-17T10:00:00Z"),
        () -> jdbc.update("DELETE FROM protected_user_projection WHERE user_id=?", userId));
  }

  private static int count(UUID userId) {
    return jdbc.queryForObject(
        "SELECT COUNT(*) FROM protected_user_projection WHERE user_id=?", Integer.class, userId);
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Latch timeout");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
