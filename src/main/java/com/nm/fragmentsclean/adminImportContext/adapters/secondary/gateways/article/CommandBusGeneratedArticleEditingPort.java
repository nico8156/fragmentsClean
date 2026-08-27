package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article;

import java.time.Instant;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioGeneratedArticleEdit;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GeneratedArticleEditingPort;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.EditGeneratedArticleRevisionCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;

public final class CommandBusGeneratedArticleEditingPort implements GeneratedArticleEditingPort {
	private final CommandBus commandBus;

	public CommandBusGeneratedArticleEditingPort(CommandBus commandBus) {
		this.commandBus = commandBus;
	}

	@Override
	public void edit(UUID commandId, Instant clientAt, StudioGeneratedArticleEdit edit) {
		commandBus.dispatch(new EditGeneratedArticleRevisionCommand(
				commandId,
				clientAt,
				edit.sagaId(),
				edit.articleId(),
				edit.revisionId(),
				edit.title(),
				edit.introduction(),
				edit.conclusion(),
				new EditGeneratedArticleRevisionCommand.Cover(
						edit.cover().storageReference(),
						edit.cover().width(),
						edit.cover().height(),
						edit.cover().alt()),
				edit.sections().stream().map(section -> new EditGeneratedArticleRevisionCommand.Section(
						section.heading(),
						section.paragraph(),
						section.storageReference(),
						section.width(),
						section.height(),
						section.alt())).toList(),
				edit.tags()));
	}
}
