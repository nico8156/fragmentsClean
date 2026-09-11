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
    jdbc.update("DELETE FROM social_comments_projection WHERE author_id = ?", userId);
    jdbc.update("DELETE FROM social_likes_projection WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM comments WHERE author_id = ?", userId);
    jdbc.update("DELETE FROM likes WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM user_social_projection WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM users WHERE user_id = ?", userId);
  }
}
