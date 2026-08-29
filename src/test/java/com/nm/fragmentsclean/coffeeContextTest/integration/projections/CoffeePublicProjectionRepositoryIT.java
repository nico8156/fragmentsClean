package com.nm.fragmentsclean.coffeeContextTest.integration.projections;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.nm.fragmentsclean.TestContainers;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.JdbcCoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.JdbcCoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.JdbcCoffeeProjectionRepository;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.sql.init.mode=always")
@Import({JdbcCoffeeProjectionRepository.class, JdbcCoffeePhotoProjectionRepository.class,
		JdbcCoffeeOpeningHoursProjectionRepository.class})
class CoffeePublicProjectionRepositoryIT extends TestContainers {

	private static final UUID PUBLISHED_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID DRAFT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	@Autowired JdbcTemplate jdbc;
	@Autowired CoffeeProjectionRepository summaries;
	@Autowired CoffeePhotoProjectionRepository photos;
	@Autowired CoffeeOpeningHoursProjectionRepository hours;

	@BeforeEach
	void setUp() {
		jdbc.update("DELETE FROM coffee_photos_projection");
		jdbc.update("DELETE FROM coffee_openinghours_projection");
		jdbc.update("DELETE FROM coffee_summaries_projection");
		insertSummary(PUBLISHED_ID, "Published Coffee", "PUBLISHED");
		insertSummary(DRAFT_ID, "Draft Coffee", "DRAFT");
		jdbc.update("INSERT INTO coffee_photos_projection (id, coffee_id, photo_uri) VALUES (?, ?, ?)",
				UUID.randomUUID(), PUBLISHED_ID, "published.webp");
		jdbc.update("INSERT INTO coffee_photos_projection (id, coffee_id, photo_uri) VALUES (?, ?, ?)",
				UUID.randomUUID(), DRAFT_ID, "draft.webp");
		jdbc.update("INSERT INTO coffee_openinghours_projection (id, coffee_id, weekday_description) VALUES (?, ?, ?)",
				UUID.randomUUID(), PUBLISHED_ID, "Monday: 09:00-18:00");
		jdbc.update("INSERT INTO coffee_openinghours_projection (id, coffee_id, weekday_description) VALUES (?, ?, ?)",
				UUID.randomUUID(), DRAFT_ID, "Tuesday: 09:00-18:00");
	}

	@Test
	void public_summary_detail_hides_drafts() {
		assertThat(summaries.findById(PUBLISHED_ID, true)).isPresent();
		assertThat(summaries.findById(DRAFT_ID, true)).isEmpty();
		assertThat(summaries.findById(DRAFT_ID, false)).isPresent();
	}

	@Test
	void public_children_belong_only_to_published_coffees() {
		assertThat(photos.findAll(true)).extracting(view -> view.coffeeId()).containsExactly(PUBLISHED_ID);
		assertThat(hours.findAll(true)).extracting(view -> view.coffeeId()).containsExactly(PUBLISHED_ID);
		assertThat(photos.findAll(false)).hasSize(2);
		assertThat(hours.findAll(false)).hasSize(2);
	}

	private void insertSummary(UUID coffeeId, String name, String status) {
		jdbc.update("""
				INSERT INTO coffee_summaries_projection (
				    id, google_place_id, name, address_line1, city, postal_code, country,
				    lat, lon, phone_number, website, tags_json, publication_status, version, updated_at
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '[]'::jsonb, ?, ?, NOW())
				""", coffeeId, "google-" + coffeeId, name, "1 rue Test", "Rennes", "35000", "FR",
				48.11, -1.67, null, null, status, 1);
	}
}
