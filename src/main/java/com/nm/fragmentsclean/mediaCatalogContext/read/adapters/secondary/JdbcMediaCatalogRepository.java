package com.nm.fragmentsclean.mediaCatalogContext.read.adapters.secondary;
import com.nm.fragmentsclean.mediaCatalogContext.read.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcMediaCatalogRepository implements MediaCatalogProjection, MediaCatalogReadRepository, CoffeeMediaCatalogProjection {
    private final JdbcTemplate jdbc;
    public JdbcMediaCatalogRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public boolean apply(MediaCatalogEntry e) {
        return jdbc.update("""
            INSERT INTO media_catalog_entries(origin,media_id,resource_id,owner_id,status,object_key,content_type,size_bytes,width,height,created_at,updated_at,source_version)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(origin,media_id) DO UPDATE SET
            resource_id=excluded.resource_id,owner_id=excluded.owner_id,status=excluded.status,
            object_key=excluded.object_key,content_type=excluded.content_type,size_bytes=excluded.size_bytes,
            width=excluded.width,height=excluded.height,created_at=coalesce(excluded.created_at,media_catalog_entries.created_at),
            updated_at=excluded.updated_at,source_version=excluded.source_version
            WHERE excluded.source_version > media_catalog_entries.source_version
               OR (excluded.source_version = media_catalog_entries.source_version AND media_catalog_entries.created_at IS NULL AND excluded.created_at IS NOT NULL)
            """,e.origin(),e.mediaId(),e.resourceId(),e.ownerId(),e.status(),e.objectKey(),e.contentType(),e.size(),e.width(),e.height(),time(e.createdAt()),time(e.updatedAt()),e.version())>0;
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
            if(!deleted){sql.append(" AND source_version<=?");args.add(version);}
            if(!entries.isEmpty()){sql.append(" AND media_id NOT IN (").append(String.join(",",Collections.nCopies(entries.size(),"?"))).append(")");entries.forEach(e->args.add(e.mediaId()));}
            jdbc.update(sql.toString(),args.toArray());
        }
        entries.forEach(this::apply);
    }
    public void erase(UUID ownerId){jdbc.update("DELETE FROM media_catalog_entries WHERE owner_id=?",ownerId);}
    public MediaCatalogPage search(SearchMediaCatalogQuery q) {
        var sql=new StringBuilder("SELECT * FROM media_catalog_entries WHERE (?='' OR position(lower(?) in lower(media_id::text))>0 OR resource_id::text=? OR owner_id::text=?)");
        var args=new ArrayList<Object>(List.of(q.q(),q.q(),q.q(),q.q()));
        if(q.origin()!=null){sql.append(" AND origin=?");args.add(q.origin());}
        if(q.status()!=null){sql.append(" AND status=?");args.add(q.status());}
        if(q.ownerId()!=null){sql.append(" AND owner_id=?");args.add(q.ownerId());}
        if(q.cursor()!=null){var c=SearchMediaCatalogQuery.parseId(q.cursor());sql.append(" AND (origin,media_id)>(?,?)");args.add(c[0]);args.add(UUID.fromString(c[1]));}
        sql.append(" ORDER BY origin,media_id LIMIT ?");args.add(q.limit()+1);
        var rows=jdbc.query(sql.toString(),this::view,args.toArray());
        boolean more=rows.size()>q.limit();var items=List.copyOf(rows.subList(0,Math.min(q.limit(),rows.size())));
        return new MediaCatalogPage(items,more?items.getLast().id():null,List.of("EXPERIENCE","COFFEE"));
    }
    public Optional<MediaCatalogView> byId(String id) {
        var parts=SearchMediaCatalogQuery.parseId(id);
        return jdbc.query("SELECT * FROM media_catalog_entries WHERE origin=? AND media_id=?",this::view,parts[0],UUID.fromString(parts[1])).stream().findFirst();
    }
    private MediaCatalogView view(ResultSet rs,int row)throws SQLException {
        String origin=rs.getString("origin"),status=rs.getString("status");
        UUID id=rs.getObject("media_id",UUID.class);
        String preview=null; // Enriched in one batch by the owning domain query.
        var created=rs.getTimestamp("created_at");long size=rs.getLong("size_bytes");
        return new MediaCatalogView(origin+":"+id,origin,id,rs.getObject("resource_id",UUID.class),rs.getObject("owner_id",UUID.class),status,preview,rs.getString("content_type"),size>0?size:null,rs.getObject("width",Integer.class),rs.getObject("height",Integer.class),created==null?null:created.toInstant(),rs.getTimestamp("updated_at").toInstant());
    }
    private static Timestamp time(Instant value){return value==null?null:Timestamp.from(value);}
}
