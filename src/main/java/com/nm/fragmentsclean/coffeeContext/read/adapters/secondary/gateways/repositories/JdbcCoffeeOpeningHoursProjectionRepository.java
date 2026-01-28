package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcCoffeeOpeningHoursProjectionRepository implements CoffeeOpeningHoursProjectionRepository {

	private final JdbcTemplate jdbc;

	public JdbcCoffeeOpeningHoursProjectionRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public long count() {
		Long n = jdbc.queryForObject("SELECT COUNT(*) FROM coffee_openinghours_projection", Long.class);
		return n == null ? 0L : n;
	}

	@Override
	public void insertSeed(CoffeeOpeningHoursView view) {
		jdbc.update("""
				    INSERT INTO coffee_openinghours_projection (id, coffee_id, weekday_description)
				    VALUES (?, ?, ?)
				    ON CONFLICT (id) DO UPDATE SET
				      coffee_id = EXCLUDED.coffee_id,
				      weekday_description = EXCLUDED.weekday_description
				""", view.id(), view.coffeeId(), view.weekdayDescription());
	}

	@Override
	public List<CoffeeOpeningHoursView> findAll() {
		return jdbc.query("""
				    SELECT id, coffee_id, weekday_description
				    FROM coffee_openinghours_projection
				    ORDER BY coffee_id ASC
				""", this::mapRow);
	}

	private CoffeeOpeningHoursView mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		UUID coffeeId = rs.getObject("coffee_id", UUID.class);
		String desc = rs.getString("weekday_description");
		return new CoffeeOpeningHoursView(id, coffeeId, desc);
	}
}
