package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways;
import java.util.UUID;
public interface ArticleMediaUploadRecorder {
    void record(UUID articleId,String reference,String originalName,String contentType,byte[] bytes,Integer width,Integer height,UUID uploadedBy,String purpose);
}
