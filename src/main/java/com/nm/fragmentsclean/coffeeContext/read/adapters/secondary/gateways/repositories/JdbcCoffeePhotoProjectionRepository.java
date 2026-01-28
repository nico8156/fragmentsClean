package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcCoffeePhotoProjectionRepository implements CoffeePhotoProjectionRepository {

	private final JdbcTemplate jdbc;

	public JdbcCoffeePhotoProjectionRepository(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public long count() {
		Long n = jdbc.queryForObject("SELECT COUNT(*) FROM coffee_photos_projection", Long.class);
		return n == null ? 0L : n;
	}

	@Override
	public void insertSeed(CoffeePhotoView view) {
		jdbc.update("""
				    INSERT INTO coffee_photos_projection (id, coffee_id, photo_uri)
				    VALUES (?, ?, ?)
				    ON CONFLICT (id) DO UPDATE SET
				      coffee_id = EXCLUDED.coffee_id,
				      photo_uri = EXCLUDED.photo_uri
				""", view.id(), view.coffeeId(), view.photoUri());
	}

	@Override
	public List<CoffeePhotoView> findAll() {
		return jdbc.query("""
				    SELECT id, coffee_id, photo_uri
				    FROM coffee_photos_projection
				    ORDER BY coffee_id ASC
				""", this::mapRow);
	}

	private CoffeePhotoView mapRow(ResultSet rs, int rowNum) throws SQLException {
		UUID id = rs.getObject("id", UUID.class);
		UUID coffeeId = rs.getObject("coffee_id", UUID.class);
		String uri = rs.getString("photo_uri");
		return new CoffeePhotoView(id, coffeeId, uri);
	}
}
