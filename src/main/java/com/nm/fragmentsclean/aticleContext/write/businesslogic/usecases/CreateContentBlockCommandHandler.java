package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.aticleContext.ContentBlock;
import com.nm.fragmentsclean.aticleContext.ContentValue;
import com.nm.fragmentsclean.aticleContext.OrderBlock;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ContentBlockRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;

import java.util.UUID;

public class CreateContentBlockCommandHandler implements CommandHandler<CreateContenBlockCommand> {

    private final ContentBlockRepository contentBlockRepository;

    public CreateContentBlockCommandHandler(ContentBlockRepository contentBlockRepository) {
        this.contentBlockRepository = contentBlockRepository;
    }

    @Override
    public void execute(CreateContenBlockCommand command) {

        contentBlockRepository.save(
                new ContentBlock(
                        command.id(),
                        command.articleId(),
                        command.type(),
                        new ContentValue(command.content()),
                        new OrderBlock(command.order())
                )
        );
    }
}

