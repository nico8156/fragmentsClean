package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;

@Repository
public class JdbcCoffeeProjectionSource implements CoffeeProjectionSource {
	private final JdbcTemplate jdbcTemplate;

	public JdbcCoffeeProjectionSource(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public Optional<CoffeeSummaryView> findByCoffeeId(UUID coffeeId) {
		return jdbcTemplate.query("""
				SELECT id, google_place_id, name, lat, lon, address_line1, city,
				       postal_code, country, phone_number, website, tags_csv, publication_status, version, updated_at
				FROM coffees
				WHERE id = ? AND archived_at IS NULL
				""", this::mapRow, coffeeId).stream().findFirst();
	}

	private CoffeeSummaryView mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new CoffeeSummaryView(
				rs.getObject("id", UUID.class), rs.getString("google_place_id"), rs.getString("name"),
				rs.getDouble("lat"), rs.getDouble("lon"), rs.getString("address_line1"),
				rs.getString("city"), rs.getString("postal_code"), rs.getString("country"),
				rs.getString("phone_number"), rs.getString("website"), parseTags(rs.getString("tags_csv")),
				rs.getString("publication_status"),
				rs.getLong("version"), rs.getTimestamp("updated_at").toInstant());
	}

	private LinkedHashSet<String> parseTags(String tagsCsv) {
		if (tagsCsv == null || tagsCsv.isBlank()) return new LinkedHashSet<>();
		return Arrays.stream(tagsCsv.split(",")).map(String::trim).filter(tag -> !tag.isBlank())
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}
}
