package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaCatalogSource;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaCatalogSnapshotEvent.Reference;
import com.nm.fragmentsclean.articleContext.read.ArticleMediaReferenceIdentity;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcArticleMediaCatalogSource implements ArticleMediaCatalogSource {
    private final JdbcTemplate jdbc;
    public JdbcArticleMediaCatalogSource(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public Snapshot nextSnapshot(UUID articleId){
        jdbc.update("INSERT INTO article_media_catalog_versions(article_id) VALUES(?) ON CONFLICT DO NOTHING",articleId);
        long version=jdbc.queryForObject("UPDATE article_media_catalog_versions SET version=version+1 WHERE article_id=? RETURNING version",Long.class,articleId);
        var references=jdbc.query("""
            SELECT r.cover_reference AS reference,r.revision_id AS usage_id,r.revision_id,r.revision_number,r.title,r.status AS revision_status,
              'COVER' AS role,NULL::integer AS section_position,0 AS image_position,r.cover_alt AS alt,r.cover_width AS width,r.cover_height AS height,
              coalesce(r.revision_id=a.working_revision_id,false) AS working,coalesce(r.revision_id=a.published_revision_id,false) AS published,
              NULL::text AS content_type,NULL::bigint AS size,NULL::timestamptz AS uploaded_at,NULL::uuid AS uploaded_by,NULL::text AS purpose,NULL::text AS original_name,'ACTIVE' AS upload_status
            FROM article_revisions r JOIN articles a ON a.article_id=r.article_id WHERE r.article_id=? AND r.cover_reference IS NOT NULL AND length(trim(r.cover_reference))>0
            UNION ALL
            SELECT i.storage_reference,i.image_id,r.revision_id,r.revision_number,r.title,r.status,'SECTION',s.position,i.position,i.alt,i.width,i.height,
              coalesce(r.revision_id=a.working_revision_id,false),coalesce(r.revision_id=a.published_revision_id,false),NULL,NULL,NULL,NULL,NULL,NULL,'ACTIVE'
            FROM article_revision_images i JOIN article_revisions r ON r.revision_id=i.revision_id JOIN articles a ON a.article_id=r.article_id
            LEFT JOIN article_revision_sections s ON s.section_id=i.section_id AND s.revision_id=r.revision_id
            WHERE r.article_id=? AND length(trim(i.storage_reference))>0
            UNION ALL
            SELECT storage_reference,media_id,NULL,NULL,NULL,NULL,'UPLOAD',NULL,NULL,NULL,width,height,false,false,content_type,size_bytes,uploaded_at,uploaded_by,purpose,original_name,lifecycle_status
            FROM article_media_uploads WHERE article_id=?
            """,(rs,n)->{
                String reference=rs.getString("reference").trim();var at=rs.getTimestamp("uploaded_at");
                return new Reference(ArticleMediaReferenceIdentity.of(reference),reference.length()>8192?null:reference,rs.getObject("usage_id",UUID.class),rs.getObject("revision_id",UUID.class),rs.getObject("revision_number",Integer.class),bounded(rs.getString("title"),300),rs.getString("revision_status"),rs.getString("role"),rs.getObject("section_position",Integer.class),rs.getObject("image_position",Integer.class),bounded(rs.getString("alt"),2000),rs.getObject("width",Integer.class),rs.getObject("height",Integer.class),rs.getBoolean("working"),rs.getBoolean("published"),bounded(rs.getString("content_type"),128),rs.getObject("size",Long.class),at==null?null:at.toInstant(),rs.getObject("uploaded_by",UUID.class),rs.getString("purpose"),bounded(rs.getString("original_name"),255),rs.getString("upload_status"));
            },articleId,articleId,articleId);
        var boundIds=references.stream().filter(ref->!ref.role().equals("UPLOAD")).map(ref->ref.mediaId().toString()).distinct().sorted().toList();
        if(!boundIds.isEmpty()){
            var states=jdbc.query("SELECT lifecycle_status FROM article_media_uploads WHERE media_id=ANY(?::uuid[]) ORDER BY media_id FOR SHARE",(rs,n)->rs.getString(1),"{"+String.join(",",boundIds)+"}");
            states.forEach(com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaLifecycle::requireActive);
        }
        return new Snapshot(version,references);
    }
    private static String bounded(String value,int limit){return value==null?null:value.substring(0,Math.min(limit,value.length()));}
}
