package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketAccountDataEraser;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcTicketAccountDataEraser implements TicketAccountDataEraser {
  private final JdbcTemplate jdbc;

  public JdbcTicketAccountDataEraser(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void erase(UUID userId) {
    jdbc.update("DELETE FROM ticket_submission_fingerprints WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM ticket_status_projection WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM user_entitlements_projection WHERE user_id = ?", userId);
    jdbc.update("DELETE FROM tickets WHERE user_id = ?", userId);
  }
}
