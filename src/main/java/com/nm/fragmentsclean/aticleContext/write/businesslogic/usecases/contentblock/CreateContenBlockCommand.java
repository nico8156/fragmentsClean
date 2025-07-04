package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.contentblock;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.BlockType;

import java.util.UUID;

public record CreateContenBlockCommand (
        UUID id,
        UUID articleId,
        BlockType type,
        String content,
        int order
){
}
