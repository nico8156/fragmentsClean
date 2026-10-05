package com.nm.fragmentsclean.userApplicationContext.read.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.userApplicationContext.read.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaUrlResolver;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcAdminAvatarMediaPreviewRepository implements AdminAvatarMediaPreviewRepository {
    private final JdbcTemplate jdbc;private final PrivateMediaUrlResolver resolver;
    public JdbcAdminAvatarMediaPreviewRepository(JdbcTemplate jdbc,PrivateMediaUrlResolver resolver){this.jdbc=jdbc;this.resolver=resolver;}
    public Map<UUID,String> currentPreviews(Map<UUID,UUID> mediaProfiles){
        if(mediaProfiles.isEmpty())return Map.of();
        var clauses=new ArrayList<String>();var args=new ArrayList<Object>();
        mediaProfiles.forEach((media,profile)->{clauses.add("(m.media_id=? AND m.user_id=?)");args.add(media);args.add(profile);});
        Map<UUID,String> result=new HashMap<>();
        jdbc.query("SELECT m.media_id,m.object_key FROM user_avatar_media m JOIN app_users u ON u.id=m.user_id WHERE m.status='AVAILABLE' AND u.lifecycle_status='ACTIVE' AND m.object_key IS NOT NULL AND u.avatar_url='media:avatar:' || m.object_key AND ("+String.join(" OR ",clauses)+")",rs->{
            result.put(rs.getObject("media_id",UUID.class),resolver.resolve("media:avatar:"+rs.getString("object_key")));
        },args.toArray());
        return Map.copyOf(result);
    }
}
