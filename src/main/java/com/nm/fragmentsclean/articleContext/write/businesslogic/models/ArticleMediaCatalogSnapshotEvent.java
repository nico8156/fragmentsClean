package com.nm.fragmentsclean.articleContext.write.businesslogic.models;
import java.time.Instant;
import java.util.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
public record ArticleMediaCatalogSnapshotEvent(UUID eventId,UUID articleId,long version,int part,int parts,List<Reference> references,Instant occurredAt) implements DomainEvent {
    public ArticleMediaCatalogSnapshotEvent {references=List.copyOf(references);}
    public record Reference(UUID mediaId,String storageReference,UUID usageId,UUID revisionId,Integer revisionNumber,String title,String revisionStatus,String role,Integer sectionPosition,Integer imagePosition,String alt,Integer width,Integer height,boolean working,boolean published,String contentType,Long size,Instant uploadedAt,UUID uploadedBy,String purpose,String originalName,String uploadStatus) {
        public Reference(UUID mediaId,String storageReference,UUID usageId,UUID revisionId,Integer revisionNumber,String title,String revisionStatus,String role,Integer sectionPosition,Integer imagePosition,String alt,Integer width,Integer height,boolean working,boolean published,String contentType,Long size,Instant uploadedAt,UUID uploadedBy,String purpose,String originalName){this(mediaId,storageReference,usageId,revisionId,revisionNumber,title,revisionStatus,role,sectionPosition,imagePosition,alt,width,height,working,published,contentType,size,uploadedAt,uploadedBy,purpose,originalName,"ACTIVE");}
}
}
