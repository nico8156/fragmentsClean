package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ScheduledCommandOutcomePort;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.CommandStatusRepository;

import java.util.UUID;

/** Command status is a technical shared contract, not article domain state. */
public final class CommandStatusScheduledOutcomeAdapter implements ScheduledCommandOutcomePort {
    private final CommandStatusRepository statuses;
    public CommandStatusScheduledOutcomeAdapter(CommandStatusRepository statuses) { this.statuses = statuses; }

    @Override
    public Outcome find(UUID commandId) {
        var value = statuses.find(commandId);
        return new Outcome(Outcome.Status.valueOf(value.status()), value.reason());
    }
}
