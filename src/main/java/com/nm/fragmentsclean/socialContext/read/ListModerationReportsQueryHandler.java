package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.socialContext.read.projections.ModerationActionView;
import com.nm.fragmentsclean.socialContext.read.projections.ModerationReportView;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

public final class ListModerationReportsQueryHandler {
    private final JdbcTemplate jdbc;
    public ListModerationReportsQueryHandler(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<ModerationReportView> handle(String status, int requestedLimit) {
        int limit = Math.min(Math.max(requestedLimit, 1), 100);
        String normalizedStatus = status == null || status.isBlank() ? "OPEN" : status;
        var reports = jdbc.query("""
            SELECT report.report_id, report.comment_id, report.target_id, report.author_id,
                   author.display_name AS author_name, report.reporter_id, report.reason, report.details,
                   report.status, comment.body, report.created_at,
                   (SELECT count(*) FROM social_content_reports_projection same_comment
                    WHERE same_comment.comment_id=report.comment_id) AS report_count
            FROM social_content_reports_projection report
            LEFT JOIN social_comments_projection comment ON comment.id=report.comment_id
            LEFT JOIN user_social_projection author ON author.user_id=report.author_id
            WHERE report.status=? ORDER BY report.created_at ASC LIMIT ?
            """, (rs, row) -> new ModerationReportView(rs.getObject("report_id", UUID.class),
                rs.getObject("comment_id", UUID.class), rs.getObject("target_id", UUID.class),
                rs.getObject("author_id", UUID.class), rs.getString("author_name"),
                rs.getObject("reporter_id", UUID.class), rs.getString("reason"), rs.getString("details"),
                rs.getString("status"), rs.getString("body"), rs.getLong("report_count"),
                rs.getTimestamp("created_at").toInstant(), List.of()), normalizedStatus, limit);
        return reports.stream().map(report -> new ModerationReportView(report.reportId(), report.commentId(),
                report.targetId(), report.authorId(), report.authorName(), report.reporterId(), report.reason(),
                report.details(), report.status(), report.content(), report.reportCount(), report.createdAt(),
                actions(report.reportId()))).toList();
    }

    private List<ModerationActionView> actions(UUID reportId) {
        return jdbc.query("""
            SELECT action_id, operator_id, decision, reason, occurred_at
            FROM social_moderation_actions_projection WHERE report_id=? ORDER BY occurred_at DESC
            """, (rs, row) -> new ModerationActionView(rs.getObject("action_id", UUID.class),
                rs.getObject("operator_id", UUID.class), rs.getString("decision"), rs.getString("reason"),
                rs.getTimestamp("occurred_at").toInstant()), reportId);
    }
}
