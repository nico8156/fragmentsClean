package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;
import java.time.Instant;
import java.util.UUID;

/** Technical intent to claim one due source before external consultation. */
public record ClaimEditorialSourceConsultationCommand(UUID sourceId, String workerId, Instant leaseUntil) implements Command { }
