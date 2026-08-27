package com.nm.fragmentsclean.adminImportContext.businessLogic.models;
import java.time.Instant;
import java.util.UUID;
public record StudioArticleGenerationCommand(UUID commandId, Instant clientAt, UUID sagaId, UUID articleId,
        UUID revisionId, String subject, String slug, String locale, UUID operatorId, String operatorName) { }
