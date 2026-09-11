package com.nm.fragmentsclean.userApplicationContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.userApplicationContext.read.projections.AppUserProfileView;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public final class GetAppUserProfileQueryHandler
    implements QueryHandler<GetAppUserProfileQuery, AppUserProfileView> {
  private final JdbcTemplate jdbc;

  public GetAppUserProfileQueryHandler(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public AppUserProfileView handle(GetAppUserProfileQuery query) {
    try {
      return jdbc.queryForObject(
          """
          SELECT id, display_name, avatar_url, created_at, updated_at, version
          FROM app_users
          WHERE id = ? AND lifecycle_status = 'ACTIVE'
          """,
          (rs, row) ->
              new AppUserProfileView(
                  rs.getObject("id", java.util.UUID.class),
                  rs.getString("display_name"),
                  rs.getString("avatar_url"),
                  rs.getTimestamp("created_at").toInstant(),
                  rs.getTimestamp("updated_at").toInstant(),
                  rs.getLong("version")),
          query.userId());
    } catch (EmptyResultDataAccessException missing) {
      return null;
    }
  }
}
