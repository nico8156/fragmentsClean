package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.storage;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.GeneratedArticleImageStorage;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix="article.images.storage",name="backend",havingValue="s3")
public final class S3GeneratedArticleImageStorage implements GeneratedArticleImageStorage {
    private final ArticleImageStorageProperties properties; private final S3Client s3;
    public S3GeneratedArticleImageStorage(ArticleImageStorageProperties properties,
                                         @Qualifier("articleImageS3Client") S3Client s3){this.properties=properties;this.s3=s3;}
    @Override public StoredImage store(UUID articleId,UUID imageId,String mediaType,byte[] bytes){
        String bucket=require(properties.getS3Bucket()); String prefix=properties.getS3Prefix().replaceAll("^/+|/+$","");
        String extension="image/webp".equals(mediaType)?".webp":"image/png".equals(mediaType)?".png":".jpg";
        String key=prefix+"/"+articleId+"/generated/"+imageId+extension;
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(mediaType).build(),RequestBody.fromBytes(bytes.clone()));
        return new StoredImage("s3://"+bucket+"/"+key,0,0);
    }
    private static String require(String value){if(value==null||value.isBlank())throw new IllegalStateException("article.images.storage.s3-bucket is required");return value.trim();}
}
