package com.nm.fragmentsclean.articleContextTest.unit;
import java.util.*;import java.nio.file.*;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
import com.nm.fragmentsclean.articleContext.media.ArticleManagedStorageReferences;
import com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.storage.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;
class ArticleMediaDeletionStorageTest {
 @TempDir Path directory;
 @Test void local_deletes_only_managed_filename_is_idempotent_and_rejects_traversal()throws Exception{
  var properties=new ArticleImageStorageProperties();properties.setDirectory(directory);var refs=new ArticleManagedStorageReferences(properties);var adapter=new LocalArticleMediaObjectDeletion(properties,refs);var article=UUID.randomUUID();String file=UUID.randomUUID()+".png";String reference="/api/articles/image-assets/"+file;
  Files.write(directory.resolve(file),new byte[]{1});Files.write(directory.resolve("other.png"),new byte[]{1});adapter.delete(article,reference);adapter.delete(article,reference);
  assertThat(Files.exists(directory.resolve(file))).isFalse();assertThat(Files.exists(directory.resolve("other.png"))).isTrue();
  for(String bad:List.of("/api/articles/image-assets/../other.png","/api/articles/image-assets/other.png","https://evil.test"+reference,reference+"?key=other")){assertThatThrownBy(()->adapter.delete(article,bad)).isInstanceOf(IllegalArgumentException.class);}
 }
 @Test void s3_uses_configured_bucket_exact_owner_and_prefix_without_listing(){
  var properties=new ArticleImageStorageProperties();properties.setBackend("s3");properties.setS3Bucket("articles");properties.setS3Prefix("managed/articles");var refs=new ArticleManagedStorageReferences(properties);var s3=mock(software.amazon.awssdk.services.s3.S3Client.class);var adapter=new S3ArticleMediaObjectDeletion(properties,refs,s3);var article=UUID.randomUUID();String key="managed/articles/"+article+"/generated/"+UUID.randomUUID()+".webp";String ref="s3://articles/"+key;
  adapter.delete(article,ref);verify(s3).deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder().bucket("articles").key(key).build());verifyNoMoreInteractions(s3);
  for(String bad:List.of(ref.replace("s3://articles/","s3://foreign/"),ref.replace(article.toString(),UUID.randomUUID().toString()),ref.replace("managed/articles/","other/"),ref+"/../x",ref+"?version=1")){assertThatThrownBy(()->adapter.delete(article,bad)).isInstanceOf(IllegalArgumentException.class);}
  verifyNoMoreInteractions(s3);
 }
}
