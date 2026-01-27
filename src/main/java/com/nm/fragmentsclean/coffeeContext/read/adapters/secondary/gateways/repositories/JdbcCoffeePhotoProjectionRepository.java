package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcCoffeePhotoProjectionRepository implements CoffeePhotoProjectionRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcCoffeePhotoProjectionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public long count() {
		Long v = jdbcTemplate.queryForObject("SELECT count(*) FROM coffee_photos_projection", Long.class);
		return v == null ? 0 : v;
	}

	@Override
	public List<CoffeePhotoView> findAll() {
		String sql = """
				SELECT id, coffee_id, photo_uri
				FROM coffee_photos_projection
				ORDER BY coffee_id
				""";
		return jdbcTemplate.query(sql, this::mapRow);
	}

	@Override
	public void insertSeed(CoffeePhotoView view) {
		jdbcTemplate.update("""
				  INSERT INTO coffee_photos_projection (id, coffee_id, photo_uri)
				  VALUES (?, ?, ?)
				  ON CONFLICT (id) DO NOTHING
				""", view.id(), view.coffeeId(), view.photoUri());
	}

	private CoffeePhotoView mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		UUID coffeeId = rs.getObject("coffee_id", UUID.class);
		String uri = rs.getString("photo_uri");
		return new CoffeePhotoView(id, coffeeId, uri);
	}
}
