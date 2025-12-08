package com.nm.fragmentsclean.authenticationContext.read;

import com.nm.fragmentsclean.authenticationContext.read.projections.AuthMeView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class GetMeQueryHandler implements QueryHandler<GetMeQuery, AuthMeView> {

    private final JdbcTemplate jdbcTemplate;

    public GetMeQueryHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AuthMeView handle(GetMeQuery query) {
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT id, display_name
                    FROM app_users
                    WHERE id = ?
                    """,
                    (rs, rowNum) -> new AuthMeView(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("display_name")
                    ),
                    query.userId()
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
