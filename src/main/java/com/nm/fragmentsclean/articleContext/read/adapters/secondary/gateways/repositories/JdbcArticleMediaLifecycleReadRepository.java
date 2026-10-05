package com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.repositories;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.nm.fragmentsclean.articleContext.read.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaLifecycle;
import com.nm.fragmentsclean.articleContext.media.ArticleManagedStorageReferences;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
@Repository public class JdbcArticleMediaLifecycleReadRepository implements ArticleMediaLifecycleReadRepository {
 private final JdbcTemplate jdbc;private final DateTimeProvider clock;private final ArticleManagedStorageReferences references;
 public JdbcArticleMediaLifecycleReadRepository(JdbcTemplate jdbc,DateTimeProvider clock,ArticleManagedStorageReferences references){this.jdbc=jdbc;this.clock=clock;this.references=references;}
 public Optional<ArticleMediaLifecycleView> byId(UUID id){var now=clock.now();return jdbc.query("SELECT u.article_id,u.lifecycle_status,u.updated_at,u.storage_reference,(SELECT count(*) FROM article_revisions r WHERE trim(r.cover_reference)=u.storage_reference)+(SELECT count(*) FROM article_revision_images i WHERE trim(i.storage_reference)=u.storage_reference) AS usages FROM article_media_uploads u WHERE u.media_id=?",(rs,n)->{
  var media=new ArticleMediaLifecycle(id,rs.getObject(1,UUID.class),rs.getString(4),rs.getString(2),rs.getLong(5),rs.getTimestamp(3).toInstant());
  var retiredAt="RETIRED".equals(media.status())?media.updatedAt():null;
  return new ArticleMediaLifecycleView(id,media.articleId(),media.status(),media.usages(),media.usages()==0&&"ACTIVE".equals(media.status()),"RETIRED".equals(media.status()),"INDEFINITE_NO_AUTOMATIC_PURGE",media.canPurge(now)&&references.supports(media.articleId(),media.reference()),retiredAt,retiredAt==null?null:retiredAt.plus(ArticleMediaLifecycle.MINIMUM_RETENTION));
 },id).stream().findFirst();}
}
