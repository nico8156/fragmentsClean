package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioGeneratedArticleEdit;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GeneratedArticleEditingPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

public final class EditStudioGeneratedArticle {
	private final GeneratedArticleEditingPort editingPort;
	private final UuidGenerator uuidGenerator;
	private final DateTimeProvider clock;

	public EditStudioGeneratedArticle(
			GeneratedArticleEditingPort editingPort,
			UuidGenerator uuidGenerator,
			DateTimeProvider clock) {
		this.editingPort = editingPort;
		this.uuidGenerator = uuidGenerator;
		this.clock = clock;
	}

	public UUID execute(StudioGeneratedArticleEdit edit) {
		UUID commandId = uuidGenerator.generate();
		editingPort.edit(commandId, clock.now(), edit);
		return commandId;
	}
}
