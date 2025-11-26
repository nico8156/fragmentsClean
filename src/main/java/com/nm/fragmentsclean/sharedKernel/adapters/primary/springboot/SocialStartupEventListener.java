package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot;


import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.CreateCommentCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.DeleteCommentCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.UpdateCommentCommandHandler;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SocialStartupEventListener {
    private final CommandBus commandBus;
    private final MakeLikeCommandHandler makeLikeCommandHandler;
    private final CreateCommentCommandHandler createCommentCommandHandler;
    private final UpdateCommentCommandHandler updateCommentCommandHandler;
    private final DeleteCommentCommandHandler deleteCommentCommandHandler;

    public SocialStartupEventListener(CommandBus commandBus,
                                      MakeLikeCommandHandler makeLikeCommandHandler,
                                      CreateCommentCommandHandler createCommentCommandHandler,
                                      UpdateCommentCommandHandler updateCommentCommandHandler,
                                      DeleteCommentCommandHandler deleteCommentCommandHandler) {
        this.commandBus = commandBus;
        this.makeLikeCommandHandler = makeLikeCommandHandler;
        this.createCommentCommandHandler = createCommentCommandHandler;
        this.updateCommentCommandHandler = updateCommentCommandHandler;
        this.deleteCommentCommandHandler = deleteCommentCommandHandler;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        List<CommandHandler<?>> handlers = List.of(
                makeLikeCommandHandler,
                createCommentCommandHandler,
                updateCommentCommandHandler,
                deleteCommentCommandHandler
        );
        commandBus.registerCommandHandlers(handlers);
    }
}
