package com.nm.fragmentsclean.platform.eventing.contracts;
import java.time.Instant;
import java.util.*;
/** Admin-only inventory part. All parts of a version form one atomic snapshot. */
public record ArticleMediaCatalogSnapshotIntegrationEvent(UUID eventId,UUID articleId,long version,int part,int parts,List<Reference> references,Instant occurredAt) {
    public ArticleMediaCatalogSnapshotIntegrationEvent {
        Objects.requireNonNull(eventId);Objects.requireNonNull(articleId);Objects.requireNonNull(occurredAt);
        if(version<1 || parts<1 || part<0 || part>=parts || references==null || references.size()>100)throw new IllegalArgumentException("Invalid article media inventory part");
        references=List.copyOf(references);
    }
    public record Reference(UUID mediaId,String storageReference,UUID usageId,UUID revisionId,Integer revisionNumber,String title,String revisionStatus,String role,Integer sectionPosition,Integer imagePosition,String alt,Integer width,Integer height,boolean working,boolean published,String contentType,Long size,Instant uploadedAt,UUID uploadedBy,String purpose,String originalName) {
        public Reference {
            Objects.requireNonNull(mediaId);Objects.requireNonNull(usageId);
            if(!Set.of("COVER","SECTION","UPLOAD").contains(role))throw new IllegalArgumentException("Invalid article media role");
            if(!"UPLOAD".equals(role) && (revisionId==null || revisionNumber==null || revisionNumber<1))throw new IllegalArgumentException("Missing article media revision");
        }
    }
}
