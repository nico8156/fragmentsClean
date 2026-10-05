package com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.nm.fragmentsclean.socialContext.read.AdminModerationReportReadRepository;
import com.nm.fragmentsclean.socialContext.read.projections.*;
@Repository public class JdbcAdminModerationReportReadRepository implements AdminModerationReportReadRepository {
 private final JdbcTemplate jdbc;
 public JdbcAdminModerationReportReadRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public Optional<ModerationReportView> findModerationReport(UUID reportId){
  var reports=jdbc.query("""
   SELECT report.*, author.display_name AS author_name, comment.body,
     (SELECT count(*) FROM social_content_reports_projection same WHERE same.comment_id=report.comment_id) AS report_count
   FROM social_content_reports_projection report
   LEFT JOIN social_comments_projection comment ON comment.id=report.comment_id
   LEFT JOIN user_social_projection author ON author.user_id=report.author_id
   WHERE report.report_id=?
   """,(rs,row)->new ModerationReportView(rs.getObject("report_id",UUID.class),rs.getObject("comment_id",UUID.class),rs.getObject("target_id",UUID.class),rs.getObject("author_id",UUID.class),rs.getString("author_name"),rs.getObject("reporter_id",UUID.class),rs.getString("reason"),rs.getString("details"),rs.getString("status"),rs.getString("body"),rs.getLong("report_count"),rs.getTimestamp("created_at").toInstant(),List.of()),reportId);
  if(reports.isEmpty())return Optional.empty();
  var actions=jdbc.query("""
   SELECT action_id,operator_id,decision,reason,occurred_at FROM social_moderation_actions_projection
   WHERE report_id=? ORDER BY occurred_at DESC,action_id DESC
   """,(rs,row)->new ModerationActionView(rs.getObject("action_id",UUID.class),rs.getObject("operator_id",UUID.class),rs.getString("decision"),rs.getString("reason"),rs.getTimestamp("occurred_at").toInstant()),reportId);
  var r=reports.getFirst();return Optional.of(new ModerationReportView(r.reportId(),r.commentId(),r.targetId(),r.authorId(),r.authorName(),r.reporterId(),r.reason(),r.details(),r.status(),r.content(),r.reportCount(),r.createdAt(),actions));
 }
}
