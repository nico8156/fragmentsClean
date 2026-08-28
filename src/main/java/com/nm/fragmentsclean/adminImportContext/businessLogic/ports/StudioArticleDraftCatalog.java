package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleDraftDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudioArticleDraftCatalog {
    List<StudioArticleDraftDocument> list();
    Optional<StudioArticleDraftDocument> byId(UUID articleId);
}
