package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

public record CommentCreateRequestDto(
        String commandId,
        String commentId,   // côté front: tempId (ou déjà l’id serveur)
        String userId,
        String targetId,
        String parentId,    // peut être null
        String body,
        String at           // ISODate côté front
) {
}
