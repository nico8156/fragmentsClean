package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jdbc;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceAccountDataEraser;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcExperienceAccountDataEraser implements ExperienceAccountDataEraser{
    private final JdbcTemplate jdbc;public JdbcExperienceAccountDataEraser(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Override public void erase(UUID userId){
        jdbc.update("UPDATE experience_media SET status='DELETION_PENDING',user_id=NULL,updated_at=now(),version=version+1 WHERE user_id=? AND status<>'DELETED'",userId);
        jdbc.update("DELETE FROM experience_media_views WHERE user_id=?",userId);
        jdbc.update("DELETE FROM experience_moderation_actions_projection WHERE operator_id=? OR report_id IN (SELECT report_id FROM experience_reports_projection WHERE author_id=? OR reporter_id=?)",userId,userId,userId);
        jdbc.update("DELETE FROM experience_reports_projection WHERE author_id=? OR reporter_id=?",userId,userId);
        jdbc.update("DELETE FROM experience_reports WHERE author_id=? OR reporter_id=?",userId,userId);
        jdbc.update("DELETE FROM experience_views WHERE user_id=?",userId);
        jdbc.update("DELETE FROM experiences WHERE user_id=?",userId);
        jdbc.update("DELETE FROM experience_user_profiles WHERE user_id=?",userId);
        jdbc.update("DELETE FROM experience_user_blocks WHERE blocker_id=? OR blocked_user_id=?",userId,userId);
    }
}
