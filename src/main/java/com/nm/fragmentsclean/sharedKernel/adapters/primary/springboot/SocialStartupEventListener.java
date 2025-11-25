package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot;


import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommandHandler;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SocialStartupEventListener {
    private final CommandBus commandBus;
    private final MakeLikeCommandHandler makeLikeCommandHandler;

    public SocialStartupEventListener(CommandBus commandBus,
                                      MakeLikeCommandHandler makeLikeCommandHandler) {
        this.commandBus = commandBus;
        this.makeLikeCommandHandler = makeLikeCommandHandler;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        List<CommandHandler<?>> handlers = List.of(
                makeLikeCommandHandler
        );
        commandBus.registerCommandHandlers(handlers);
    }
}
