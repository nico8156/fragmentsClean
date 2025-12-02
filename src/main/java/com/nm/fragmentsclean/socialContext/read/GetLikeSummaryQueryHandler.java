package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.projections.LikeSummaryView;
import org.springframework.jdbc.core.JdbcTemplate;

public class GetLikeSummaryQueryHandler implements QueryHandler<GetLikeSummaryQuery, LikeSummaryView> {
    private final JdbcTemplate jdbcTemplate;

    public GetLikeSummaryQueryHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LikeSummaryView handle(GetLikeSummaryQuery query) {

        // 1) est-ce que l'utilisateur a liké ?
        Boolean active = jdbcTemplate.query(
                """
                SELECT active
                FROM social_likes_projection
                WHERE user_id = ? AND target_id = ?
                """,
                new Object[]{query.userId(), query.targetId()},
                rs -> rs.next() ? rs.getBoolean("active") : Boolean.FALSE
        );

        // 2) combien de likes sur ce target ?
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM social_likes_projection
                WHERE target_id = ? AND active = true
                """,
                new Object[]{query.targetId()},
                Long.class
        );

        return new LikeSummaryView(
                query.userId(),
                query.targetId(),
                active != null && active,
                count != null ? count : 0L
        );
    }
}
