package com.nm.fragmentsclean.coffeeContextTest.integration.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.TestContainers;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.bootstrap.CoffeeReadSeedRunner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;

@SpringBootTest
@ActiveProfiles("database")
class CoffeeReadSeedRunnerIT extends TestContainers {

	@Autowired
	CoffeeReadSeedRunner runner;

	@Autowired
	CoffeeProjectionRepository coffeeRepo;
	@Autowired
	CoffeePhotoProjectionRepository photoRepo;
	@Autowired
	CoffeeOpeningHoursProjectionRepository hoursRepo;

	@Autowired
	JdbcTemplate jdbcTemplate;
	@Autowired
	ObjectMapper objectMapper;

	@BeforeEach
	void resetDb() {
		// si FK existent, passe en CASCADE:
		// jdbcTemplate.execute("TRUNCATE TABLE coffee_photos_projection CASCADE");
		// sinon garde simple.
		jdbcTemplate.execute("TRUNCATE TABLE coffee_photos_projection");
		jdbcTemplate.execute("TRUNCATE TABLE coffee_openinghours_projection");
		jdbcTemplate.execute("TRUNCATE TABLE coffee_summaries_projection");
	}

	@Test
	void seed_populates_exactly_seed_file_sizes() throws Exception {
		// arrange
		int expectedCoffees = seedArraySize("seed/coffees.old.json");
		int expectedPhotos = seedArraySize("seed/coffee_photos.seed.json");
		int expectedHours = seedArraySize("seed/coffee_opening_hours.seed.json");

		// act
		runner.run();

		// assert
		assertThat(coffeeRepo.count()).isEqualTo(expectedCoffees);
		assertThat(photoRepo.count()).isEqualTo(expectedPhotos);
		assertThat(hoursRepo.count()).isEqualTo(expectedHours);

		// sanity: un café connu seedé
		assertThat(coffeeRepo.findAll())
				.anyMatch(v -> "Magma Coffee Club".equals(v.name()));
	}

	@Test
	void seed_is_idempotent() throws Exception {
		runner.run();
		long c1 = coffeeRepo.count();
		long p1 = photoRepo.count();
		long h1 = hoursRepo.count();

		runner.run();
		long c2 = coffeeRepo.count();
		long p2 = photoRepo.count();
		long h2 = hoursRepo.count();

		assertThat(c2).isEqualTo(c1);
		assertThat(p2).isEqualTo(p1);
		assertThat(h2).isEqualTo(h1);
	}

	private int seedArraySize(String classpathLocation) throws Exception {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		try (InputStream in = resource.getInputStream()) {
			JsonNode root = objectMapper.readTree(in);
			// on attend un tableau JSON
			assertThat(root.isArray())
					.as("Seed file must be a JSON array: " + classpathLocation)
					.isTrue();
			return root.size();
		}
	}
}
