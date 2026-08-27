package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.time.Instant;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioGeneratedArticleEdit;

public interface GeneratedArticleEditingPort {
	void edit(UUID commandId, Instant clientAt, StudioGeneratedArticleEdit edit);
}
