package com.nm.fragmentsclean.mediaCatalogContext.read.adapters.secondary;
import com.nm.fragmentsclean.mediaCatalogContext.read.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcMediaCatalogRepository implements MediaCatalogProjection, MediaCatalogReadRepository, CoffeeMediaCatalogProjection {
    private static final String USAGE="""
        CASE WHEN m.origin='ARTICLE' THEN CASE
          WHEN EXISTS(SELECT 1 FROM media_catalog_article_references u WHERE u.media_id=m.media_id AND u.role<>'UPLOAD') THEN 'USED'
          WHEN EXISTS(SELECT 1 FROM media_catalog_article_references u WHERE u.media_id=m.media_id AND u.role='UPLOAD') THEN 'UNUSED'
          ELSE 'UNKNOWN' END
        WHEN m.origin='COFFEE' AND (m.status='DELETION_PENDING' OR m.physically_deleted) THEN 'UNUSED'
        WHEN m.resource_id IS NOT NULL THEN 'USED'
        WHEN m.origin='AVATAR' AND m.status<>'DELETED' THEN 'UNUSED' ELSE 'UNKNOWN' END
        """;
    private final JdbcTemplate jdbc;
    public JdbcMediaCatalogRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public boolean apply(MediaCatalogEntry e) {return apply(e,false);}
    private boolean apply(MediaCatalogEntry e,boolean retainedInventory) {
        return jdbc.update("""
            INSERT INTO media_catalog_entries(origin,media_id,resource_id,owner_id,status,object_key,content_type,size_bytes,width,height,created_at,updated_at,source_version,physically_deleted)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(origin,media_id) DO UPDATE SET
            resource_id=excluded.resource_id,owner_id=excluded.owner_id,status=excluded.status,
            object_key=excluded.object_key,content_type=excluded.content_type,size_bytes=excluded.size_bytes,
            width=excluded.width,height=excluded.height,created_at=coalesce(excluded.created_at,media_catalog_entries.created_at),
            updated_at=excluded.updated_at,source_version=excluded.source_version,physically_deleted=media_catalog_entries.physically_deleted OR excluded.physically_deleted
            WHERE (NOT media_catalog_entries.physically_deleted OR excluded.physically_deleted) AND ( excluded.source_version > media_catalog_entries.source_version
               OR (excluded.source_version = media_catalog_entries.source_version AND media_catalog_entries.created_at IS NULL AND excluded.created_at IS NOT NULL)
               OR (excluded.source_version = media_catalog_entries.source_version AND excluded.physically_deleted AND NOT media_catalog_entries.physically_deleted)
               OR (? AND excluded.origin='COFFEE' AND excluded.source_version = media_catalog_entries.source_version AND media_catalog_entries.status='DELETED' AND excluded.status='DELETION_PENDING'))
            """,e.origin(),e.mediaId(),e.resourceId(),e.ownerId(),e.status(),e.objectKey(),e.contentType(),e.size(),e.width(),e.height(),time(e.createdAt()),time(e.updatedAt()),e.version(),e.physicallyDeleted(),retainedInventory)>0;
    }
    @org.springframework.transaction.annotation.Transactional
    public void applyCoffee(UUID coffeeId,List<MediaCatalogEntry> entries,long version,Instant at,boolean complete,boolean deleted) {
        jdbc.update("INSERT INTO media_catalog_coffee_versions(coffee_id) VALUES(?) ON CONFLICT DO NOTHING",coffeeId);
        var state=jdbc.queryForMap("SELECT snapshot_version,deleted FROM media_catalog_coffee_versions WHERE coffee_id=? FOR UPDATE",coffeeId);
        long snapshot=((Number)state.get("snapshot_version")).longValue();
        if(Boolean.TRUE.equals(state.get("deleted")) || (complete?version<snapshot:version<=snapshot))return;
        if(complete) {
            jdbc.update("UPDATE media_catalog_coffee_versions SET snapshot_version=greatest(snapshot_version,?),deleted=? WHERE coffee_id=?",version,deleted,coffeeId);
            var sql=new StringBuilder("UPDATE media_catalog_entries SET status='DELETED',resource_id=NULL,owner_id=NULL,object_key=NULL,content_type=NULL,size_bytes=0,width=NULL,height=NULL,updated_at=?,source_version=greatest(source_version,?) WHERE origin='COFFEE' AND resource_id=?");
            var args=new ArrayList<Object>(List.of(time(at),version,coffeeId));
            if(!deleted){sql.append(" AND source_version<=? AND status<>'DELETION_PENDING'");args.add(version);}
            if(!entries.isEmpty()){sql.append(" AND media_id NOT IN (").append(String.join(",",Collections.nCopies(entries.size(),"?"))).append(")");entries.forEach(e->args.add(e.mediaId()));}
            jdbc.update(sql.toString(),args.toArray());
        }
        entries.forEach(e->apply(e,complete && !deleted));
    }
    public void erase(UUID ownerId){jdbc.update("DELETE FROM media_catalog_entries WHERE owner_id=?",ownerId);}
    public MediaCatalogPage search(SearchMediaCatalogQuery q) {
        var sql=new StringBuilder("SELECT m.*, "+USAGE+" AS usage_status FROM media_catalog_entries m WHERE (?='' OR position(lower(?) in lower(m.media_id::text))>0 OR m.resource_id::text=? OR m.owner_id::text=? OR position(lower(?) in lower(coalesce(m.original_name,'')))>0 OR EXISTS(SELECT 1 FROM media_catalog_article_references ar WHERE ar.media_id=m.media_id AND m.origin='ARTICLE' AND (ar.article_id::text=? OR position(lower(?) in lower(coalesce(ar.payload_json->>'title','')))>0)))");
        var args=new ArrayList<Object>(List.of(q.q(),q.q(),q.q(),q.q(),q.q(),q.q(),q.q()));
        if(q.usage()!=null){sql.append(" AND (").append(USAGE).append(")=?");args.add(q.usage());}
        if(q.origin()!=null){sql.append(" AND m.origin=?");args.add(q.origin());}
        if(q.status()!=null){sql.append(" AND m.status=?");args.add(q.status());}
        if(q.ownerId()!=null){sql.append(" AND m.owner_id=?");args.add(q.ownerId());}
        if(q.cursor()!=null){var c=SearchMediaCatalogQuery.parseId(q.cursor());sql.append(" AND (m.origin,m.media_id)>(?,?)");args.add(c[0]);args.add(UUID.fromString(c[1]));}
        sql.append(" ORDER BY m.origin,m.media_id LIMIT ?");args.add(q.limit()+1);
        var rows=jdbc.query(sql.toString(),this::view,args.toArray());
        boolean more=rows.size()>q.limit();var items=List.copyOf(rows.subList(0,Math.min(q.limit(),rows.size())));
        return new MediaCatalogPage(items,more?items.getLast().id():null,List.of("EXPERIENCE","COFFEE","AVATAR","ARTICLE"));
    }
    public Optional<MediaCatalogView> byId(String id) {
        var parts=SearchMediaCatalogQuery.parseId(id);
        return jdbc.query("SELECT m.*, "+USAGE+" AS usage_status FROM media_catalog_entries m WHERE m.origin=? AND m.media_id=?",this::view,parts[0],UUID.fromString(parts[1])).stream().findFirst();
    }
    public Map<UUID,String> articleReferences(List<UUID> ids){
        if(ids.isEmpty())return Map.of();var result=new HashMap<UUID,String>();
        String array="{"+String.join(",",ids.stream().map(UUID::toString).toList())+"}";
        jdbc.query("SELECT media_id,object_key FROM media_catalog_entries WHERE origin='ARTICLE' AND status='AVAILABLE' AND object_key IS NOT NULL AND media_id=ANY(?::uuid[])",rs->{result.put(rs.getObject(1,UUID.class),rs.getString(2));},array);return Map.copyOf(result);
    }
    public MediaCatalogArticleUsagePage articleUsages(ListMediaCatalogArticleUsagesQuery q){
        var id=UUID.fromString(SearchMediaCatalogQuery.parseId(q.id())[1]);
        var sql=new StringBuilder("SELECT article_id,role,usage_id,payload_json FROM media_catalog_article_references WHERE media_id=? AND role<>'UPLOAD'");
        var args=new ArrayList<Object>(List.of(id));
        if(q.cursor()!=null){var c=ListMediaCatalogArticleUsagesQuery.parseCursor(q.cursor());sql.append(" AND (article_id,role,usage_id)>(?,?,?)");args.add(UUID.fromString(c[0]));args.add(c[1]);args.add(UUID.fromString(c[2]));}
        sql.append(" ORDER BY article_id,role,usage_id LIMIT ?");args.add(q.limit()+1);
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var rows=jdbc.query(sql.toString(),(rs,n)->{
            try{var ref=json.readTree(rs.getString("payload_json"));
                var view=new MediaCatalogArticleUsageView(rs.getObject("article_id",UUID.class),UUID.fromString(ref.path("revisionId").asText()),ref.path("revisionNumber").asInt(),ref.path("title").asText(),ref.path("revisionStatus").asText(),rs.getString("role"),integer(ref,"sectionPosition"),integer(ref,"imagePosition"),integer(ref,"width"),integer(ref,"height"),ref.path("working").asBoolean(),ref.path("published").asBoolean());
                return new UsageRow(view,rs.getString("article_id")+":"+rs.getString("role")+":"+rs.getString("usage_id"));
            }catch(java.io.IOException e){throw new java.sql.SQLException("Invalid article usage projection",e);}
        },args.toArray());
        boolean more=rows.size()>q.limit();var items=rows.subList(0,Math.min(q.limit(),rows.size()));
        return new MediaCatalogArticleUsagePage(items.stream().map(UsageRow::view).toList(),more?items.getLast().cursor():null);
    }
    private static Integer integer(com.fasterxml.jackson.databind.JsonNode json,String field){var value=json.get(field);return value==null || value.isNull()?null:value.asInt();}
    private record UsageRow(MediaCatalogArticleUsageView view,String cursor){}
    private MediaCatalogView view(ResultSet rs,int row)throws SQLException {
        String origin=rs.getString("origin"),status=rs.getString("status");
        UUID id=rs.getObject("media_id",UUID.class);
        String preview=null; // Enriched in one batch by the owning domain query.
        var created=rs.getTimestamp("created_at");long size=rs.getLong("size_bytes");
        return new MediaCatalogView(origin+":"+id,origin,id,rs.getObject("resource_id",UUID.class),rs.getObject("owner_id",UUID.class),status,preview,rs.getString("content_type"),size>0?size:null,rs.getObject("width",Integer.class),rs.getObject("height",Integer.class),created==null?null:created.toInstant(),rs.getTimestamp("updated_at").toInstant(),rs.getString("original_name"),rs.getObject("uploaded_by",UUID.class),rs.getString("purpose"),rs.getString("usage_status"),null);
    }
    private static Timestamp time(Instant value){return value==null?null:Timestamp.from(value);}
}
