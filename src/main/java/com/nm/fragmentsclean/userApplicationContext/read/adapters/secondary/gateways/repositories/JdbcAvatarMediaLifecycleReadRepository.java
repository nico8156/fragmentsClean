package com.nm.fragmentsclean.userApplicationContext.read.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.userApplicationContext.read.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository public class JdbcAvatarMediaLifecycleReadRepository implements AvatarMediaLifecycleReadRepository {
  private final JdbcTemplate jdbc;
  public JdbcAvatarMediaLifecycleReadRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
  public Optional<AvatarMediaLifecycleView> byId(UUID id){
    return jdbc.query("""
      SELECT m.user_id,m.status,u.lifecycle_status,
        (SELECT count(*) FROM app_users used WHERE used.avatar_url='media:avatar:' || m.object_key) AS usages,
        EXISTS(SELECT 1 FROM user_avatar_media other WHERE other.user_id=m.user_id
          AND other.media_id<>m.media_id AND other.status='AVAILABLE') AS another_available
      FROM user_avatar_media m LEFT JOIN app_users u ON u.id=m.user_id WHERE m.media_id=?
      """,(rs,n)->{
        long usages=rs.getLong("usages"); String status=rs.getString("status");
        boolean active="ACTIVE".equals(rs.getString("lifecycle_status"));
        return new AvatarMediaLifecycleView(id,rs.getObject("user_id",UUID.class),status,usages,
          active && usages==0 && "AVAILABLE".equals(status),
          active && usages==0 && "RETIRED".equals(status) && !rs.getBoolean("another_available"),
          "AVAILABLE".equals(status)||"RETIRED".equals(status)?"INDEFINITE_NO_AUTOMATIC_PURGE":"EXISTING_SOURCE_LIFECYCLE");
      },id).stream().findFirst();
  }
}
