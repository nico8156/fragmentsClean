package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
    public void apply(CoffeeCreatedEvent event) {

        Timestamp updatedAt = Timestamp.from(event.occurredAt());

        jdbcTemplate.update(
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
                    rating,
                    version,
                    updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET
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
                    rating = EXCLUDED.rating,
                    version = EXCLUDED.version,
                    updated_at = EXCLUDED.updated_at
                """,
                event.coffeeId().value(),
                event.googlePlaceId() != null ? event.googlePlaceId().value() : null,
                event.name().value(),
                event.address().line1(),
                event.address().city(),
                event.address().postalCode(),
                event.address().country(),
                event.location().lat(),
                event.location().lon(),
                event.phoneNumber() != null ? event.phoneNumber().value() : null,
                event.website() != null ? event.website().value() : null,
                // tags → JSON
                toTagsJson(event),
                null, // rating pour plus tard
                event.version(),
                updatedAt
        );
    }

    @Override
    public List<CoffeeSummaryView> findAll() {
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
                   version,
                   updated_at
            FROM coffee_summaries_projection
            ORDER BY name ASC
            """;

        return jdbcTemplate.query(sql, this::mapRow);
    }

    private String toTagsJson(CoffeeCreatedEvent event) {
        if (event.tags() == null || event.tags().isEmpty()) {
            return "[]";
        }
        // JSON très simple : ["espresso","chain"]
        String joined = event.tags().stream()
                .map(t -> "\"" + t.value().replace("\"", "\\\"") + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return "[" + joined + "]";
    }

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
                version,
                updatedAt
        );
    }

    /**
     * Parsing ultra-simple du JSON ["tag1","tag2"] -> Set<String>
     * (suffisant vu toTagsJson plus haut)
     */
    private Set<String> parseTagsJson(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank() || tagsJson.equals("[]")) {
            return Set.of();
        }
        // En entrée on a un truc du genre ["filter","specialty"]
        String trimmed = tagsJson.trim();
        if (trimmed.startsWith("[")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("]")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        if (trimmed.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .map(s -> s.replace("\"", "")) // enlève les guillemets
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
