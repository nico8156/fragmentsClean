package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ModerateCommentRequestDto(UUID commandId, UUID actionId, UUID commentId,
                                        Decision decision, String reason, Instant at) {
    public ModerateCommentRequestDto {
        Objects.requireNonNull(commandId); Objects.requireNonNull(actionId);
        Objects.requireNonNull(commentId); Objects.requireNonNull(decision); Objects.requireNonNull(at);
    }
    public enum Decision { HIDDEN, PUBLISHED }
}
