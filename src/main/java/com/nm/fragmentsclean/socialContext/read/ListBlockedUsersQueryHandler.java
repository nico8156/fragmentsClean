package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.socialContext.read.projections.BlockedUserView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaUrlResolver;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class ListBlockedUsersQueryHandler {
    private final JdbcTemplate jdbc;
    private final PrivateMediaUrlResolver mediaUrls;
    public ListBlockedUsersQueryHandler(JdbcTemplate jdbc,PrivateMediaUrlResolver mediaUrls) { this.jdbc = jdbc;this.mediaUrls=mediaUrls; }
    public List<BlockedUserView> handle(UUID requesterId) {
        return jdbc.query("""
            SELECT block.block_id, block.blocked_user_id, users.display_name, users.avatar_url,
                   block.updated_at, block.version
            FROM social_user_blocks_projection block
            LEFT JOIN user_social_projection users ON users.user_id=block.blocked_user_id
            WHERE block.blocker_id=? AND block.active=TRUE ORDER BY block.updated_at DESC
            """, (rs, row) -> new BlockedUserView(rs.getObject("block_id", UUID.class),
                rs.getObject("blocked_user_id", UUID.class), rs.getString("display_name"),
                mediaUrls.resolve(rs.getString("avatar_url")), rs.getTimestamp("updated_at").toInstant(), rs.getLong("version")), requesterId);
    }
}
