package com.nm.fragmentsclean.experienceContext.read.adapters.secondary.repositories;

import com.nm.fragmentsclean.experienceContext.read.ExperienceCursor;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceReadRepository;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceModerationActionView;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceModerationReportView;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceMediaView;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperiencePage;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaReferences;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaUrlResolver;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcExperienceReadRepository implements ExperienceReadRepository {
  private final JdbcTemplate jdbc;
  private final PrivateMediaUrlResolver mediaUrls;

  public JdbcExperienceReadRepository(JdbcTemplate jdbc, PrivateMediaUrlResolver mediaUrls) {
    this.jdbc = jdbc;
    this.mediaUrls = mediaUrls;
  }

  @Override
  public ExperiencePage listForCoffee(
      UUID requesterId, UUID coffeeId, String rawCursor, int requestedLimit) {
    ExperienceCursor cursor = ExperienceCursor.parse(rawCursor);
    int limit = boundedLimit(requestedLimit);
    var sql =
        new StringBuilder(
            """
            SELECT view.*, profile.display_name, profile.avatar_url
            FROM experience_views view
            LEFT JOIN experience_user_profiles profile ON profile.user_id = view.user_id
            WHERE view.coffee_id = ?
              AND view.publication_status = 'PUBLISHED'
              AND view.moderation_status = 'VISIBLE'
              AND view.deleted_at IS NULL
              AND NOT EXISTS (
                SELECT 1 FROM experience_reports_projection report
                WHERE report.experience_id = view.experience_id AND report.reporter_id = ?
              )
              AND NOT EXISTS (
                SELECT 1 FROM experience_user_blocks block
                WHERE block.blocker_id = ? AND block.blocked_user_id = view.user_id AND block.active = TRUE
              )
            """);
    List<Object> parameters = new ArrayList<>(List.of(coffeeId, requesterId, requesterId));
    appendCursor(sql, parameters, cursor);
    return page(sql, parameters, limit);
  }

  @Override
  public ExperiencePage listForUser(UUID userId, String rawCursor, int requestedLimit) {
    ExperienceCursor cursor = ExperienceCursor.parse(rawCursor);
    int limit = boundedLimit(requestedLimit);
    var sql =
        new StringBuilder(
            """
            SELECT view.*, profile.display_name, profile.avatar_url
            FROM experience_views view
            LEFT JOIN experience_user_profiles profile ON profile.user_id = view.user_id
            WHERE view.user_id = ?
              AND view.publication_status <> 'DELETED'
              AND view.deleted_at IS NULL
            """);
    List<Object> parameters = new ArrayList<>(List.of(userId));
    appendCursor(sql, parameters, cursor);
    return page(sql, parameters, limit);
  }

  @Override
  public List<ExperienceModerationReportView> listModerationReports(
      String requestedStatus, int requestedLimit) {
    String status = requestedStatus == null || requestedStatus.isBlank() ? "OPEN" : requestedStatus;
    int limit = Math.min(Math.max(requestedLimit, 1), 100);
    var reports =
        jdbc.query(
            """
            SELECT report.*, view.message, profile.display_name,
              (SELECT count(*) FROM experience_reports_projection same
               WHERE same.experience_id = report.experience_id) report_count
            FROM experience_reports_projection report
            LEFT JOIN experience_views view ON view.experience_id = report.experience_id
            LEFT JOIN experience_user_profiles profile ON profile.user_id = report.author_id
            WHERE report.status = ?
            ORDER BY report.created_at ASC
            LIMIT ?
            """,
            (rs, row) ->
                new ExperienceModerationReportView(
                    rs.getObject("report_id", UUID.class),
                    rs.getObject("experience_id", UUID.class),
                    rs.getObject("coffee_id", UUID.class),
                    rs.getObject("author_id", UUID.class),
                    rs.getString("display_name"),
                    rs.getObject("reporter_id", UUID.class),
                    rs.getString("reason"),
                    rs.getString("details"),
                    rs.getString("status"),
                    rs.getString("message"),
                    rs.getLong("report_count"),
                    rs.getTimestamp("created_at").toInstant(),
                    List.of(),
                    List.of()),
            status,
            limit);
    var media = mediaForExperiences(reports.stream().map(ExperienceModerationReportView::experienceId).toList());
    return reports.stream()
		.map(report -> withMedia(report, media.getOrDefault(report.experienceId(), List.of())))
        .map(report -> withActions(report, moderationActions(report.reportId())))
        .toList();
  }

  private ExperiencePage page(StringBuilder sql, List<Object> parameters, int limit) {
    sql.append(" ORDER BY view.created_at DESC, view.experience_id DESC LIMIT ?");
    parameters.add(limit + 1);
    var rows =
        jdbc.query(
            sql.toString(),
            (rs, row) ->
                new ExperienceView(
                    rs.getObject("experience_id", UUID.class),
                    rs.getObject("coffee_id", UUID.class),
                    rs.getObject("user_id", UUID.class),
                    Optional.ofNullable(rs.getString("display_name")).orElse("Utilisateur"),
                    mediaUrls.resolve(rs.getString("avatar_url")),
                    rs.getString("message"),
                    rs.getString("publication_status"),
                    rs.getString("moderation_status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant(),
					rs.getLong("version"),
					List.of()),
            parameters.toArray());
    boolean hasMore = rows.size() > limit;
	var baseItems = hasMore ? List.copyOf(rows.subList(0, limit)) : List.copyOf(rows);
	var media = mediaForExperiences(baseItems.stream().map(ExperienceView::experienceId).toList());
	var items = baseItems.stream().map(item -> withMedia(item, media.getOrDefault(item.experienceId(), List.of()))).toList();
    String nextCursor =
        hasMore && !items.isEmpty()
            ? new ExperienceCursor(items.getLast().createdAt(), items.getLast().experienceId()).encode()
            : null;
    return new ExperiencePage(items, nextCursor, Instant.now());
  }

  private List<ExperienceModerationActionView> moderationActions(UUID reportId) {
    return jdbc.query(
        """
        SELECT action_id, operator_id, decision, reason, occurred_at
        FROM experience_moderation_actions_projection
        WHERE report_id = ?
        ORDER BY occurred_at DESC
        """,
        (rs, row) ->
            new ExperienceModerationActionView(
                rs.getObject("action_id", UUID.class),
                rs.getObject("operator_id", UUID.class),
                rs.getString("decision"),
                rs.getString("reason"),
                rs.getTimestamp("occurred_at").toInstant()),
        reportId);
  }

  private static ExperienceModerationReportView withActions(
      ExperienceModerationReportView report, List<ExperienceModerationActionView> actions) {
    return new ExperienceModerationReportView(
        report.reportId(), report.experienceId(), report.coffeeId(), report.authorId(),
        report.authorName(), report.reporterId(), report.reason(), report.details(), report.status(),
        report.content(), report.reportCount(), report.createdAt(), report.media(), actions);
  }

  private ExperienceModerationReportView withMedia(ExperienceModerationReportView report,List<ExperienceMediaView> media){return new ExperienceModerationReportView(report.reportId(),report.experienceId(),report.coffeeId(),report.authorId(),report.authorName(),report.reporterId(),report.reason(),report.details(),report.status(),report.content(),report.reportCount(),report.createdAt(),media,report.actions());}

	private static ExperienceView withMedia(ExperienceView item,List<ExperienceMediaView> media){return new ExperienceView(item.experienceId(),item.coffeeId(),item.authorId(),item.authorName(),item.avatarUrl(),item.message(),item.publicationStatus(),item.moderationStatus(),item.createdAt(),item.updatedAt(),item.version(),media);}

	private Map<UUID,List<ExperienceMediaView>> mediaForExperiences(List<UUID> experienceIds){
		if(experienceIds.isEmpty())return Map.of();
		String placeholders=String.join(",",java.util.Collections.nCopies(experienceIds.size(),"?"));
		var rows=jdbc.query("SELECT experience_id,media_id,object_key,width,height,position FROM experience_media_views WHERE experience_id IN ("+placeholders+") AND status='AVAILABLE' AND object_key IS NOT NULL ORDER BY experience_id,position,media_id",(rs,row)->Map.entry(rs.getObject("experience_id",UUID.class),new ExperienceMediaView(rs.getObject("media_id",UUID.class),mediaUrls.resolve(PrivateMediaReferences.experience(rs.getString("object_key"))),rs.getObject("width",Integer.class),rs.getObject("height",Integer.class),rs.getInt("position"))),experienceIds.toArray());
		Map<UUID,List<ExperienceMediaView>> result=new LinkedHashMap<>();
		for(var row:rows)result.computeIfAbsent(row.getKey(),ignored->new ArrayList<>()).add(row.getValue());
		return result;
	}

  private static void appendCursor(
      StringBuilder sql, List<Object> parameters, ExperienceCursor cursor) {
    if (cursor == null) return;
    sql.append(" AND (view.created_at < ? OR (view.created_at = ? AND view.experience_id < ?))");
    parameters.add(Timestamp.from(cursor.createdAt()));
    parameters.add(Timestamp.from(cursor.createdAt()));
    parameters.add(cursor.id());
  }

  private static int boundedLimit(int requested) {
    return Math.min(Math.max(requested <= 0 ? 20 : requested, 1), 50);
  }
}
