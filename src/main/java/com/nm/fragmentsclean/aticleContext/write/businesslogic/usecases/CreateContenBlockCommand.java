package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.aticleContext.BlockType;
import com.nm.fragmentsclean.aticleContext.ContentValue;

import java.util.UUID;

public record CreateContenBlockCommand (
        UUID id,
        UUID articleId,
        BlockType type,
        String content,
        int order
){
}
