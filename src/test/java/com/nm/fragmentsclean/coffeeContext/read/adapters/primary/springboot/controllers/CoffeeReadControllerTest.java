package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.nm.fragmentsclean.coffeeContext.read.GetCoffeeQuery;
import com.nm.fragmentsclean.coffeeContext.read.GetCoffeeQueryHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;

class CoffeeReadControllerTest {

	private static final UUID COFFEE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	@Test
	void returns_the_published_projection_detail() throws Exception {
		QueryBus queryBus = queryBusReturning(Optional.of(view()));
		var mockMvc = MockMvcBuilders.standaloneSetup(new CoffeeReadController(queryBus)).build();

		mockMvc.perform(get("/api/coffees/{coffeeId}", COFFEE_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(COFFEE_ID.toString()))
				.andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"));
	}

	@Test
	void returns_not_found_when_no_public_projection_exists() throws Exception {
		QueryBus queryBus = queryBusReturning(Optional.empty());
		var mockMvc = MockMvcBuilders.standaloneSetup(new CoffeeReadController(queryBus)).build();

		mockMvc.perform(get("/api/coffees/{coffeeId}", COFFEE_ID))
				.andExpect(status().isNotFound());
	}

	private QueryBus queryBusReturning(Optional<CoffeeSummaryView> result) {
		QueryBus queryBus = new QueryBus();
		queryBus.registerQueryHandlers(List.of(new GetCoffeeQueryHandler(new StubRepository(result))));
		return queryBus;
	}

	private CoffeeSummaryView view() {
		return new CoffeeSummaryView(
				COFFEE_ID,
				"google-place",
				"Fragments Coffee",
				48.11,
				-1.67,
				"1 rue Test",
				"Rennes",
				"35000",
				"FR",
				null,
				null,
				Set.of("specialty"),
				"PUBLISHED",
				2,
				Instant.parse("2026-08-29T10:00:00Z"));
	}

	private static class StubRepository implements CoffeeProjectionRepository {
		private final Optional<CoffeeSummaryView> result;

		private StubRepository(Optional<CoffeeSummaryView> result) {
			this.result = result;
		}

		@Override public Optional<CoffeeSummaryView> findById(UUID coffeeId, boolean publishedOnly) { return result; }
		@Override public void apply(CoffeeCreatedEvent event) { }
		@Override public void deleteByCoffeeId(UUID coffeeId) { }
		@Override public List<CoffeeSummaryView> findAll() { return List.of(); }
		@Override public void insertSeed(CoffeeSummaryView view) { }
		@Override public long count() { return 0; }
	}
}
