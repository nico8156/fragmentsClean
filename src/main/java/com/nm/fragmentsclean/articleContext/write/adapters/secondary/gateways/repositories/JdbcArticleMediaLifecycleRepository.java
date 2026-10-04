package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;
import java.util.*;import java.time.Instant;import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Repository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaLifecycleRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaLifecycle;
@Repository public class JdbcArticleMediaLifecycleRepository implements ArticleMediaLifecycleRepository {
 private final JdbcTemplate jdbc;public JdbcArticleMediaLifecycleRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public Optional<ArticleMediaLifecycle> lock(UUID id){
  var rows=jdbc.query("SELECT article_id,storage_reference,lifecycle_status,updated_at FROM article_media_uploads WHERE media_id=? FOR UPDATE",(rs,n)->new ArticleMediaLifecycle(id,rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),0,rs.getTimestamp(4).toInstant()),id);
  return rows.stream().findFirst().map(row->new ArticleMediaLifecycle(id,row.articleId(),row.reference(),row.status(),jdbc.queryForObject("SELECT (SELECT count(*) FROM article_revisions WHERE trim(cover_reference)=?)+(SELECT count(*) FROM article_revision_images WHERE trim(storage_reference)=?)",Long.class,row.reference(),row.reference()),row.updatedAt()));
 }
 public List<UUID> pendingPurgeIds(int limit){if(limit<1||limit>100)throw new IllegalArgumentException("Purge batch must be 1 to 100");return jdbc.query("SELECT media_id FROM article_media_uploads WHERE lifecycle_status='DELETION_PENDING' ORDER BY updated_at,media_id LIMIT ?",(rs,n)->rs.getObject(1,UUID.class),limit);}
 public void change(UUID id,String status,Instant at){jdbc.update("UPDATE article_media_uploads SET lifecycle_status=?,updated_at=? WHERE media_id=?",status,Timestamp.from(at),id);}
}
