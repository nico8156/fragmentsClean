package com.nm.fragmentsclean.mediaCatalogContext.read.adapters.secondary;
import com.nm.fragmentsclean.mediaCatalogContext.read.ArticleMediaCatalogProjection;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleMediaCatalogSnapshotIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleMediaCatalogSnapshotIntegrationEvent.Reference;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
public class JdbcArticleMediaCatalogProjection implements ArticleMediaCatalogProjection {
    private final JdbcTemplate jdbc;private final ObjectMapper json;
    public JdbcArticleMediaCatalogProjection(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}
    @Transactional public boolean apply(ArticleMediaCatalogSnapshotIntegrationEvent e){
        jdbc.update("INSERT INTO media_catalog_article_versions(article_id) VALUES(?) ON CONFLICT DO NOTHING",e.articleId());
        long current=jdbc.queryForObject("SELECT snapshot_version FROM media_catalog_article_versions WHERE article_id=? FOR UPDATE",Long.class,e.articleId());
        if(e.version()<=current)return false;
        String payload=encode(e.references());
        jdbc.update("INSERT INTO media_catalog_article_parts(article_id,version,part,parts,payload_json) VALUES(?,?,?,?,?) ON CONFLICT DO NOTHING",e.articleId(),e.version(),e.part(),e.parts(),payload);
        var part=jdbc.queryForMap("SELECT parts,payload_json FROM media_catalog_article_parts WHERE article_id=? AND version=? AND part=?",e.articleId(),e.version(),e.part());
        if(((Number)part.get("parts")).intValue()!=e.parts() || !payload.equals(part.get("payload_json")))throw new IllegalStateException("Conflicting article media inventory part");
        var counts=jdbc.queryForMap("SELECT count(*) AS total,min(parts) AS smallest,max(parts) AS largest FROM media_catalog_article_parts WHERE article_id=? AND version=?",e.articleId(),e.version());
        if(((Number)counts.get("smallest")).intValue()!=e.parts() || ((Number)counts.get("largest")).intValue()!=e.parts())throw new IllegalStateException("Conflicting article media part count");
        if(((Number)counts.get("total")).intValue()!=e.parts())return false;
        var references=new ArrayList<Reference>();
        for(String data:jdbc.query("SELECT payload_json FROM media_catalog_article_parts WHERE article_id=? AND version=? ORDER BY part",(rs,n)->rs.getString(1),e.articleId(),e.version()))references.addAll(decode(data));
        var ids=new TreeSet<UUID>(Comparator.comparing(UUID::toString));
        ids.addAll(jdbc.query("SELECT DISTINCT media_id FROM media_catalog_article_references WHERE article_id=?",(rs,n)->rs.getObject(1,UUID.class),e.articleId()));
        references.forEach(r->ids.add(r.mediaId()));
        // Deterministic global asset locking serializes snapshots from different articles.
        var placeholders=ids.stream().map(id->new Object[]{id,Timestamp.from(e.occurredAt())}).toList();
        jdbc.batchUpdate("INSERT INTO media_catalog_entries(origin,media_id,status,size_bytes,updated_at,source_version) VALUES('ARTICLE',?,'AVAILABLE',0,?,0) ON CONFLICT DO NOTHING",placeholders);
        String array="{"+String.join(",",ids.stream().map(UUID::toString).toList())+"}";
        jdbc.query("SELECT media_id FROM media_catalog_entries WHERE origin='ARTICLE' AND media_id=ANY(?::uuid[]) ORDER BY media_id FOR UPDATE",(rs,n)->rs.getObject(1),array);
        jdbc.update("DELETE FROM media_catalog_article_references WHERE article_id=?",e.articleId());
        jdbc.batchUpdate("INSERT INTO media_catalog_article_references(article_id,role,usage_id,media_id,storage_reference,payload_json) VALUES(?,?,?,?,?,?::jsonb)",references.stream().map(r->new Object[]{e.articleId(),r.role(),r.usageId(),r.mediaId(),r.storageReference(),encode(r)}).toList());
        jdbc.update("""
            WITH grouped AS (
              SELECT media_id,count(*) AS total,count(DISTINCT article_id) FILTER(WHERE role<>'UPLOAD') AS articles,
                min(article_id::text) FILTER(WHERE role<>'UPLOAD') AS resource,
                max(storage_reference) AS reference,
                max(payload_json->>'contentType') FILTER(WHERE role='UPLOAD') AS content_type,
                max((payload_json->>'size')::bigint) FILTER(WHERE role='UPLOAD') AS size,
                bool_or(payload_json->>'uploadStatus'='RETIRED') FILTER(WHERE role='UPLOAD') AS retired,
                min((payload_json->>'uploadedAt')::timestamptz) FILTER(WHERE role='UPLOAD') AS uploaded_at,
                max(payload_json->>'uploadedBy') FILTER(WHERE role='UPLOAD') AS uploaded_by,
                max(payload_json->>'purpose') FILTER(WHERE role='UPLOAD') AS purpose,
                max(payload_json->>'originalName') FILTER(WHERE role='UPLOAD') AS original_name,
                max((payload_json->>'width')::integer) FILTER(WHERE role='UPLOAD') AS stored_width,
                max((payload_json->>'height')::integer) FILTER(WHERE role='UPLOAD') AS stored_height,
                min((payload_json->>'width')::integer) AS min_width,max((payload_json->>'width')::integer) AS max_width,
                min((payload_json->>'height')::integer) AS min_height,max((payload_json->>'height')::integer) AS max_height
              FROM media_catalog_article_references WHERE media_id=ANY(?::uuid[]) GROUP BY media_id
            ), affected AS (SELECT m.media_id AS affected_media_id,g.* FROM media_catalog_entries m LEFT JOIN grouped g ON g.media_id=m.media_id WHERE m.origin='ARTICLE' AND m.media_id=ANY(?::uuid[]))
            UPDATE media_catalog_entries m SET status=CASE WHEN a.total IS NULL THEN 'DELETED' WHEN a.retired THEN 'DELETION_PENDING' ELSE 'AVAILABLE' END,
              resource_id=CASE WHEN a.articles=1 THEN a.resource::uuid ELSE NULL END,owner_id=NULL,object_key=a.reference,
              content_type=a.content_type,size_bytes=coalesce(a.size,0),created_at=a.uploaded_at,
              width=coalesce(a.stored_width,CASE WHEN a.min_width=a.max_width THEN a.min_width END),
              height=coalesce(a.stored_height,CASE WHEN a.min_height=a.max_height THEN a.min_height END),
              uploaded_by=a.uploaded_by::uuid,purpose=a.purpose,original_name=a.original_name,
              updated_at=greatest(m.updated_at,?),source_version=m.source_version+1
            FROM affected a WHERE m.origin='ARTICLE' AND m.media_id=a.affected_media_id
            """,array,array,Timestamp.from(e.occurredAt()));
        jdbc.update("UPDATE media_catalog_article_versions SET snapshot_version=? WHERE article_id=?",e.version(),e.articleId());
        jdbc.update("DELETE FROM media_catalog_article_parts WHERE article_id=? AND version<=?",e.articleId(),e.version());
        return true;
    }
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("Cannot serialize article media projection",e);}}
    private List<Reference> decode(String value){try{return json.readValue(value,new TypeReference<List<Reference>>(){});}catch(Exception e){throw new IllegalStateException("Cannot read article media projection",e);}}
}
