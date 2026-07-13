package com.nm.fragmentsclean.ticketContext.read.pass;

import java.util.List;

public record PassLevelView(
        PassLevel level,
        PassLevelStatus status,
        PassRequirementsView requirements,
        List<String> unlockedCapabilities) {
}
