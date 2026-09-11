package com.nm.fragmentsclean.experienceContext.read.adapters.secondary.repositories;

import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceProjectionRepository;
import com.nm.fragmentsclean.platform.eventing.contracts.*;
import java.sql.Timestamp;import java.time.Instant;import java.util.UUID;import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.stereotype.Repository;

@Repository
public class JdbcExperienceProjectionRepository implements ExperienceProjectionRepository {
    private final JdbcTemplate jdbc;public JdbcExperienceProjectionRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public void apply(ExperienceIntegrationEvents.SnapshotChanged e){jdbc.update("""
        INSERT INTO experience_views(experience_id,user_id,coffee_id,message,publication_status,moderation_status,created_at,updated_at,deleted_at,version)
        VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(experience_id) DO UPDATE SET message=EXCLUDED.message,
        publication_status=EXCLUDED.publication_status,moderation_status=EXCLUDED.moderation_status,
        updated_at=EXCLUDED.updated_at,deleted_at=EXCLUDED.deleted_at,version=EXCLUDED.version
        WHERE experience_views.version<EXCLUDED.version
        """,e.experienceId(),e.userId(),e.coffeeId(),e.message(),e.publicationStatus(),e.moderationStatus(),Timestamp.from(e.createdAt()),Timestamp.from(e.updatedAt()),timestamp(e.deletedAt()),e.version());}
    public void apply(ExperienceIntegrationEvents.Reported e){jdbc.update("""
        INSERT INTO experience_reports_projection(report_id,experience_id,coffee_id,author_id,reporter_id,reason,details,status,created_at,resolved_at,version)
        VALUES(?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(report_id) DO NOTHING
        """,e.reportId(),e.experienceId(),e.coffeeId(),e.authorId(),e.reporterId(),e.reason(),e.details(),e.status(),Timestamp.from(e.occurredAt()),null,e.version());}
    public void apply(ExperienceIntegrationEvents.Moderated e){
        jdbc.update("UPDATE experience_views SET moderation_status=?,updated_at=?,version=? WHERE experience_id=? AND version<?",e.moderationStatus(),Timestamp.from(e.occurredAt()),e.version(),e.experienceId(),e.version());
        jdbc.update("UPDATE experience_reports_projection SET status=?,resolved_at=?,version=version+1,moderation_version=? WHERE experience_id=? AND status='OPEN' AND moderation_version<?",e.reportStatus(),Timestamp.from(e.occurredAt()),e.version(),e.experienceId(),e.version());
        jdbc.update("UPDATE experience_reports_projection SET status=?,resolved_at=?,version=version+1,moderation_version=? WHERE report_id=? AND moderation_version<?",e.reportStatus(),Timestamp.from(e.occurredAt()),e.version(),e.reportId(),e.version());
        jdbc.update("INSERT INTO experience_moderation_actions_projection(action_id,report_id,experience_id,operator_id,decision,reason,occurred_at) VALUES(?,?,?,?,?,?,?) ON CONFLICT(action_id) DO NOTHING",e.actionId(),e.reportId(),e.experienceId(),e.operatorId(),e.moderationStatus(),e.reason(),Timestamp.from(e.occurredAt()));
    }
    public void upsertProfile(UUID userId,String displayName,String avatarUrl,long version,Instant at){jdbc.update("""
        INSERT INTO experience_user_profiles(user_id,display_name,avatar_url,updated_at,version) VALUES(?,?,?,?,?)
        ON CONFLICT(user_id) DO UPDATE SET display_name=EXCLUDED.display_name,avatar_url=EXCLUDED.avatar_url,
        updated_at=EXCLUDED.updated_at,version=EXCLUDED.version WHERE experience_user_profiles.version<EXCLUDED.version
        """,userId,displayName==null||displayName.isBlank()?"Utilisateur":displayName,avatarUrl,Timestamp.from(at),version);}
    public void upsertCoffee(UUID coffeeId,boolean active,long version,Instant at){jdbc.update("""
        INSERT INTO experience_coffee_references(coffee_id,active,updated_at,version) VALUES(?,?,?,?)
        ON CONFLICT(coffee_id) DO UPDATE SET active=EXCLUDED.active,updated_at=EXCLUDED.updated_at,version=EXCLUDED.version
        WHERE experience_coffee_references.version<EXCLUDED.version
        """,coffeeId,active,Timestamp.from(at),version);}
    public void upsertBlock(UUID blockId,UUID blockerId,UUID blockedUserId,boolean active,long version,Instant at){jdbc.update("""
        INSERT INTO experience_user_blocks(block_id,blocker_id,blocked_user_id,active,updated_at,version) VALUES(?,?,?,?,?,?)
        ON CONFLICT(block_id) DO UPDATE SET active=EXCLUDED.active,updated_at=EXCLUDED.updated_at,version=EXCLUDED.version
        WHERE experience_user_blocks.version<EXCLUDED.version
        """,blockId,blockerId,blockedUserId,active,Timestamp.from(at),version);}
    private static Timestamp timestamp(Instant value){return value==null?null:Timestamp.from(value);}
}
