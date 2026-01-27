package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.bootstrap;

import java.io.InputStream;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;

@Component
public class CoffeeReadSeedRunner implements ApplicationRunner {

	private final CoffeeProjectionRepository coffeeRepo;
	private final CoffeePhotoProjectionRepository photoRepo;
	private final CoffeeOpeningHoursProjectionRepository hoursRepo;
	private final ObjectMapper om;

	public CoffeeReadSeedRunner(
			CoffeeProjectionRepository coffeeRepo,
			CoffeePhotoProjectionRepository photoRepo,
			CoffeeOpeningHoursProjectionRepository hoursRepo,
			ObjectMapper om) {
		this.coffeeRepo = coffeeRepo;
		this.photoRepo = photoRepo;
		this.hoursRepo = hoursRepo;
		this.om = om;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		if (coffeeRepo.count() > 0)
			return;

		// charge tes 3 JSON depuis resources/seed/
		var coffees = read("/seed/coffees.json", CoffeeSeed[].class);
		var photos = read("/seed/photos.json", PhotoSeed[].class);
		var hours = read("/seed/opening_hours.json", HoursSeed[].class);

		for (var c : coffees) {
			var view = new CoffeeSummaryView(
					UUID.fromString(c.id()),
					c.google_id(),
					c.display_name(),
					c.latitude(),
					c.longitude(),
					c.address_line1(),
					c.city(),
					c.postal_code(),
					c.country(),
					c.national_phone_number(),
					c.website_uri(),
					Set.of(),
					1L,
					Instant.now());
			coffeeRepo.insertSeed(view, "[]");
		}

		for (var p : photos) {
			photoRepo.insertSeed(UUID.fromString(p.id()), UUID.fromString(p.coffee_id()), p.photo_uri());
		}

		for (var h : hours) {
			hoursRepo.insertSeed(UUID.fromString(h.id()), UUID.fromString(h.coffee_id()),
					h.weekday_description());
		}
	}

	private <T> T read(String path, Class<T> type) throws Exception {
		try (InputStream in = getClass().getResourceAsStream(path)) {
			if (in == null)
				throw new IllegalStateException("Missing seed file " + path);
			return om.readValue(in, type);
		}
	}

	// records seed (match ton JSON legacy)
	public record CoffeeSeed(
			String id,
			String google_id,
			String display_name,
			String formatted_address,
			String national_phone_number,
			String website_uri,
			double latitude,
			double longitude,
			String address_line1,
			String city,
			String postal_code,
			String country) {
	}

	public record PhotoSeed(String id, String coffee_id, String photo_uri) {
	}

	public record HoursSeed(String id, String coffee_id, String weekday_description) {
	}
}
