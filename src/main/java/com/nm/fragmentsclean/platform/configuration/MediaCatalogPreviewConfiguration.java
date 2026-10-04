package com.nm.fragmentsclean.platform.configuration;
import com.nm.fragmentsclean.mediaCatalogContext.read.MediaCatalogPreviewPort;
import com.nm.fragmentsclean.experienceContext.read.AdminExperienceMediaPreviewsQuery;
import com.nm.fragmentsclean.experienceContext.read.AdminExperienceMediaPreviewsQueryHandler;
import com.nm.fragmentsclean.coffeeContext.read.*;
import java.util.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/** Composition root bridges primitive ACLs; neither context imports the other. */
@Configuration
public class MediaCatalogPreviewConfiguration {
    @Bean MediaCatalogPreviewPort mediaCatalogPreviewPort(AdminExperienceMediaPreviewsQueryHandler query,AdminCoffeeMediaPreviewsQueryHandler coffee,com.nm.fragmentsclean.userApplicationContext.read.AdminAvatarMediaPreviewsQueryHandler avatar,com.nm.fragmentsclean.articleContext.read.AdminArticleMediaPreviewsQueryHandler article){
        return new MediaCatalogPreviewPort(){
            public Map<UUID,String> articlePreviews(Map<UUID,String> refs){return article.handle(new com.nm.fragmentsclean.articleContext.read.AdminArticleMediaPreviewsQuery(refs));}
            public Map<UUID,String> avatarPreviews(Map<UUID,UUID> refs){return avatar.handle(new com.nm.fragmentsclean.userApplicationContext.read.AdminAvatarMediaPreviewsQuery(refs));}
            public Map<UUID,String> experiencePreviews(List<UUID> ids){return query.handle(new AdminExperienceMediaPreviewsQuery(ids));}
            public Map<UUID,String> coffeePreviews(Map<UUID,UUID> refs){return coffee.handle(new AdminCoffeeMediaPreviewsQuery(refs));}
        };
    }
}
