package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaUpload;
import com.nm.fragmentsclean.articleContext.read.ArticleMediaReferenceIdentity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class RecordArticleMediaUpload implements ArticleMediaUploadRecorder {
    private final ArticleMediaUploadRepository repository;private final ArticleImageMetadataReader metadata;private final ArticleMediaCatalogPublisher catalogue;private final DateTimeProvider clock;
    public RecordArticleMediaUpload(ArticleMediaUploadRepository repository,ArticleImageMetadataReader metadata,ArticleMediaCatalogPublisher catalogue,DateTimeProvider clock){this.repository=repository;this.metadata=metadata;this.catalogue=catalogue;this.clock=clock;}
    @Transactional public void record(UUID articleId,String reference,String originalName,String contentType,byte[] bytes,Integer width,Integer height,UUID uploadedBy,String purpose){
        Objects.requireNonNull(articleId);if(bytes==null || bytes.length==0 || !Set.of("STUDIO","GENERATION").contains(purpose))throw new IllegalArgumentException("Invalid stored article upload");
        var measured=metadata.read(bytes);if(measured.width()!=null && measured.height()!=null){width=measured.width();height=measured.height();}
        if(width==null || height==null || width<=0 || height<=0){width=null;height=null;}
        var upload=new ArticleMediaUpload(ArticleMediaReferenceIdentity.of(reference),articleId,reference.trim(),originalName,contentType==null || contentType.isBlank()?"application/octet-stream":contentType,bytes.length,width,height,uploadedBy,purpose,clock.now());
        repository.save(upload);catalogue.publish(articleId);
    }
}
