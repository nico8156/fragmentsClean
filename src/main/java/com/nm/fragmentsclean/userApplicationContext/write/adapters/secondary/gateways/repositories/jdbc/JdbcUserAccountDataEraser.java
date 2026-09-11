package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.UserAccountDataEraser;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcUserAccountDataEraser implements UserAccountDataEraser {
  private final JdbcTemplate jdbc;

  public JdbcUserAccountDataEraser(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void erase(UUID userId) {
    jdbc.update("UPDATE user_avatar_media SET status='DELETION_PENDING',user_id=NULL,updated_at=now(),version=version+1 WHERE user_id=? AND status<>'DELETED'", userId);
    jdbc.update("DELETE FROM user_saved_coffees_projection WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM saved_coffees WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM pass_ticket_contributions WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM pass_experience_contributions WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM user_pass_projection WHERE user_id = ?", userId);
  }
}
