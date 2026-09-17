package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class JdbcAccountErasureBarrier implements AccountErasureBarrier {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transactions;

  public JdbcAccountErasureBarrier(
      JdbcTemplate jdbc, PlatformTransactionManager transactionManager) {
    this.jdbc = jdbc;
    this.transactions = new TransactionTemplate(transactionManager);
  }

  @Override
  public boolean ifAllActive(Scope scope, Collection<UUID> userIds, Runnable mutation) {
    var orderedIds = normalized(userIds);
    if (orderedIds.isEmpty()) throw new IllegalArgumentException("At least one user id is required");
    return Boolean.TRUE.equals(
        transactions.execute(
            ignored -> {
              orderedIds.forEach(userId -> ensureActive(scope, userId));
              boolean active =
                  orderedIds.stream().allMatch(userId -> lockAndIsActive(scope, userId));
              if (active) mutation.run();
              return active;
            }));
  }

  @Override
  public void erase(
      Scope scope, UUID userId, UUID requestId, Instant erasedAt, Runnable erasure) {
    require(scope, userId);
    if (requestId == null) throw new IllegalArgumentException("requestId is required");
    if (erasedAt == null) throw new IllegalArgumentException("erasedAt is required");
    transactions.executeWithoutResult(
        ignored -> {
          ensureActive(scope, userId);
          lockAndIsActive(scope, userId);
          jdbc.update(
              """
              UPDATE account_erasure_barriers
              SET status='ERASED', request_id=?, erased_at=?, updated_at=?
              WHERE context_name=? AND user_id=?
              """,
              requestId,
              Timestamp.from(erasedAt),
              Timestamp.from(erasedAt),
              scope.name(),
              userId);
          erasure.run();
        });
  }

  private void ensureActive(Scope scope, UUID userId) {
    require(scope, userId);
    jdbc.update(
        """
        INSERT INTO account_erasure_barriers(context_name,user_id,status,created_at,updated_at)
        VALUES (?,?,'ACTIVE',now(),now())
        ON CONFLICT(context_name,user_id) DO NOTHING
        """,
        scope.name(),
        userId);
  }

  private boolean lockAndIsActive(Scope scope, UUID userId) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            """
            SELECT status='ACTIVE' FROM account_erasure_barriers
            WHERE context_name=? AND user_id=?
            FOR UPDATE
            """,
            Boolean.class,
            scope.name(),
            userId));
  }

  private static java.util.List<UUID> normalized(Collection<UUID> userIds) {
    if (userIds == null) throw new IllegalArgumentException("userIds are required");
    var unique = new LinkedHashSet<UUID>();
    userIds.forEach(
        userId -> {
          if (userId != null) unique.add(userId);
        });
    return unique.stream().sorted(Comparator.comparing(UUID::toString)).toList();
  }

  private static void require(Scope scope, UUID userId) {
    if (scope == null) throw new IllegalArgumentException("scope is required");
    if (userId == null) throw new IllegalArgumentException("userId is required");
  }
}
