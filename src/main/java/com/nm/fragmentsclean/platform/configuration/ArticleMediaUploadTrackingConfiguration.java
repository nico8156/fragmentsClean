package com.nm.fragmentsclean.platform.configuration;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleMediaUploadTracking;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.RecordArticleMediaUpload;
import org.springframework.context.annotation.*;
@Configuration
public class ArticleMediaUploadTrackingConfiguration {
    @Bean ArticleMediaUploadTracking articleMediaUploadTracking(RecordArticleMediaUpload recorder){return recorder::record;}
}
