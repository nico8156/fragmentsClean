package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaUploadRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaUpload;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcArticleMediaUploadRepository implements ArticleMediaUploadRepository {
    private final JdbcTemplate jdbc;public JdbcArticleMediaUploadRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public void save(ArticleMediaUpload u){
        int count=jdbc.update("""
            INSERT INTO article_media_uploads(media_id,article_id,storage_reference,original_name,content_type,size_bytes,width,height,uploaded_by,purpose,uploaded_at,updated_at)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(media_id) DO UPDATE SET
              content_type=CASE WHEN article_media_uploads.lifecycle_status='ACTIVE' THEN excluded.content_type ELSE article_media_uploads.content_type END,
              size_bytes=CASE WHEN article_media_uploads.lifecycle_status='ACTIVE' THEN excluded.size_bytes ELSE article_media_uploads.size_bytes END,
              width=CASE WHEN article_media_uploads.lifecycle_status='ACTIVE' THEN excluded.width ELSE article_media_uploads.width END,
              height=CASE WHEN article_media_uploads.lifecycle_status='ACTIVE' THEN excluded.height ELSE article_media_uploads.height END,
              updated_at=CASE WHEN article_media_uploads.lifecycle_status='ACTIVE' THEN excluded.updated_at ELSE article_media_uploads.updated_at END
            WHERE article_media_uploads.article_id=excluded.article_id AND article_media_uploads.storage_reference=excluded.storage_reference
            """,u.mediaId(),u.articleId(),u.storageReference(),u.originalName(),u.contentType(),u.size(),u.width(),u.height(),u.uploadedBy(),u.purpose(),Timestamp.from(u.uploadedAt()),Timestamp.from(u.uploadedAt()));
        if(count!=1)throw new IllegalStateException("Conflicting stored article media identity");
    }
}
