package com.nm.fragmentsclean.adminImportContextTest.unit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.AdminImportPlacesController;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.AdminStudioArticlesController;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminSecurityProperties;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminTokenAuthenticationFilter;
import com.nm.fragmentsclean.coffeeContext.read.ListCoffeesQuery;
import com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.admin.AdminCoffeeManagementExceptionHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.admin.AdminCoffeesReadController;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ArchiveCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.AddCoffeePhotoCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.DeleteCoffeePhotoCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.CoffeeCreationResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeeImportStatus;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeePreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceSearchResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageAsset;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GooglePlacesGateway;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StoreStudioArticleImage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.PreviewGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchGooglePlacesForCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SubmitStudioArticle;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration.cors.FragmentsCorsConfiguration;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration.cors.FragmentsCorsProperties;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync.ProjectionSyncController;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync.ProjectionSyncDispatcher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminTokenSecurityTest {
	@Test
	void admin_route_without_token_returns_401() throws Exception {
		mockMvc("admin-secret").perform(get("/api/admin/import/places").param("query", "cafe"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void admin_route_with_invalid_token_returns_401() throws Exception {
		mockMvc("admin-secret").perform(get("/api/admin/import/places")
						.param("query", "cafe")
						.header(HttpHeaders.AUTHORIZATION, "Bearer wrong"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void admin_route_with_valid_token_reaches_controller() throws Exception {
		mockMvc("admin-secret").perform(get("/api/admin/import/places")
						.param("query", "cafe")
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-secret"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].googlePlaceId").value("ChIJ-google-place"));
	}

	@Test
	void admin_coffees_without_token_returns_401() throws Exception {
		mockMvc("admin-secret").perform(get("/api/admin/coffees"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void admin_sync_events_without_token_returns_401() throws Exception {
		mockMvc("admin-secret").perform(get("/api/admin/sync/events"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void admin_sync_events_with_valid_token_opens_stream() throws Exception {
		mockMvc("admin-secret").perform(get("/api/admin/sync/events")
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-secret"))
				.andExpect(status().isOk())
				.andExpect(request().asyncStarted());
	}

	@Test
	void admin_coffees_with_valid_token_delegates_to_existing_read_query() throws Exception {
		var queryHandler = new CountingListCoffeesQueryHandler();

		mockMvc("admin-secret", queryHandler).perform(get("/api/admin/coffees")
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-secret"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("11111111-1111-1111-1111-111111111111"))
				.andExpect(jsonPath("$[0].googleId").value("google-place-1"))
				.andExpect(jsonPath("$[0].name").value("Fragments Cafe"))
				.andExpect(jsonPath("$[0].photos[0].id").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
				.andExpect(jsonPath("$[0].photos[0].photoUri").value("https://signed.fragments.test/coffee-1.jpg"))
				.andExpect(jsonPath("$[0].openingHours[0].id").value("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
				.andExpect(jsonPath("$[0].openingHours[0].weekdayDescription").value("lundi: 08:00-18:00"))
				.andExpect(jsonPath("$[1].id").value("22222222-2222-2222-2222-222222222222"))
				.andExpect(jsonPath("$[1].photos").isEmpty())
				.andExpect(jsonPath("$[1].openingHours").isEmpty());

		org.assertj.core.api.Assertions.assertThat(queryHandler.calls.get()).isEqualTo(1);
	}

	@Test
	void admin_delete_coffee_without_token_returns_401() throws Exception {
		mockMvc("admin-secret").perform(delete("/api/admin/coffees/11111111-1111-1111-1111-111111111111"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void admin_delete_coffee_with_valid_token_dispatches_archive_command() throws Exception {
		var queryHandler = new CountingListCoffeesQueryHandler();
		var archiveHandler = new RecordingArchiveCoffeeCommandHandler();

		mockMvc("admin-secret", queryHandler, archiveHandler)
				.perform(delete("/api/admin/coffees/11111111-1111-1111-1111-111111111111")
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-secret"))
				.andExpect(status().isAccepted());

		org.assertj.core.api.Assertions.assertThat(archiveHandler.commands).hasSize(1);
		org.assertj.core.api.Assertions.assertThat(archiveHandler.commands.getFirst().coffeeId())
				.isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
	}

	@Test
	void admin_add_coffee_photo_with_valid_token_dispatches_add_photo_command() throws Exception {
		var handlers = new RecordingCoffeeCommandHandlers();

		mockMvc("admin-secret", new CountingListCoffeesQueryHandler(), handlers)
				.perform(multipart("/api/admin/coffees/11111111-1111-1111-1111-111111111111/photos")
						.file("photo", "jpeg-bytes".getBytes())
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-secret"))
				.andExpect(status().isAccepted());

		org.assertj.core.api.Assertions.assertThat(handlers.addPhoto.commands).hasSize(1);
		org.assertj.core.api.Assertions.assertThat(handlers.addPhoto.commands.getFirst().coffeeId())
				.isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
		org.assertj.core.api.Assertions.assertThat(handlers.addPhoto.commands.getFirst().bytes())
				.isEqualTo("jpeg-bytes".getBytes());
	}

	@Test
	void admin_add_coffee_photo_payload_too_large_returns_413() throws Exception {
		var handlers = new RecordingCoffeeCommandHandlers();
		handlers.addPhoto.exception = new MaxUploadSizeExceededException(1024);

		mockMvc("admin-secret", new CountingListCoffeesQueryHandler(), handlers)
				.perform(multipart("/api/admin/coffees/11111111-1111-1111-1111-111111111111/photos")
						.file("photo", "jpeg-bytes".getBytes())
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-secret"))
				.andExpect(status().isPayloadTooLarge());
	}

	@Test
	void admin_delete_coffee_photo_with_valid_token_dispatches_delete_photo_command() throws Exception {
		var handlers = new RecordingCoffeeCommandHandlers();

		mockMvc("admin-secret", new CountingListCoffeesQueryHandler(), handlers)
				.perform(delete("/api/admin/coffees/11111111-1111-1111-1111-111111111111/photos/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-secret"))
				.andExpect(status().isAccepted());

		org.assertj.core.api.Assertions.assertThat(handlers.deletePhoto.commands).hasSize(1);
		org.assertj.core.api.Assertions.assertThat(handlers.deletePhoto.commands.getFirst().coffeeId())
				.isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
		org.assertj.core.api.Assertions.assertThat(handlers.deletePhoto.commands.getFirst().photoId())
				.isEqualTo(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
	}

	@Test
	void admin_studio_article_submit_without_token_returns_401() throws Exception {
		mockMvc("admin-secret").perform(post("/api/admin/studio/articles")
						.contentType("application/json")
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void admin_studio_article_submit_with_valid_token_reaches_controller() throws Exception {
		mockMvc("admin-secret").perform(post("/api/admin/studio/articles")
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-secret")
						.contentType("application/json")
						.content("""
								{
								  "slug": "guide-cafe-rennes",
								  "locale": "fr-FR",
								  "authorId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
								  "authorName": "Fragments Studio",
								  "title": "Guide cafe Rennes",
								  "intro": "Intro",
								  "blocks": [{ "heading": "Debut", "paragraph": "Paragraphe" }],
								  "conclusion": "Conclusion",
								  "tags": ["coffee"],
								  "coffeeIds": []
								}
								"""))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.commandId").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
				.andExpect(jsonPath("$.articleId").value("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
	}

	@Test
	void admin_studio_article_image_with_valid_token_reaches_controller() throws Exception {
		mockMvc("admin-secret").perform(multipart("/api/admin/studio/articles/images")
						.file(new org.springframework.mock.web.MockMultipartFile(
								"articleId",
								"",
								"text/plain",
								"bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb".getBytes()))
						.file("image", "jpeg-bytes".getBytes())
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-secret"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.assetId").value("dddddddd-dddd-dddd-dddd-dddddddddddd"))
				.andExpect(jsonPath("$.url").value("/api/articles/image-assets/test.jpg"));
	}

	@Test
	void admin_route_with_missing_configured_token_stays_closed() throws Exception {
		mockMvc("").perform(get("/api/admin/import/places")
						.param("query", "cafe")
						.header(HttpHeaders.AUTHORIZATION, "Bearer anything"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void preflight_from_localhost_is_allowed() throws Exception {
		mockMvc("admin-secret").perform(options("/api/admin/import/places")
						.header(HttpHeaders.ORIGIN, "http://localhost:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type,Accept"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS"));
	}

	@Test
	void preflight_from_127_0_0_1_is_allowed() throws Exception {
		mockMvc("admin-secret").perform(options("/api/admin/import/places")
						.header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type,Accept"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS"));
	}

	@Test
	void preflight_for_admin_sse_allows_last_event_id_header() throws Exception {
		mockMvc("admin-secret").perform(options("/api/admin/sync/events")
						.header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Accept,Last-Event-ID"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
						"Authorization, Accept, Last-Event-ID"));
	}

	@Test
	void preflight_for_public_coffees_api_from_127_0_0_1_is_allowed() throws Exception {
		mockMvc("admin-secret").perform(options("/api/coffees")
						.header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Accept"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,PATCH,DELETE,OPTIONS"));
	}

	@Test
	void preflight_from_unconfigured_origin_is_not_allowed() throws Exception {
		mockMvc("admin-secret").perform(options("/api/admin/import/places")
						.header(HttpHeaders.ORIGIN, "https://evil.example")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	private MockMvc mockMvc(String token) {
		return mockMvc(token, new CountingListCoffeesQueryHandler());
	}

	private MockMvc mockMvc(String token, CountingListCoffeesQueryHandler queryHandler) {
		return mockMvc(token, queryHandler, new RecordingCoffeeCommandHandlers());
	}

	private MockMvc mockMvc(String token,
			CountingListCoffeesQueryHandler queryHandler,
			RecordingArchiveCoffeeCommandHandler archiveHandler) {
		return mockMvc(token, queryHandler, new RecordingCoffeeCommandHandlers(archiveHandler));
	}

	private MockMvc mockMvc(String token,
			CountingListCoffeesQueryHandler queryHandler,
			RecordingCoffeeCommandHandlers handlers) {
		var properties = new AdminSecurityProperties();
		properties.setToken(token);
		CorsConfigurationSource corsConfigurationSource = new FragmentsCorsConfiguration()
				.corsConfigurationSource(corsProperties());

		return MockMvcBuilders.standaloneSetup(
						controller(),
						adminStudioArticlesController(),
						adminCoffeesController(queryHandler, handlers),
						projectionSyncController())
				.addFilters(new CorsFilter(corsConfigurationSource), new AdminTokenAuthenticationFilter(properties))
				.setControllerAdvice(new AdminCoffeeManagementExceptionHandler())
				.build();
	}

	private FragmentsCorsProperties corsProperties() {
		var properties = new FragmentsCorsProperties();
		properties.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
		properties.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		properties.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "Last-Event-ID"));
		properties.setExposedHeaders(List.of("Location"));
		properties.setAllowCredentials(false);
		properties.setMaxAge(3600);
		return properties;
	}

	private AdminImportPlacesController controller() {
		var gateway = new FakeGooglePlacesGateway();
		var preview = new PreviewGooglePlaceCoffee(gateway);
		return new AdminImportPlacesController(
				new SearchGooglePlacesForCoffee(gateway),
				preview,
				new ImportGooglePlaceCoffee(
						preview,
						command -> new CoffeeCreationResult(
								command.coffeeId(),
								command.googlePlaceId(),
								GooglePlaceCoffeeImportStatus.IMPORTED
						),
						() -> UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
						() -> Instant.parse("2026-07-03T09:00:00Z")
				)
		);
	}

	private AdminCoffeesReadController adminCoffeesController(
			CountingListCoffeesQueryHandler queryHandler,
			RecordingCoffeeCommandHandlers handlers) {
		var queryBus = new QueryBus();
		queryBus.registerQueryHandlers(List.of(queryHandler));
		var commandBus = new CommandBus();
		commandBus.registerCommandHandlers(List.of(handlers.archiveCoffee, handlers.addPhoto, handlers.deletePhoto));
		return new AdminCoffeesReadController(
				commandBus,
				queryBus,
				new FakeCoffeePhotoProjectionRepository(),
				new FakeCoffeeOpeningHoursProjectionRepository(),
				storedPhotoUri -> storedPhotoUri.startsWith("s3://")
						? "https://signed.fragments.test/coffee-1.jpg"
						: storedPhotoUri
		);
	}

	private AdminStudioArticlesController adminStudioArticlesController() {
		return new AdminStudioArticlesController(
				new SubmitStudioArticle(
						command -> {
						},
						new SequenceUuidGenerator(
								UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
								UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")),
						() -> Instant.parse("2026-07-10T10:15:30Z"),
						new com.fasterxml.jackson.databind.ObjectMapper()),
				new StoreStudioArticleImage((articleId, fileName, contentType, bytes, alt) ->
						new StudioArticleImageAsset(
								UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"),
								"/api/articles/image-assets/test.jpg",
								"/api/articles/image-assets/test.jpg",
								null,
								null,
								alt)));
	}

	private ProjectionSyncController projectionSyncController() {
		return new ProjectionSyncController(new FakeProjectionSyncDispatcher());
	}

	private static class FakeGooglePlacesGateway implements GooglePlacesGateway {
		@Override
		public List<GooglePlaceSearchResult> searchCoffeePlaces(String query) {
			return List.of(new GooglePlaceSearchResult(
					"ChIJ-google-place",
					"Cafe",
					"Rennes",
					48.111,
					-1.679
			));
		}

		@Override
		public Optional<GooglePlaceCoffeePreview> findCoffeePreview(String googlePlaceId) {
			return Optional.empty();
		}
	}

	private static class SequenceUuidGenerator implements com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator {
		private final UUID first;
		private final UUID second;
		private int calls;

		SequenceUuidGenerator(UUID first, UUID second) {
			this.first = first;
			this.second = second;
		}

		@Override
		public UUID generate() {
			calls += 1;
			return calls == 1 ? first : second;
		}
	}

	private static class CountingListCoffeesQueryHandler
			implements QueryHandler<ListCoffeesQuery, List<CoffeeSummaryView>> {
		private final AtomicInteger calls = new AtomicInteger();

		@Override
		public List<CoffeeSummaryView> handle(ListCoffeesQuery query) {
			calls.incrementAndGet();
			return List.of(
					new CoffeeSummaryView(
							UUID.fromString("11111111-1111-1111-1111-111111111111"),
							"google-place-1",
							"Fragments Cafe",
							48.111,
							-1.679,
							"1 rue du Test",
							"Rennes",
							"35000",
							"FR",
							null,
							"https://fragments.example",
							java.util.Set.of("filter"),
							1,
							Instant.parse("2026-07-03T08:00:00Z")
					),
					new CoffeeSummaryView(
							UUID.fromString("22222222-2222-2222-2222-222222222222"),
							"google-place-2",
							"Coffee Without Details",
							48.112,
							-1.680,
							"2 rue du Test",
							"Rennes",
							"35000",
							"FR",
							null,
							null,
							java.util.Set.of(),
							1,
							Instant.parse("2026-07-03T08:05:00Z")
					)
			);
		}
	}

	private static class RecordingArchiveCoffeeCommandHandler implements CommandHandler<ArchiveCoffeeCommand> {
		private final List<ArchiveCoffeeCommand> commands = new java.util.ArrayList<>();

		@Override
		public void execute(ArchiveCoffeeCommand command) {
			commands.add(command);
		}
	}

	private static class RecordingAddCoffeePhotoCommandHandler implements CommandHandler<AddCoffeePhotoCommand> {
		private final List<AddCoffeePhotoCommand> commands = new java.util.ArrayList<>();
		private RuntimeException exception;

		@Override
		public void execute(AddCoffeePhotoCommand command) {
			if (exception != null) {
				throw exception;
			}
			commands.add(command);
		}
	}

	private static class RecordingDeleteCoffeePhotoCommandHandler implements CommandHandler<DeleteCoffeePhotoCommand> {
		private final List<DeleteCoffeePhotoCommand> commands = new java.util.ArrayList<>();

		@Override
		public void execute(DeleteCoffeePhotoCommand command) {
			commands.add(command);
		}
	}

	private static class RecordingCoffeeCommandHandlers {
		private final RecordingArchiveCoffeeCommandHandler archiveCoffee;
		private final RecordingAddCoffeePhotoCommandHandler addPhoto = new RecordingAddCoffeePhotoCommandHandler();
		private final RecordingDeleteCoffeePhotoCommandHandler deletePhoto = new RecordingDeleteCoffeePhotoCommandHandler();

		private RecordingCoffeeCommandHandlers() {
			this(new RecordingArchiveCoffeeCommandHandler());
		}

		private RecordingCoffeeCommandHandlers(RecordingArchiveCoffeeCommandHandler archiveCoffee) {
			this.archiveCoffee = archiveCoffee;
		}
	}

	private static class FakeProjectionSyncDispatcher extends ProjectionSyncDispatcher {
		FakeProjectionSyncDispatcher() {
			super(null, null, null, null);
		}

		@Override
		public SseEmitter openStream(String lastEventId) {
			return new SseEmitter(1_000L);
		}
	}

	private static class FakeCoffeePhotoProjectionRepository implements CoffeePhotoProjectionRepository {
		@Override
		public void insertSeed(CoffeePhotoView view) {
		}

		@Override
		public void replaceForCoffee(UUID coffeeId, List<CoffeePhotoView> photos) {
		}

		@Override
		public void append(CoffeePhotoView photo) {
		}

		@Override
		public void deletePhoto(UUID coffeeId, UUID photoId) {
		}

		@Override
		public void deleteForCoffee(UUID coffeeId) {
		}

		@Override
		public List<CoffeePhotoView> findAll() {
			return List.of(new CoffeePhotoView(
					UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
					UUID.fromString("11111111-1111-1111-1111-111111111111"),
					"s3://anchor-assets-prod-851725375299/fragments/staging/coffees/11111111-1111-1111-1111-111111111111/photos/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa.jpg"
			));
		}

		@Override
		public long count() {
			return 1;
		}
	}

	private static class FakeCoffeeOpeningHoursProjectionRepository implements CoffeeOpeningHoursProjectionRepository {
		@Override
		public void insertSeed(CoffeeOpeningHoursView view) {
		}

		@Override
		public List<CoffeeOpeningHoursView> findAll() {
			return List.of(new CoffeeOpeningHoursView(
					UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
					UUID.fromString("11111111-1111-1111-1111-111111111111"),
					"lundi: 08:00-18:00"
			));
		}

		@Override
		public void replaceForCoffee(UUID coffeeId, List<CoffeeOpeningHoursView> openingHours) {
		}

		@Override
		public void deleteForCoffee(UUID coffeeId) {
		}

		@Override
		public long count() {
			return 1;
		}
	}
}
