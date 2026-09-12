package com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketVerificationJobRepository {
    Optional<TicketVerificationJob> byId(UUID jobId);
    void save(TicketVerificationJob job);
    List<UUID> claimableIds(Instant now, int limit);
}
