package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCommand;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;

public class CommandBusArticleAuthoringPort implements ArticleAuthoringPort {
	private final CommandBus commandBus;

	public CommandBusArticleAuthoringPort(CommandBus commandBus) {
		this.commandBus = commandBus;
	}

	@Override
	public void createArticle(StudioArticleCommand command) {
		commandBus.dispatch(new CreateArticleCommand(
				command.commandId(),
				command.clientAt(),
				command.articleId(),
				command.slug(),
				command.locale(),
				command.authorId(),
				command.authorName(),
				command.title(),
				command.intro(),
				command.blocksJson(),
				command.conclusion(),
				command.coverUrl(),
				command.coverWidth(),
				command.coverHeight(),
				command.coverAlt(),
				command.tags(),
				command.readingTimeMin(),
				command.coffeeIds()
		));
	}
}
