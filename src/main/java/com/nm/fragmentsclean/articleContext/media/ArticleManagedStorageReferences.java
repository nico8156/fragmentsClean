package com.nm.fragmentsclean.articleContext.media;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;
/** Resolves only references emitted by the configured Article storage adapters. */
@Component public final class ArticleManagedStorageReferences implements com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaPurgeCapability {
 private static final String FILE="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|webp|gif)";
 private final ArticleImageStorageProperties properties;
 public ArticleManagedStorageReferences(ArticleImageStorageProperties properties){this.properties=properties;}
 public Optional<String> locate(UUID articleId,String reference){
  if(articleId==null||reference==null)return Optional.empty();
  if("s3".equals(properties.getBackend())){
   String bucket=properties.getS3Bucket().trim();String prefix=properties.getS3Prefix().replaceAll("^/+|/+$","");
   if(bucket.isBlank()||prefix.isBlank())return Optional.empty();
   String start="s3://"+bucket+"/"+prefix+"/"+articleId+"/";
   if(!reference.startsWith(start)||!reference.substring(start.length()).matches("(images|generated)/"+FILE))return Optional.empty();
   return Optional.of(reference.substring(("s3://"+bucket+"/").length()));
  }
  if(!"local".equals(properties.getBackend()))return Optional.empty();
  String start="/api/articles/image-assets/";String publicBase=properties.getPublicBaseUrl().replaceAll("/+$","");
  String candidate=reference.startsWith(start)?reference.substring(start.length()):!publicBase.isBlank()&&reference.startsWith(publicBase+start)?reference.substring((publicBase+start).length()):null;
  return candidate!=null&&Pattern.matches(FILE,candidate)?Optional.of(candidate):Optional.empty();
 }
 public boolean supports(UUID articleId,String reference){return locate(articleId,reference).isPresent();}
}
