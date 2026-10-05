package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.time.Instant;
import java.util.UUID;
public record MediaCatalogView(String id,String origin,UUID mediaId,UUID resourceId,UUID ownerId,String status,String previewUrl,String contentType,Long size,Integer width,Integer height,Instant createdAt,Instant updatedAt,String originalName,UUID uploadedBy,String purpose,String usageStatus,MediaCatalogArticleUsagePage articleUsages) {
    public MediaCatalogView(String id,String origin,UUID mediaId,UUID resourceId,UUID ownerId,String status,String previewUrl,String contentType,Long size,Integer width,Integer height,Instant createdAt,Instant updatedAt){this(id,origin,mediaId,resourceId,ownerId,status,previewUrl,contentType,size,width,height,createdAt,updatedAt,null,null,null,null,null);}
    public MediaCatalogView withPreview(String url){return new MediaCatalogView(id,origin,mediaId,resourceId,ownerId,status,url,contentType,size,width,height,createdAt,updatedAt,originalName,uploadedBy,purpose,usageStatus,articleUsages);}
    public MediaCatalogView withArticleUsages(MediaCatalogArticleUsagePage page){return new MediaCatalogView(id,origin,mediaId,resourceId,ownerId,status,previewUrl,contentType,size,width,height,createdAt,updatedAt,originalName,uploadedBy,purpose,usageStatus,page);}
}
