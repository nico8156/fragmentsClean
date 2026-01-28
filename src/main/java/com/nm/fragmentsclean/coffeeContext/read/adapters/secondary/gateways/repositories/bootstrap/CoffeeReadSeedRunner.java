package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.bootstrap;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;

@Component
public class CoffeeReadSeedRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(CoffeeReadSeedRunner.class);

	private final CoffeeProjectionRepository coffeeRepo;
	private final CoffeePhotoProjectionRepository photoRepo;
	private final CoffeeOpeningHoursProjectionRepository hoursRepo;
	private final ObjectMapper objectMapper;

	public CoffeeReadSeedRunner(
			CoffeeProjectionRepository coffeeRepo,
			CoffeePhotoProjectionRepository photoRepo,
			CoffeeOpeningHoursProjectionRepository hoursRepo,
			ObjectMapper objectMapper) {
		this.coffeeRepo = coffeeRepo;
		this.photoRepo = photoRepo;
		this.hoursRepo = hoursRepo;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		seedCoffeeSummariesIfEmpty();
		seedCoffeePhotosIfEmpty();
		seedCoffeeOpeningHoursIfEmpty();
	}

	private void seedCoffeeSummariesIfEmpty() throws Exception {
		long existing = coffeeRepo.count();
		if (existing > 0) {
			log.info("[SEED][coffee_summaries_projection] skip (count={})", existing);
			return;
		}

		List<OldCoffeeSeedRow> rows = readJsonList(
				"seed/coffees.old.json",
				new TypeReference<List<OldCoffeeSeedRow>>() {
				});

		if (rows.isEmpty()) {
			log.warn("[SEED][coffee_summaries_projection] no seed rows (file missing or empty)");
			return;
		}

		log.info("[SEED][coffee_summaries_projection] start (items={})", rows.size());

		Instant now = Instant.now();

		for (OldCoffeeSeedRow r : rows) {
			CoffeeAddressParts ap = AddressParser.parse(r.formattedAddress());

			CoffeeSummaryView view = new CoffeeSummaryView(
					r.id(),
					normalizeOptional(r.googleId()),
					normalizeRequired(r.displayName(), "display_name"),
					r.latitude(),
					r.longitude(),
					ap.addressLine(),
					ap.city(),
					ap.postalCode(),
					ap.country(),
					normalizeDefaultPhone(r.nationalPhoneNumber()),
					normalizeDefaultWebsite(r.websiteUri()),
					Set.of(), // tags seed: vide
					0L, // version initiale
					now);

			coffeeRepo.insertSeed(view);
		}

		log.info("[SEED][coffee_summaries_projection] done");
	}

	private void seedCoffeePhotosIfEmpty() throws Exception {
		long existing = photoRepo.count();
		if (existing > 0) {
			log.info("[SEED][coffee_photos_projection] skip (count={})", existing);
			return;
		}

		List<CoffeePhotoSeedRow> rows = readJsonList(
				"seed/coffee_photos.seed.json",
				new TypeReference<List<CoffeePhotoSeedRow>>() {
				});

		if (rows.isEmpty()) {
			log.warn("[SEED][coffee_photos_projection] no seed rows (file missing or empty)");
			return;
		}

		log.info("[SEED][coffee_photos_projection] start (items={})", rows.size());

		for (CoffeePhotoSeedRow r : rows) {
			CoffeePhotoView view = new CoffeePhotoView(
					r.id(),
					r.coffeeId(),
					normalizeRequired(r.photoUri(), "photo_uri"));
			photoRepo.insertSeed(view);
		}

		log.info("[SEED][coffee_photos_projection] done");
	}

	private void seedCoffeeOpeningHoursIfEmpty() throws Exception {
		long existing = hoursRepo.count();
		if (existing > 0) {
			log.info("[SEED][coffee_opening_hours_projection] skip (count={})", existing);
			return;
		}

		List<CoffeeOpeningHoursSeedRow> rows = readJsonList(
				"seed/coffee_opening_hours.seed.json",
				new TypeReference<List<CoffeeOpeningHoursSeedRow>>() {
				});

		if (rows.isEmpty()) {
			log.warn("[SEED][coffee_opening_hours_projection] no seed rows (file missing or empty)");
			return;
		}

		log.info("[SEED][coffee_opening_hours_projection] start (items={})", rows.size());

		for (CoffeeOpeningHoursSeedRow r : rows) {
			CoffeeOpeningHoursView view = new CoffeeOpeningHoursView(
					r.id(),
					r.coffeeId(),
					normalizeRequired(r.weekdayDescription(), "weekday_description"));
			hoursRepo.insertSeed(view);
		}

		log.info("[SEED][coffee_opening_hours_projection] done");
	}

	private <T> List<T> readJsonList(String classpathLocation, TypeReference<List<T>> ref) throws Exception {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		if (!resource.exists())
			return List.of();

		try (InputStream in = resource.getInputStream()) {
			return objectMapper.readValue(in, ref);
		}
	}

	// ----------------------
	// Normalizers
	// ----------------------

	private static String normalizeRequired(String s, String fieldName) {
		if (s == null)
			throw new IllegalArgumentException("Missing required field: " + fieldName);
		String t = s.trim();
		if (t.isEmpty())
			throw new IllegalArgumentException("Empty required field: " + fieldName);
		return t;
	}

	private static String normalizeOptional(String s) {
		if (s == null)
			return null;
		String t = s.trim();
		return t.isEmpty() ? null : t;
	}

	private static String normalizeDefaultPhone(String s) {
		String t = normalizeOptional(s);
		if (t == null)
			return null;
		if ("default phone number".equalsIgnoreCase(t))
			return null;
		return t;
	}

	private static String normalizeDefaultWebsite(String s) {
		String t = normalizeOptional(s);
		if (t == null)
			return null;
		if ("default website".equalsIgnoreCase(t))
			return null;
		return t;
	}
}
