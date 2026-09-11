package com.nm.fragmentsclean.authenticationContext.read.adapters.secondary;

import com.nm.fragmentsclean.authenticationContext.read.AuthAccountStatusReader;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcAuthAccountStatusReader implements AuthAccountStatusReader {
  private final JdbcTemplate jdbc;

  public JdbcAuthAccountStatusReader(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean isActive(UUID userId) {
    Integer count =
        jdbc.queryForObject(
            "SELECT count(*) FROM auth_users WHERE id=? AND lifecycle_status='ACTIVE'",
            Integer.class,
            userId);
    return count != null && count == 1;
  }
}
