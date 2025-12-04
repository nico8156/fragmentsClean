package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcCoffeeProjectionRepository implements CoffeeProjectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcCoffeeProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void apply(CoffeeCreatedEvent event) {
        // on peut faire un simple INSERT ... ON CONFLICT UPDATE si besoin
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
                event.occurredAt()
        );
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
}
