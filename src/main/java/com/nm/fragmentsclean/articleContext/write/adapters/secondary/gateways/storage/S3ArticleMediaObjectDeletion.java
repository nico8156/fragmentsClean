package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.storage;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import com.nm.fragmentsclean.articleContext.media.ArticleManagedStorageReferences;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaObjectDeletion;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;
@Component @ConditionalOnProperty(prefix="article.images.storage",name="backend",havingValue="s3")
public final class S3ArticleMediaObjectDeletion implements ArticleMediaObjectDeletion {
 private final ArticleImageStorageProperties properties;private final ArticleManagedStorageReferences references;private final S3Client s3;
 public S3ArticleMediaObjectDeletion(ArticleImageStorageProperties properties,ArticleManagedStorageReferences references,@Qualifier("articleImageS3Client") S3Client s3){this.properties=properties;this.references=references;this.s3=s3;}
 public void delete(UUID articleId,String reference){String key=references.locate(articleId,reference).orElseThrow(()->new IllegalArgumentException("Unmanaged Article media reference"));s3.deleteObject(DeleteObjectRequest.builder().bucket(properties.getS3Bucket().trim()).key(key).build());}
}
