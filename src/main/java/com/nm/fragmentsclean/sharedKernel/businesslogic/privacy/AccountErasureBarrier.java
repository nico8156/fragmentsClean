package com.nm.fragmentsclean.sharedKernel.businesslogic.privacy;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

/**
 * Serializes user-owned projection mutations with account erasure.
 *
 * <p>The callback is executed in the same transaction as the durable barrier decision. Contexts
 * remain responsible for selecting the user identifiers that own or are referenced by an event.
 */
public interface AccountErasureBarrier {

  enum Scope {
    AUTHENTICATION,
    USER_APPLICATION,
    SOCIAL,
    TICKET,
    EXPERIENCE
  }

  boolean ifAllActive(Scope scope, Collection<UUID> userIds, Runnable mutation);

  default boolean ifActive(Scope scope, UUID userId, Runnable mutation) {
    return ifAllActive(scope, java.util.List.of(userId), mutation);
  }

  default boolean ifAccountActive(UUID userId, Runnable mutation) {
    var applied = new java.util.concurrent.atomic.AtomicBoolean();
    Runnable guarded = () -> {
      mutation.run();
      applied.set(true);
    };
    var scopes = Scope.values();
    for (int index = scopes.length - 1; index >= 0; index--) {
      var scope = scopes[index];
      var next = guarded;
      guarded = () -> ifActive(scope, userId, next);
    }
    guarded.run();
    return applied.get();
  }

  void erase(
      Scope scope,
      UUID userId,
      UUID requestId,
      Instant erasedAt,
      Runnable erasure);
}
