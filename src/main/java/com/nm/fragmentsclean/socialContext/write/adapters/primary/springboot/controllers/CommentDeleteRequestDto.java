package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

public record CommentDeleteRequestDto(
        String commandId,
        String commentId,
        String deletedAt     // ISODate
) {
}
