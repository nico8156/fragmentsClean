package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;

public class CommandBusArticleAuthoringPort implements ArticleAuthoringPort {
	private final CommandBus commandBus;

	public CommandBusArticleAuthoringPort(CommandBus commandBus) {
		this.commandBus = commandBus;
	}

	@Override
	public void createArticle(CreateArticleCommand command) {
		commandBus.dispatch(command);
	}
}
