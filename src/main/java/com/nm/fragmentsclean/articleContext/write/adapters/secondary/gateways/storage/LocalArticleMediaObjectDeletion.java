package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.storage;
import java.util.UUID;
import java.nio.file.Files;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.nm.fragmentsclean.articleContext.media.ArticleManagedStorageReferences;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaObjectDeletion;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties;
@Component @ConditionalOnProperty(prefix="article.images.storage",name="backend",havingValue="local",matchIfMissing=true)
public final class LocalArticleMediaObjectDeletion implements ArticleMediaObjectDeletion {
 private final ArticleImageStorageProperties properties;private final ArticleManagedStorageReferences references;
 public LocalArticleMediaObjectDeletion(ArticleImageStorageProperties properties,ArticleManagedStorageReferences references){this.properties=properties;this.references=references;}
 public void delete(UUID articleId,String reference){
  String file=references.locate(articleId,reference).orElseThrow(()->new IllegalArgumentException("Unmanaged Article media reference"));
  var root=properties.getDirectory().toAbsolutePath().normalize();var target=root.resolve(file).normalize();
  if(!target.getParent().equals(root))throw new IllegalArgumentException("Invalid Article media path");
  try{Files.deleteIfExists(target);}catch(java.io.IOException e){throw new IllegalStateException("Article media deletion failed",e);}
 }
}
