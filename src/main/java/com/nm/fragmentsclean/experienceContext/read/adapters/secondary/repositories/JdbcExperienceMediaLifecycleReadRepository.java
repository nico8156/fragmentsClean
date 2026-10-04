package com.nm.fragmentsclean.experienceContext.read.adapters.secondary.repositories;
import com.nm.fragmentsclean.experienceContext.read.*;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository public class JdbcExperienceMediaLifecycleReadRepository implements ExperienceMediaLifecycleReadRepository {
    private final JdbcTemplate jdbc;
    public JdbcExperienceMediaLifecycleReadRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public Optional<ExperienceMediaLifecycleView> byId(UUID id) {
        return jdbc.query("""
            SELECT m.media_id,m.experience_id,m.coffee_id,m.user_id,m.status,
                   e.publication_status,e.moderation_status
            FROM experience_media m LEFT JOIN experiences e ON e.experience_id=m.experience_id
            WHERE m.media_id=?
            """, (rs,n) -> {
                String publication=rs.getString("publication_status"), moderation=rs.getString("moderation_status");
                boolean reviewable=publication!=null && !"DELETED".equals(publication);
                return new ExperienceMediaLifecycleView(id,rs.getObject("experience_id",UUID.class),
                    rs.getObject("coffee_id",UUID.class),rs.getObject("user_id",UUID.class),rs.getString("status"),
                    publication,moderation,reviewable && "VISIBLE".equals(moderation),reviewable && "HIDDEN".equals(moderation));
            },id).stream().findFirst();
    }
}
