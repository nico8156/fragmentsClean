package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AvatarMediaCatalogScan;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcAvatarMediaCatalogScan implements AvatarMediaCatalogScan {
    private final JdbcTemplate jdbc;
    public JdbcAvatarMediaCatalogScan(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public Batch lockNext(int limit) {
        var checkpoints=jdbc.query("SELECT cursor_id FROM avatar_media_catalog_scan WHERE id=1 AND next_scan_at<=now() FOR UPDATE SKIP LOCKED",(rs,n)->Optional.ofNullable(rs.getObject(1,UUID.class)));
        if(checkpoints.isEmpty())return new Batch(false,List.of());
        var cursor=checkpoints.getFirst();
        String sql="SELECT m.*,CASE WHEN m.status='AVAILABLE' AND u.lifecycle_status='ACTIVE' AND u.avatar_url='media:avatar:' || m.object_key THEN u.id ELSE NULL END AS profile_user_id FROM user_avatar_media m LEFT JOIN app_users u ON u.id=m.user_id"+(cursor.isPresent()?" WHERE m.media_id>?":"")+" ORDER BY m.media_id LIMIT ?";
        var args=new ArrayList<Object>();cursor.ifPresent(args::add);args.add(limit);
        var rows=jdbc.query(sql,(rs,n)->new Item(new AvatarMedia.Snapshot(rs.getObject("media_id",UUID.class),rs.getObject("user_id",UUID.class),rs.getString("declared_content_type"),rs.getLong("declared_size"),rs.getString("pending_object_key"),AvatarMediaStatus.valueOf(rs.getString("status")),rs.getString("object_key"),rs.getString("content_type"),rs.getLong("size_bytes"),rs.getObject("width",Integer.class),rs.getObject("height",Integer.class),rs.getString("sha256"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant(),rs.getLong("version")),rs.getObject("profile_user_id",UUID.class)),args.toArray());
        return new Batch(true,rows);
    }
    public void advance(UUID cursor,boolean complete){
        if(complete)jdbc.update("UPDATE avatar_media_catalog_scan SET cursor_id=NULL,next_scan_at=now()+interval '1 day',completed_at=now() WHERE id=1");
        else jdbc.update("UPDATE avatar_media_catalog_scan SET cursor_id=? WHERE id=1",cursor);
    }
}
