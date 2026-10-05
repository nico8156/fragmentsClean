package com.nm.fragmentsclean.articleContext.write.businesslogic.models;
import java.time.Instant;
import java.util.UUID;
public record ArticleMediaUpload(UUID mediaId,UUID articleId,String storageReference,String originalName,String contentType,long size,Integer width,Integer height,UUID uploadedBy,String purpose,Instant uploadedAt){}
