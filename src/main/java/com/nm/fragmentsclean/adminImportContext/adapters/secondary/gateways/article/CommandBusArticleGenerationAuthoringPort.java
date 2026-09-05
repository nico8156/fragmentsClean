package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleGenerationAuthoringPort;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.RequestArticleGenerationCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
public final class CommandBusArticleGenerationAuthoringPort implements ArticleGenerationAuthoringPort {
    private final CommandBus bus; public CommandBusArticleGenerationAuthoringPort(CommandBus bus){this.bus=bus;}
    @Override public void requestGeneration(StudioArticleGenerationCommand c){bus.dispatch(new RequestArticleGenerationCommand(c.commandId(),c.clientAt(),c.sagaId(),c.articleId(),c.revisionId(),c.subject(),c.slug(),c.locale(),c.operatorId(),c.operatorName(),ArticleAuthoringTrigger.MANUAL));}
}
