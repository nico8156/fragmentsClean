package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;
import java.util.UUID;
/** Primitive ACL to owner-owned durable upload tracking; no database or storage access. */
public interface ArticleMediaUploadTracking {
    void record(UUID articleId,String reference,String originalName,String contentType,byte[] bytes,Integer width,Integer height,UUID uploadedBy,String purpose);
}
