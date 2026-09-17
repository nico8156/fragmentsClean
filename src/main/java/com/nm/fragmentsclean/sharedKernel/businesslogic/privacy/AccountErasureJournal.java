package com.nm.fragmentsclean.sharedKernel.businesslogic.privacy;

import java.time.Instant;
import java.util.UUID;

/**
 * Append-only, off-database evidence that an account erasure was requested.
 *
 * <p>The journal deliberately lives outside PostgreSQL so that restoring an older database backup
 * cannot forget an accepted erasure request.
 */
public interface AccountErasureJournal {
  void record(Entry entry);

  record Entry(UUID requestId, UUID userId, UUID authUserId, Instant requestedAt) {
    public Entry {
      if (requestId == null || userId == null || authUserId == null || requestedAt == null) {
        throw new IllegalArgumentException("Account erasure journal entries must be complete");
      }
    }
  }
}
