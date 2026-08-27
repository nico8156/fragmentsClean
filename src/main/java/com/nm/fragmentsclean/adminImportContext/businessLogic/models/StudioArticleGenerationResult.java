package com.nm.fragmentsclean.adminImportContext.businessLogic.models;
import java.util.UUID;
public record StudioArticleGenerationResult(UUID commandId, UUID sagaId, UUID articleId, UUID revisionId, String status) { }
