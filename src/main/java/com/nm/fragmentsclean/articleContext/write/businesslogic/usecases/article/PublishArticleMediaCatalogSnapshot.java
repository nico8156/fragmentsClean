package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaCatalogSnapshotEvent;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaCatalogSnapshotEvent.Reference;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class PublishArticleMediaCatalogSnapshot implements ArticleMediaCatalogPublisher {
    private final ArticleMediaCatalogSource source;private final DomainEventPublisher events;private final DateTimeProvider clock;
    public PublishArticleMediaCatalogSnapshot(ArticleMediaCatalogSource source,DomainEventPublisher events,DateTimeProvider clock){this.source=source;this.events=events;this.clock=clock;}
    @Transactional public void publish(UUID articleId){
        var snapshot=source.nextSnapshot(articleId);
        var chunks=new ArrayList<List<Reference>>();var chunk=new ArrayList<Reference>();long bytes=0;
        for(var reference:snapshot.references()){
            long size=upperBound(reference);
            if(!chunk.isEmpty() && (chunk.size()==100 || bytes+size>180000)){chunks.add(List.copyOf(chunk));chunk.clear();bytes=0;}
            if(size>180000)throw new IllegalStateException("Article media reference exceeds transport budget");
            chunk.add(reference);bytes+=size;
        }
        if(!chunk.isEmpty() || chunks.isEmpty())chunks.add(List.copyOf(chunk));
        var at=clock.now();for(int i=0;i<chunks.size();i++)events.publish(new ArticleMediaCatalogSnapshotEvent(UUID.randomUUID(),articleId,snapshot.version(),i,chunks.size(),chunks.get(i),at));
    }
    private static long upperBound(Reference r){
        long result=2000;for(String s:Arrays.asList(r.storageReference(),r.title(),r.revisionStatus(),r.alt(),r.contentType(),r.purpose(),r.originalName()))if(s!=null)result+=6L*s.length();return result;
    }
}
