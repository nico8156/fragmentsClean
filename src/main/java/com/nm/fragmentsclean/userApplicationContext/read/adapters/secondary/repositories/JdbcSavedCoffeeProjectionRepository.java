package com.nm.fragmentsclean.userApplicationContext.read.adapters.secondary.repositories;

import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
import com.nm.fragmentsclean.userApplicationContext.read.projections.SavedCoffeeItemView;
import com.nm.fragmentsclean.userApplicationContext.read.projections.SavedCoffeeListView;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffeeSetEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcSavedCoffeeProjectionRepository {
	private final JdbcTemplate jdbcTemplate;

	public JdbcSavedCoffeeProjectionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void apply(SavedCoffeeSetEvent event) {
		jdbcTemplate.update("""
				INSERT INTO user_saved_coffees_projection (
				  saved_coffee_id, user_id, coffee_id, active, version, updated_at
				)
				VALUES (?, ?, ?, ?, ?, ?)
				ON CONFLICT (saved_coffee_id) DO UPDATE SET
				  user_id = EXCLUDED.user_id,
				  coffee_id = EXCLUDED.coffee_id,
				  active = EXCLUDED.active,
				  version = EXCLUDED.version,
				  updated_at = EXCLUDED.updated_at
				WHERE user_saved_coffees_projection.version <= EXCLUDED.version
				""",
				event.savedCoffeeId(),
				event.userId(),
				event.coffeeId(),
				event.active(),
				event.version(),
				ts(event.occurredAt()));
	}

	public void upsertCoffee(CoffeeCreatedIntegrationEvent event) {
		jdbcTemplate.update("""
				INSERT INTO user_saved_coffee_cafes_projection (
				  coffee_id, name, address_line1, city, postal_code, country, archived, version, updated_at
				)
				VALUES (?, ?, ?, ?, ?, ?, false, ?, ?)
				ON CONFLICT (coffee_id) DO UPDATE SET
				  name = EXCLUDED.name,
				  address_line1 = EXCLUDED.address_line1,
				  city = EXCLUDED.city,
				  postal_code = EXCLUDED.postal_code,
				  country = EXCLUDED.country,
				  archived = false,
				  version = EXCLUDED.version,
				  updated_at = EXCLUDED.updated_at
				WHERE user_saved_coffee_cafes_projection.version <= EXCLUDED.version
				""",
				event.coffeeId(),
				event.name(),
				event.addressLine1(),
				event.city(),
				event.postalCode(),
				event.country(),
				(long) event.version(),
				ts(event.occurredAt()));
	}

	public void markCoffeeArchived(UUID coffeeId, long version, Instant occurredAt) {
		jdbcTemplate.update("""
				INSERT INTO user_saved_coffee_cafes_projection (
				  coffee_id, name, archived, version, updated_at
				)
				VALUES (?, 'Café indisponible', true, ?, ?)
				ON CONFLICT (coffee_id) DO UPDATE SET
				  archived = true,
				  version = EXCLUDED.version,
				  updated_at = EXCLUDED.updated_at
				WHERE user_saved_coffee_cafes_projection.version <= EXCLUDED.version
				""",
				coffeeId,
				version,
				ts(occurredAt));
	}

	public List<UUID> activeUserIdsForCoffee(UUID coffeeId) {
		return jdbcTemplate.query("""
				SELECT DISTINCT user_id
				FROM user_saved_coffees_projection
				WHERE coffee_id = ? AND active = true
				""",
				(rs, rowNum) -> rs.getObject("user_id", UUID.class),
				coffeeId);
	}

	public SavedCoffeeListView findForUser(UUID userId, Instant serverTime) {
		List<SavedCoffeeItemView> items = jdbcTemplate.query("""
				SELECT s.coffee_id,
				       COALESCE(c.name, 'Café') AS name,
				       c.address_line1,
				       c.city,
				       c.postal_code,
				       c.country,
				       s.updated_at AS saved_at,
				       GREATEST(s.version, COALESCE(c.version, 0)) AS version
				FROM user_saved_coffees_projection s
				LEFT JOIN user_saved_coffee_cafes_projection c ON c.coffee_id = s.coffee_id
				WHERE s.user_id = ?
				  AND s.active = true
				  AND COALESCE(c.archived, false) = false
				ORDER BY s.updated_at DESC
				""",
				this::mapItem,
				userId);
		return new SavedCoffeeListView(items, maxVersion(userId), serverTime.toString());
	}

	private SavedCoffeeItemView mapItem(ResultSet rs, int rowNum) throws SQLException {
		return new SavedCoffeeItemView(
				rs.getObject("coffee_id", UUID.class),
				rs.getString("name"),
				rs.getString("address_line1"),
				rs.getString("city"),
				rs.getString("postal_code"),
				rs.getString("country"),
				rs.getTimestamp("saved_at").toInstant().toString(),
				rs.getLong("version"));
	}

	private long maxVersion(UUID userId) {
		Long version = jdbcTemplate.queryForObject("""
				SELECT COALESCE(MAX(version), 0)
				FROM user_saved_coffees_projection
				WHERE user_id = ?
				""",
				Long.class,
				userId);
		return version == null ? 0L : version;
	}

	private Timestamp ts(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}
}
