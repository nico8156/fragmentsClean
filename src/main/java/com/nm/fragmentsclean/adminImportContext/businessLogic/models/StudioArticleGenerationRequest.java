package com.nm.fragmentsclean.adminImportContext.businessLogic.models;
import java.util.UUID;
public record StudioArticleGenerationRequest(String subject, String locale, UUID operatorId, String operatorName) { }
