package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class JdbcCoffeeProjectionRepository implements CoffeeProjectionRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcCoffeeProjectionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public long count() {
		Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM coffee_summaries_projection", Long.class);
		return n == null ? 0L : n;
	}

	@Override
	@Transactional
	public void apply(CoffeeCreatedEvent event) {
		applyIfNewer(event);
	}

	@Override
	@Transactional
	public CoffeeProjectionMutation applyIfNewer(CoffeeCreatedEvent event) {
		UUID coffeeId = event.coffeeId().value();
		ensureCheckpoint(coffeeId, event.version(), event.publicationStatus().name(), false, event.occurredAt());
		Checkpoint checkpoint = lockCheckpoint(coffeeId);
		if (checkpoint.deleted() && checkpoint.latestVersion() >= event.version()) {
			return CoffeeProjectionMutation.ignored(checkpoint.latestVersion(), checkpoint.changedAt());
		}

		if (event.version() > checkpoint.latestVersion()) {
			updateCheckpoint(coffeeId, event.version(), event.publicationStatus().name(), false, event.occurredAt());
			checkpoint = new Checkpoint(event.version(), event.publicationStatus().name(), false, event.occurredAt());
		}

		int changed = upsert(
				coffeeId,
				event.googlePlaceId() != null ? event.googlePlaceId().value() : null,
				event.name().value(),
				event.location().lat(),
				event.location().lon(),
				event.address().line1(),
				event.address().city(),
				event.address().postalCode(),
				event.address().country(),
				event.phoneNumber() != null ? event.phoneNumber().value() : null,
				event.website() != null ? event.website().value() : null,
				toTagsJsonFromEvent(event),
				checkpoint.publicationStatus(),
				null, // rating future
				Math.toIntExact(checkpoint.latestVersion()),
				Timestamp.from(checkpoint.changedAt()));
		return changed == 0
				? CoffeeProjectionMutation.ignored(checkpoint.latestVersion(), checkpoint.changedAt())
				: CoffeeProjectionMutation.applied(checkpoint.latestVersion(), checkpoint.changedAt());
	}

	@Override
	@Transactional
	public void deleteByCoffeeId(UUID coffeeId) {
		jdbcTemplate.update("DELETE FROM coffee_summaries_projection WHERE id = ?", coffeeId);
	}

	@Override
	@Transactional
	public CoffeeProjectionMutation deleteIfNewer(UUID coffeeId, long version, java.time.Instant changedAt) {
		CoffeeProjectionMutation mutation = recordLifecycleIfNewer(coffeeId, version, "ARCHIVED", true, changedAt);
		if (!mutation.applied()) return mutation;
		jdbcTemplate.update("DELETE FROM coffee_summaries_projection WHERE id = ?", coffeeId);
		return mutation;
	}

	@Override
	@Transactional
	public void markArchived(UUID coffeeId, long version, java.time.Instant updatedAt) {
		markArchivedIfNewer(coffeeId, version, updatedAt);
	}

	@Override
	@Transactional
	public CoffeeProjectionMutation markArchivedIfNewer(UUID coffeeId, long version, java.time.Instant changedAt) {
		return applyLifecycleIfNewer(coffeeId, version, "ARCHIVED", changedAt);
	}

	@Override
	@Transactional
	public void markPublished(UUID coffeeId, long version, java.time.Instant updatedAt) {
		markPublishedIfNewer(coffeeId, version, updatedAt);
	}

	@Override
	@Transactional
	public CoffeeProjectionMutation markPublishedIfNewer(UUID coffeeId, long version, java.time.Instant changedAt) {
		return applyLifecycleIfNewer(coffeeId, version, "PUBLISHED", changedAt);
	}

	@Override
	@Transactional
	public void insertSeed(CoffeeSummaryView view) {
		applyIfNewer(view);
	}

	@Override
	@Transactional
	public CoffeeProjectionMutation applyIfNewer(CoffeeSummaryView view) {
		ensureCheckpoint(view.id(), view.version(), view.publicationStatus(), false, view.updatedAt());
		Checkpoint checkpoint = lockCheckpoint(view.id());
		if (checkpoint.deleted() && checkpoint.latestVersion() >= view.version()) {
			return CoffeeProjectionMutation.ignored(checkpoint.latestVersion(), checkpoint.changedAt());
		}
		if (view.version() > checkpoint.latestVersion()) {
			updateCheckpoint(view.id(), view.version(), view.publicationStatus(), false, view.updatedAt());
			checkpoint = new Checkpoint(view.version(), view.publicationStatus(), false, view.updatedAt());
		}
		int changed = upsert(
				view.id(),
				view.googleId(),
				view.name(),
				view.latitude(),
				view.longitude(),
				view.addressLine(),
				view.city(),
				view.postalCode(),
				view.country(),
				view.phoneNumber(),
				view.website(),
				toTagsJson(view.tags()),
				checkpoint.publicationStatus(),
				null, // rating future
				Math.toIntExact(checkpoint.latestVersion()),
				Timestamp.from(checkpoint.changedAt()));
		return changed == 0
				? CoffeeProjectionMutation.ignored(checkpoint.latestVersion(), checkpoint.changedAt())
				: CoffeeProjectionMutation.applied(checkpoint.latestVersion(), checkpoint.changedAt());
	}

	@Override
	public List<CoffeeSummaryView> findAll(boolean publishedOnly) {
		String sql = """
				SELECT id,
				       google_place_id,
				       name,
				       lat,
				       lon,
				       address_line1,
				       city,
				       postal_code,
				       country,
				       phone_number,
				       website,
				       tags_json,
				       publication_status,
				       version,
				       updated_at
				FROM coffee_summaries_projection
				ORDER BY name ASC
				""";
		String filteredSql = publishedOnly ? sql.replace("ORDER BY", "WHERE publication_status = 'PUBLISHED' ORDER BY") : sql;
		return jdbcTemplate.query(filteredSql, this::mapRow);
	}

	@Override
	public List<CoffeeSummaryView> findAll() {
		return findAll(true);
	}

	@Override
	public Optional<CoffeeSummaryView> findById(UUID coffeeId, boolean publishedOnly) {
		String publicationFilter = publishedOnly ? " AND publication_status = 'PUBLISHED'" : "";
		return jdbcTemplate.query("""
				SELECT id,
				       google_place_id,
				       name,
				       lat,
				       lon,
				       address_line1,
				       city,
				       postal_code,
				       country,
				       phone_number,
				       website,
				       tags_json,
				       publication_status,
				       version,
				       updated_at
				FROM coffee_summaries_projection
				WHERE id = ?
				""" + publicationFilter, this::mapRow, coffeeId).stream().findFirst();
	}

	@Override
	public boolean isPublished(UUID coffeeId) {
		Boolean published = jdbcTemplate.queryForObject("""
				SELECT EXISTS (
				    SELECT 1
				    FROM coffee_summaries_projection
				    WHERE id = ? AND publication_status = 'PUBLISHED'
				)
				""", Boolean.class, coffeeId);
		return Boolean.TRUE.equals(published);
	}

	// ----------------- private -----------------

	private int upsert(
			UUID id,
			String googlePlaceId,
			String name,
			double lat,
			double lon,
			String addressLine1,
			String city,
			String postalCode,
			String country,
			String phoneNumber,
			String website,
			String tagsJson,
			String publicationStatus,
			Double rating,
			int version,
			Timestamp updatedAt) {
		return jdbcTemplate.update(
				"""
						INSERT INTO coffee_summaries_projection (
						    id,
						    google_place_id,
						    name,
						    address_line1,
						    city,
						    postal_code,
						    country,
						    lat,
						    lon,
						    phone_number,
						    website,
						    tags_json,
						    publication_status,
						    rating,
						    version,
						    updated_at
						) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
						ON CONFLICT (id) DO UPDATE SET
						    google_place_id = EXCLUDED.google_place_id,
						    name = EXCLUDED.name,
						    address_line1 = EXCLUDED.address_line1,
						    city = EXCLUDED.city,
						    postal_code = EXCLUDED.postal_code,
						    country = EXCLUDED.country,
						    lat = EXCLUDED.lat,
						    lon = EXCLUDED.lon,
						    phone_number = EXCLUDED.phone_number,
						    website = EXCLUDED.website,
						    tags_json = EXCLUDED.tags_json,
						    publication_status = EXCLUDED.publication_status,
						    rating = EXCLUDED.rating,
						    version = EXCLUDED.version,
						    updated_at = EXCLUDED.updated_at
						WHERE coffee_summaries_projection.version < EXCLUDED.version
						""",
				id,
				googlePlaceId,
				name,
				addressLine1,
				city,
				postalCode,
				country,
				lat,
				lon,
				phoneNumber,
				website,
				tagsJson,
				publicationStatus,
				rating,
				version,
				updatedAt);
	}

	private CoffeeProjectionMutation applyLifecycleIfNewer(UUID coffeeId, long version, String status,
			java.time.Instant changedAt) {
		CoffeeProjectionMutation mutation = recordLifecycleIfNewer(coffeeId, version, status, false, changedAt);
		if (!mutation.applied()) return mutation;
		jdbcTemplate.update("""
				UPDATE coffee_summaries_projection
				SET publication_status = ?, version = ?, updated_at = ?
				WHERE id = ? AND version < ?
				""", status, version, Timestamp.from(changedAt), coffeeId, version);
		return mutation;
	}

	private CoffeeProjectionMutation recordLifecycleIfNewer(UUID coffeeId, long version, String status,
			boolean deleted, java.time.Instant changedAt) {
		ensureCheckpoint(coffeeId, -1, status, false, changedAt);
		Checkpoint checkpoint = lockCheckpoint(coffeeId);
		if (version <= checkpoint.latestVersion()) {
			return CoffeeProjectionMutation.ignored(checkpoint.latestVersion(), checkpoint.changedAt());
		}
		updateCheckpoint(coffeeId, version, status, deleted, changedAt);
		return CoffeeProjectionMutation.applied(version, changedAt);
	}

	private void ensureCheckpoint(UUID coffeeId, long version, String status, boolean deleted,
			java.time.Instant changedAt) {
		jdbcTemplate.update("""
				INSERT INTO coffee_projection_checkpoints
				    (coffee_id, latest_version, publication_status, deleted, changed_at)
				VALUES (?, ?, ?, ?, ?)
				ON CONFLICT (coffee_id) DO NOTHING
				""", coffeeId, version, status, deleted, Timestamp.from(changedAt));
	}

	private Checkpoint lockCheckpoint(UUID coffeeId) {
		return jdbcTemplate.queryForObject("""
				SELECT latest_version, publication_status, deleted, changed_at
				FROM coffee_projection_checkpoints
				WHERE coffee_id = ?
				FOR UPDATE
				""", (rs, rowNum) -> new Checkpoint(
				rs.getLong("latest_version"),
				rs.getString("publication_status"),
				rs.getBoolean("deleted"),
				rs.getTimestamp("changed_at").toInstant()), coffeeId);
	}

	private void updateCheckpoint(UUID coffeeId, long version, String status, boolean deleted,
			java.time.Instant changedAt) {
		jdbcTemplate.update("""
				UPDATE coffee_projection_checkpoints
				SET latest_version = ?, publication_status = ?, deleted = ?, changed_at = ?
				WHERE coffee_id = ?
				""", version, status, deleted, Timestamp.from(changedAt), coffeeId);
	}

	private record Checkpoint(long latestVersion, String publicationStatus, boolean deleted,
			java.time.Instant changedAt) { }

	private CoffeeSummaryView mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		String googleId = rs.getString("google_place_id");
		String name = rs.getString("name");
		double latitude = rs.getDouble("lat");
		double longitude = rs.getDouble("lon");

		String addressLine = rs.getString("address_line1");
		String city = rs.getString("city");
		String postalCode = rs.getString("postal_code");
		String country = rs.getString("country");
		String phoneNumber = rs.getString("phone_number");
		String website = rs.getString("website");

		String tagsJson = rs.getString("tags_json");
		Set<String> tags = parseTagsJson(tagsJson);
		String publicationStatus = rs.getString("publication_status");

		long version = rs.getLong("version");
		var updatedAt = rs.getTimestamp("updated_at").toInstant();

		return new CoffeeSummaryView(
				id,
				googleId,
				name,
				latitude,
				longitude,
				addressLine,
				city,
				postalCode,
				country,
				phoneNumber,
				website,
				tags,
				publicationStatus == null ? "PUBLISHED" : publicationStatus,
				version,
				updatedAt);
	}

	private String toTagsJsonFromEvent(CoffeeCreatedEvent event) {
		if (event.tags() == null || event.tags().isEmpty())
			return "[]";
		String joined = event.tags().stream()
				.map(t -> "\"" + t.value().replace("\"", "\\\"") + "\"")
				.collect(Collectors.joining(","));
		return "[" + joined + "]";
	}

	private String toTagsJson(Set<String> tags) {
		if (tags == null || tags.isEmpty())
			return "[]";
		String joined = tags.stream()
				.map(t -> "\"" + t.replace("\"", "\\\"") + "\"")
				.collect(Collectors.joining(","));
		return "[" + joined + "]";
	}

	private Set<String> parseTagsJson(String tagsJson) {
		if (tagsJson == null || tagsJson.isBlank() || tagsJson.equals("[]"))
			return Set.of();
		String trimmed = tagsJson.trim();
		if (trimmed.startsWith("["))
			trimmed = trimmed.substring(1);
		if (trimmed.endsWith("]"))
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		if (trimmed.isBlank())
			return Set.of();

		return Arrays.stream(trimmed.split(","))
				.map(String::trim)
				.map(s -> s.replace("\"", ""))
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}
}
