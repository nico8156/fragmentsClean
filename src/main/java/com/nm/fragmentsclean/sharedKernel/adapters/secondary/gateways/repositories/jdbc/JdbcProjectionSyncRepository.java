package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncRepository;

@Repository
public class JdbcProjectionSyncRepository implements ProjectionSyncRepository {
	private final JdbcTemplate jdbcTemplate;
	private final ObjectMapper objectMapper;

	public JdbcProjectionSyncRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
		this.jdbcTemplate = jdbcTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public ProjectionSyncEvent append(ProjectionSyncEvent event) {
		Long id = jdbcTemplate.queryForObject("""
				INSERT INTO projection_sync_events (
				    event_name,
				    projection,
				    scope,
				    entity_id,
				    version,
				    changed_at,
				    payload_json
				) VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
				RETURNING id
				""",
				Long.class,
				event.eventName(),
				event.projection(),
				event.scope(),
				event.entityId(),
				event.version(),
				Timestamp.from(event.changedAt() == null ? Instant.now() : event.changedAt()),
				toJson(event));
		return event.withId(String.valueOf(id));
	}

	@Override
	public List<ProjectionSyncEvent> findAfter(long lastEventId, int limit) {
		return jdbcTemplate.query("""
				SELECT id,
				       event_name,
				       projection,
				       scope,
				       entity_id,
				       version,
				       changed_at,
				       payload_json
				FROM projection_sync_events
				WHERE id > ?
				ORDER BY id ASC
				LIMIT ?
				""",
				this::mapRow,
				lastEventId,
				limit);
	}

	@Override
	public long currentOffset() {
		Long offset = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(id), 0) FROM projection_sync_events", Long.class);
		return offset == null ? 0L : offset;
	}

	private ProjectionSyncEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new ProjectionSyncEvent(
				String.valueOf(rs.getLong("id")),
				rs.getString("event_name"),
				readSchemaVersion(rs.getString("payload_json")),
				rs.getString("projection"),
				rs.getString("scope"),
				rs.getString("entity_id"),
				rs.getObject("version", Long.class),
				rs.getTimestamp("changed_at").toInstant(),
				readHints(rs.getString("payload_json")),
				readReason(rs.getString("payload_json")));
	}

	private int readSchemaVersion(String payloadJson) {
		try {
			return objectMapper.readTree(payloadJson).path("schemaVersion").asInt(1);
		} catch (JsonProcessingException e) {
			return 1;
		}
	}

	private List<String> readHints(String payloadJson) {
		try {
			var hints = objectMapper.readTree(payloadJson).path("hints");
			if (!hints.isArray()) {
				return List.of();
			}
			return objectMapper.convertValue(
					hints,
					objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
		} catch (IllegalArgumentException | JsonProcessingException e) {
			return List.of();
		}
	}

	private String readReason(String payloadJson) {
		try {
			var reason = objectMapper.readTree(payloadJson).path("reason");
			return reason.isMissingNode() || reason.isNull() ? null : reason.asText();
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private String toJson(ProjectionSyncEvent event) {
		try {
			return objectMapper.writeValueAsString(event);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize projection sync event", e);
		}
	}
}
