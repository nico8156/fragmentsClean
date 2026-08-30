package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.nm.fragmentsclean.coffeeContext.read.CoffeeCataloguePage;
import com.nm.fragmentsclean.coffeeContext.read.SearchPublicCoffeesQueryHandler;
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

	@Test
	void returns_a_searchable_cursor_page_with_an_etag() throws Exception {
		var repository = new StubRepository(Optional.empty());
		repository.page = new CoffeeCataloguePage(List.of(view()), "next-cursor", "\"coffee-catalogue-test\"");
		QueryBus queryBus = new QueryBus();
		queryBus.registerQueryHandlers(List.of(new SearchPublicCoffeesQueryHandler(repository)));
		var mockMvc = MockMvcBuilders.standaloneSetup(new CoffeeReadController(queryBus)).build();

		mockMvc.perform(get("/api/coffees").param("query", "rennes").param("limit", "20"))
				.andExpect(status().isOk())
				.andExpect(header().string("ETag", "\"coffee-catalogue-test\""))
				.andExpect(header().string("X-Next-Cursor", "next-cursor"))
				.andExpect(jsonPath("$[0].id").value(COFFEE_ID.toString()));
	}

	@Test
	void returns_not_modified_when_the_catalogue_etag_matches() throws Exception {
		var repository = new StubRepository(Optional.empty());
		repository.page = new CoffeeCataloguePage(List.of(view()), null, "\"coffee-catalogue-test\"");
		QueryBus queryBus = new QueryBus();
		queryBus.registerQueryHandlers(List.of(new SearchPublicCoffeesQueryHandler(repository)));
		var mockMvc = MockMvcBuilders.standaloneSetup(new CoffeeReadController(queryBus)).build();

		mockMvc.perform(get("/api/coffees").header("If-None-Match", "\"coffee-catalogue-test\""))
				.andExpect(status().isNotModified())
				.andExpect(header().string("ETag", "\"coffee-catalogue-test\""));
	}

	@Test
	void rejects_an_invalid_catalogue_limit() throws Exception {
		QueryBus queryBus = new QueryBus();
		var mockMvc = MockMvcBuilders.standaloneSetup(new CoffeeReadController(queryBus))
				.setControllerAdvice(new CoffeeReadExceptionHandler())
				.build();

		mockMvc.perform(get("/api/coffees").param("limit", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("limit must be between 1 and 100"));
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
		private CoffeeCataloguePage page;

		private StubRepository(Optional<CoffeeSummaryView> result) {
			this.result = result;
		}

		@Override public Optional<CoffeeSummaryView> findById(UUID coffeeId, boolean publishedOnly) { return result; }
		@Override public CoffeeCataloguePage searchPublished(String search, String cursor, int limit) { return page; }
		@Override public void apply(CoffeeCreatedEvent event) { }
		@Override public void deleteByCoffeeId(UUID coffeeId) { }
		@Override public List<CoffeeSummaryView> findAll() { return List.of(); }
		@Override public void insertSeed(CoffeeSummaryView view) { }
		@Override public long count() { return 0; }
	}
}
