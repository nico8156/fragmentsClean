package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.contentblock;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ContentBlock;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ContentBlockRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;

public class UpdateContentBlockCommandHandler implements CommandHandler<UpdateContentBlockCommand> {

    private final ContentBlockRepository contentBlockRepository;

    public UpdateContentBlockCommandHandler(ContentBlockRepository contentBlockRepository) {
        this.contentBlockRepository = contentBlockRepository;
    }

    @Override
    public void execute(UpdateContentBlockCommand command) {
        ContentBlock updatedContentBlock = new ContentBlock(command.id(), command.articleId(), command.type(), command.content(), command.order());
        contentBlockRepository.update(updatedContentBlock);
    }
}
