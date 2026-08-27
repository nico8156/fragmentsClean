package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.article;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleGenerationRequestPort;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.RequestArticleGenerationCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import org.springframework.stereotype.Component;

@Component
public final class CommandBusArticleGenerationRequestAdapter implements ArticleGenerationRequestPort {
    private final CommandBus bus;

    public CommandBusArticleGenerationRequestAdapter(CommandBus bus) {
        this.bus = bus;
    }

    @Override
    public void request(RequestArticleGenerationCommand command) {
        bus.dispatch(command);
    }
}
