package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.SocialAccountDataEraser;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcSocialAccountDataEraser implements SocialAccountDataEraser {
  private final JdbcTemplate jdbc;

  public JdbcSocialAccountDataEraser(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void erase(UUID userId) {
    jdbc.update("""
        DELETE FROM social_moderation_actions_projection action
        WHERE action.operator_id = ? OR action.report_id IN (
          SELECT report_id FROM social_content_reports_projection
          WHERE reporter_id = ? OR author_id = ?
        )
        """, userId, userId, userId);
    jdbc.update("DELETE FROM social_content_reports_projection WHERE reporter_id = ? OR author_id = ?", userId, userId);
    jdbc.update("DELETE FROM social_user_blocks_projection WHERE blocker_id = ? OR blocked_user_id = ?", userId, userId);
    jdbc.update("DELETE FROM content_reports WHERE reporter_id = ? OR author_id = ?", userId, userId);
    jdbc.update("DELETE FROM user_blocks WHERE blocker_id = ? OR blocked_user_id = ?", userId, userId);
    jdbc.update("DELETE FROM social_comments_projection WHERE author_id = ?", userId);
    jdbc.update("DELETE FROM social_likes_projection WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM comments WHERE author_id = ?", userId);
    jdbc.update("DELETE FROM likes WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM user_social_projection WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM users WHERE user_id = ?", userId);
  }
}
