package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceMediaCatalogScan;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcExperienceMediaCatalogScan implements ExperienceMediaCatalogScan {
    private final JdbcTemplate jdbc;
    public JdbcExperienceMediaCatalogScan(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public Batch lockNext(int limit) {
        var checkpoints=jdbc.query("SELECT cursor_id FROM experience_media_catalog_scan WHERE id=1 AND next_scan_at<=now() FOR UPDATE SKIP LOCKED",(rs,n)->Optional.ofNullable(rs.getObject(1,UUID.class)));
        if(checkpoints.isEmpty())return new Batch(false,List.of());
        var cursor=checkpoints.getFirst();
        String sql="SELECT * FROM experience_media"+(cursor.isPresent()?" WHERE media_id>?":"")+" ORDER BY media_id LIMIT ?";
        var args=new ArrayList<Object>();cursor.ifPresent(args::add);args.add(limit);
        var rows=jdbc.query(sql,(rs,n)->new ExperienceMedia.Snapshot(rs.getObject("media_id",UUID.class),rs.getObject("experience_id",UUID.class),rs.getObject("coffee_id",UUID.class),rs.getObject("user_id",UUID.class),rs.getString("declared_content_type"),rs.getLong("declared_size"),rs.getString("pending_object_key"),ExperienceMediaStatus.valueOf(rs.getString("status")),rs.getString("object_key"),rs.getString("content_type"),rs.getLong("size_bytes"),rs.getObject("width",Integer.class),rs.getObject("height",Integer.class),rs.getString("sha256"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant(),rs.getLong("version")),args.toArray());
        return new Batch(true,rows);
    }
    public void advance(UUID cursor,boolean complete){
        if(complete)jdbc.update("UPDATE experience_media_catalog_scan SET cursor_id=NULL,next_scan_at=now()+interval '1 day',completed_at=now() WHERE id=1");
        else jdbc.update("UPDATE experience_media_catalog_scan SET cursor_id=? WHERE id=1",cursor);
    }
}
