package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.contentblock;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ContentBlockRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;

public class DeleteContentBlockCommandHandler implements CommandHandler<DeleteContentBlockCommand> {

     ContentBlockRepository contentBlockRepository;

     public DeleteContentBlockCommandHandler(ContentBlockRepository contentBlockRepository) {
        this.contentBlockRepository = contentBlockRepository;
     }

    @Override
    public void execute(DeleteContentBlockCommand command) {
        contentBlockRepository.delete(command.id());
    }
}
