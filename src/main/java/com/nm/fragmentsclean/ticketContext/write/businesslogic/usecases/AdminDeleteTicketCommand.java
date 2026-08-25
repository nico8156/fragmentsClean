package com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases;

import java.time.Instant;
import java.util.UUID;

public record AdminDeleteTicketCommand(UUID commandId, UUID ticketId, UUID actorUserId, Instant occurredAt) { }
