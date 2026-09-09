package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable audit of one provider call. Cost is recorded, never inferred later from a changing price list. */
public record GenerationExecution(UUID id, String operation, String model, int inputTokens, int outputTokens,
                                  BigDecimal estimatedCost, Duration duration, String outcome, Instant occurredAt) {
 public GenerationExecution { Objects.requireNonNull(id); operation=text(operation); model=text(model); if(inputTokens<0||outputTokens<0)throw new IllegalArgumentException("Tokens must not be negative"); if(estimatedCost==null||estimatedCost.signum()<0)throw new IllegalArgumentException("Cost must not be negative"); if(duration==null||duration.isNegative())throw new IllegalArgumentException("Duration must not be negative"); outcome=text(outcome); Objects.requireNonNull(occurredAt); }
 private static String text(String value){if(value==null||value.isBlank())throw new IllegalArgumentException("Execution field must not be blank");return value;}
}
