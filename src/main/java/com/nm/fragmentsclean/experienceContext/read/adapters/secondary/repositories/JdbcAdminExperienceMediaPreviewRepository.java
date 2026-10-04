package com.nm.fragmentsclean.experienceContext.read.adapters.secondary.repositories;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.AdminExperienceMediaPreviewRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcAdminExperienceMediaPreviewRepository implements AdminExperienceMediaPreviewRepository {
    private final JdbcTemplate jdbc;
    private final PrivateMediaUrlResolver urls;
    public JdbcAdminExperienceMediaPreviewRepository(JdbcTemplate jdbc,PrivateMediaUrlResolver urls){this.jdbc=jdbc;this.urls=urls;}
    public Map<UUID,String> availablePreviews(List<UUID> ids){
        if(ids.isEmpty())return Map.of();
        String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        var rows=jdbc.query("SELECT media_id,object_key FROM experience_media WHERE status='AVAILABLE' AND user_id IS NOT NULL AND object_key IS NOT NULL AND media_id IN ("+placeholders+")",(rs,n)->Map.entry(rs.getObject("media_id",UUID.class),rs.getString("object_key")),ids.toArray());
        var result=new HashMap<UUID,String>();
        for(var row:rows)result.put(row.getKey(),urls.resolve(PrivateMediaReferences.experience(row.getValue())));
        return Map.copyOf(result);
    }
}
