package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.storage;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.GeneratedArticleImageStorage;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix="article.images.storage",name="backend",havingValue="local",matchIfMissing=true)
public final class LocalGeneratedArticleImageStorage implements GeneratedArticleImageStorage {
    private final ArticleImageStorageProperties properties;
    public LocalGeneratedArticleImageStorage(ArticleImageStorageProperties properties){this.properties=properties;}
    @Override public StoredImage store(UUID articleId,UUID imageId,String mediaType,byte[] bytes){
        String extension="image/webp".equals(mediaType)?".webp":"image/png".equals(mediaType)?".png":".jpg";
        var target=properties.getDirectory().resolve(imageId+extension).normalize();
        if(!target.startsWith(properties.getDirectory().normalize())) throw new IllegalArgumentException("Invalid article image path");
        try{Files.createDirectories(properties.getDirectory());Files.write(target,bytes.clone());}catch(java.io.IOException e){throw new IllegalStateException("Cannot store generated article image",e);}
        String reference="/api/articles/image-assets/"+target.getFileName();
        return new StoredImage(reference,0,0);
    }
}
