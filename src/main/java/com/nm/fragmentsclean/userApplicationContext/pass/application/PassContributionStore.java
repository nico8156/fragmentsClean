package com.nm.fragmentsclean.userApplicationContext.pass.application;

import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassCounters;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassSnapshot;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PassContributionStore {
    boolean applyTicket(UUID ticketId, UUID userId, boolean active, long sourceVersion, Instant occurredAt);
    boolean applyExperience(UUID experienceId, UUID userId, UUID coffeeId, boolean active,
                            long sourceVersion, Instant occurredAt);
    void lockUser(UUID userId);
    PassCounters counters(UUID userId);
    Optional<PassSnapshot> find(UUID userId);
    void save(PassSnapshot snapshot);
}
