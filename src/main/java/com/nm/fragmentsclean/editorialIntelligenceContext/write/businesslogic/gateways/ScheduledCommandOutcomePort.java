package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

import java.util.UUID;

public interface ScheduledCommandOutcomePort {
    Outcome find(UUID commandId);
    record Outcome(Status status, String reason) {
        public enum Status { PENDING, APPLIED, REJECTED }
    }
}
