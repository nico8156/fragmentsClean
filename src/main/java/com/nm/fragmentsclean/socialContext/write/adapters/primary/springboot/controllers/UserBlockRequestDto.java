package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record UserBlockRequestDto(UUID commandId, UUID blockId, UUID blockedUserId, boolean active, Instant at) {
    public UserBlockRequestDto {
        Objects.requireNonNull(commandId); Objects.requireNonNull(blockId);
        Objects.requireNonNull(blockedUserId); Objects.requireNonNull(at);
    }
}
