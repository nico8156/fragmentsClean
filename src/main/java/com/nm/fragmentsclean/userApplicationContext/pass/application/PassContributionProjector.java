package com.nm.fragmentsclean.userApplicationContext.pass.application;

import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassLevel;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassProgressPolicy;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassSnapshot;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class PassContributionProjector {
    private final PassContributionStore store;

    public PassContributionProjector(PassContributionStore store) {
        this.store = store;
    }

    @Transactional
    public Optional<PassSnapshot> ticketChanged(UUID ticketId, UUID userId, boolean active,
                                                 boolean revokesAcquiredLevels, long sourceVersion,
                                                 Instant occurredAt) {
        store.lockUser(userId);
        if (!store.applyTicket(ticketId, userId, active, sourceVersion, occurredAt)) return Optional.empty();
        return Optional.of(rebuild(userId, revokesAcquiredLevels, occurredAt));
    }

    @Transactional
    public Optional<PassSnapshot> experienceChanged(UUID experienceId, UUID userId, UUID coffeeId,
                                                     boolean active, boolean revokesAcquiredLevels,
                                                     long sourceVersion, Instant occurredAt) {
        store.lockUser(userId);
        if (!store.applyExperience(experienceId, userId, coffeeId, active, sourceVersion, occurredAt)) {
            return Optional.empty();
        }
        return Optional.of(rebuild(userId, revokesAcquiredLevels, occurredAt));
    }

    private PassSnapshot rebuild(UUID userId, boolean revoke, Instant occurredAt) {
        var previous = store.find(userId).orElse(null);
        var counters = store.counters(userId);
        Set<PassLevel> eligible = PassProgressPolicy.eligibleLevels(counters);
        var acquired = eligible.isEmpty() ? EnumSet.noneOf(PassLevel.class) : EnumSet.copyOf(eligible);
        if (!revoke && previous != null) acquired.addAll(previous.acquiredLevels());
        long version = previous == null ? 1 : previous.version() + 1;
        var snapshot = PassProgressPolicy.evaluate(userId, counters, acquired, version, occurredAt);
        store.save(snapshot);
        return snapshot;
    }
}
