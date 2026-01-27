package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcCoffeeOpeningHoursProjectionRepository implements CoffeeOpeningHoursProjectionRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcCoffeeOpeningHoursProjectionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public long count() {
		Long v = jdbcTemplate.queryForObject("SELECT count(*) FROM coffee_openinghours_projection", Long.class);
		return v == null ? 0 : v;
	}

	@Override
	public List<CoffeeOpeningHoursView> findAll() {
		String sql = """
				SELECT id, coffee_id, weekday_description
				FROM coffee_openinghours_projection
				ORDER BY coffee_id
				""";
		return jdbcTemplate.query(sql, this::mapRow);
	}

	@Override
	public void insertSeed(CoffeeOpeningHoursView view) {
		jdbcTemplate.update("""
				  INSERT INTO coffee_openinghours_projection (id, coffee_id, weekday_description)
				  VALUES (?, ?, ?)
				  ON CONFLICT (id) DO NOTHING
				""", view.id(), view.coffeeId(), view.weekdayDescription());
	}

	private CoffeeOpeningHoursView mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		UUID coffeeId = rs.getObject("coffee_id", UUID.class);
		String desc = rs.getString("weekday_description");
		return new CoffeeOpeningHoursView(id, coffeeId, desc);
	}
}
