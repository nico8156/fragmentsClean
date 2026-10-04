package com.nm.fragmentsclean.platform.configuration;
import com.nm.fragmentsclean.mediaCatalogContext.read.MediaCatalogPreviewPort;
import com.nm.fragmentsclean.experienceContext.read.AdminExperienceMediaPreviewsQuery;
import com.nm.fragmentsclean.experienceContext.read.AdminExperienceMediaPreviewsQueryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/** Composition root bridges primitive ACLs; neither context imports the other. */
@Configuration
public class MediaCatalogPreviewConfiguration {
    @Bean MediaCatalogPreviewPort mediaCatalogPreviewPort(AdminExperienceMediaPreviewsQueryHandler query){
        return ids->query.handle(new AdminExperienceMediaPreviewsQuery(ids));
    }
}
