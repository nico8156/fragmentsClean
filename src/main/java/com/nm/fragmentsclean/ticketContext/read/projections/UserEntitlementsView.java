package com.nm.fragmentsclean.ticketContext.read.projections;

import com.nm.fragmentsclean.ticketContext.read.pass.PassCountersView;
import com.nm.fragmentsclean.ticketContext.read.pass.PassLevel;
import com.nm.fragmentsclean.ticketContext.read.pass.PassLevelView;
import com.nm.fragmentsclean.ticketContext.read.pass.PassProgressPolicy;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserEntitlementsView(
        UUID userId,
        int confirmedTickets,
        int publishedComments,
        int confirmedLikes,
        long version,
        Instant updatedAt) {

    public PassCountersView counters() {
        return new PassCountersView(confirmedTickets, publishedComments, confirmedLikes);
    }

    public PassLevel currentLevel() {
        return PassProgressPolicy.currentLevel(counters());
    }

    public List<PassLevelView> levels() {
        return PassProgressPolicy.levelsFor(counters());
    }

    public List<String> rights() {
        return PassProgressPolicy.rightsFor(counters());
    }
}
