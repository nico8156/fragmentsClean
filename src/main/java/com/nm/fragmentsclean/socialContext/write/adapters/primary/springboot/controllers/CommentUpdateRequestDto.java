package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

public record CommentUpdateRequestDto(
        String commandId,
        String commentId,
        String body,
        String editedAt      // ISODate
) {
}
