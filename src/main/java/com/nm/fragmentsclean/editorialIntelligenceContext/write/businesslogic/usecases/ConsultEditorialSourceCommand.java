package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.UUID;

/** Technical command that drives one durable external source consultation. */
public record ConsultEditorialSourceCommand(UUID sourceId, String workerId, Instant leaseUntil) implements Command { }
