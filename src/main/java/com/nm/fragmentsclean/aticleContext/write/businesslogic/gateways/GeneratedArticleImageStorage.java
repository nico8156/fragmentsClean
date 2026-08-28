package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways;
import java.util.UUID;
public interface GeneratedArticleImageStorage {
    StoredImage store(UUID articleId, UUID imageId, String mediaType, byte[] bytes);
    record StoredImage(String storageReference,int width,int height) { }
}
