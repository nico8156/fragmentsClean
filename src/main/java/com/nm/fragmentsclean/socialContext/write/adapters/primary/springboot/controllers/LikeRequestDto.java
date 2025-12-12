package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

public record LikeRequestDto(
        String commandId,
        String likeId,
        String targetId,
        boolean value,
        String at // ISO string, ex: "2025-11-25T10:15:30.000Z"
) {
}
