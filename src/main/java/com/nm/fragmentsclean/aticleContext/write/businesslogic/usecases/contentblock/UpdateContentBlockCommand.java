package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.contentblock;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.BlockType;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ContentValue;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.OrderBlock;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Command;

import java.util.UUID;

public record UpdateContentBlockCommand (
        UUID id,
        UUID articleId,
        BlockType type,
        ContentValue content,
        OrderBlock order
) implements Command{
}
