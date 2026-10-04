package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.util.List;
import java.util.Map;
import java.util.UUID;
/** Primitive batch ACL: only the owning domain may authorize a current preview. */
public interface MediaCatalogPreviewPort {
    Map<UUID,String> avatarPreviews(Map<UUID,UUID> mediaProfiles);
    Map<UUID,String> coffeePreviews(Map<UUID,UUID> mediaResources);
    Map<UUID,String> experiencePreviews(List<UUID> mediaIds);
}
