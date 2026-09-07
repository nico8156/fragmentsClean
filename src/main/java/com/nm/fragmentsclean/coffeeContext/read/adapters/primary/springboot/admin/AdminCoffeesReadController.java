package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.admin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import com.nm.fragmentsclean.coffeeContext.read.ListCoffeesQuery;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoUriResolver;
import com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers.CoffeeSummaryResponse;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ArchiveCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.AddCoffeePhotoCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ArrangeCoffeePhotosCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.DeleteCoffeePhotoCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.DeleteCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.EditCoffeeDetailsCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.PublishCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.UpdateCoffeeOpeningHoursCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AdminAuditRecorder;

@RestController
public class AdminCoffeesReadController {
	private final CommandBus commandBus;
	private final QueryBus queryBus;
	private final CoffeePhotoProjectionRepository photoProjectionRepository;
	private final CoffeeOpeningHoursProjectionRepository openingHoursProjectionRepository;
	private final CoffeePhotoUriResolver photoUriResolver;
	private final AdminAuditRecorder adminAuditRecorder;

	@Autowired
	public AdminCoffeesReadController(CommandBus commandBus,
			QueryBus queryBus,
			CoffeePhotoProjectionRepository photoProjectionRepository,
			CoffeeOpeningHoursProjectionRepository openingHoursProjectionRepository,
			CoffeePhotoUriResolver photoUriResolver,
			AdminAuditRecorder adminAuditRecorder) {
		this.commandBus = commandBus;
		this.queryBus = queryBus;
		this.photoProjectionRepository = photoProjectionRepository;
		this.openingHoursProjectionRepository = openingHoursProjectionRepository;
		this.photoUriResolver = photoUriResolver;
		this.adminAuditRecorder = adminAuditRecorder;
	}

	public AdminCoffeesReadController(CommandBus commandBus, QueryBus queryBus,
			CoffeePhotoProjectionRepository photoProjectionRepository,
			CoffeeOpeningHoursProjectionRepository openingHoursProjectionRepository,
			CoffeePhotoUriResolver photoUriResolver) {
		this(commandBus, queryBus, photoProjectionRepository, openingHoursProjectionRepository,
				photoUriResolver, (actor, action, type, target, command, outcome, occurred) -> {});
	}

	@GetMapping("/api/admin/coffees")
	public List<AdminCoffeeResponse> listCoffees() {
		Map<UUID, List<AdminCoffeePhotoResponse>> photosByCoffeeId = photoProjectionRepository.findAll().stream()
				.collect(Collectors.groupingBy(
						CoffeePhotoView::coffeeId,
						Collectors.mapping(photo -> AdminCoffeePhotoResponse.from(photo, photoUriResolver), Collectors.toList())
				));
		Map<UUID, List<AdminCoffeeOpeningHoursResponse>> openingHoursByCoffeeId =
				openingHoursProjectionRepository.findAll().stream()
						.collect(Collectors.groupingBy(
								CoffeeOpeningHoursView::coffeeId,
								Collectors.mapping(AdminCoffeeOpeningHoursResponse::from, Collectors.toList())
						));

		var views = queryBus.dispatch(new ListCoffeesQuery(false));
		return views.stream()
				.map(CoffeeSummaryResponse::from)
				.map(summary -> AdminCoffeeResponse.from(
						summary,
						photosByCoffeeId.getOrDefault(summary.id(), List.of()),
						openingHoursByCoffeeId.getOrDefault(summary.id(), List.of())
				))
				.toList();
	}

	@DeleteMapping("/api/admin/coffees/{coffeeId}")
	public ResponseEntity<AdminCommandAcceptedResponse> archiveCoffee(@PathVariable UUID coffeeId, Authentication authentication) {
		var commandId = UUID.randomUUID();
		var now = java.time.Instant.now();
		commandBus.dispatch(new ArchiveCoffeeCommand(commandId, coffeeId, now));
		audit(authentication, "COFFEE_ARCHIVED", coffeeId, commandId, "ACCEPTED", now);
		return ResponseEntity.accepted().body(AdminCommandAcceptedResponse.pending(commandId));
	}

	public ResponseEntity<AdminCommandAcceptedResponse> archiveCoffee(UUID coffeeId) { return archiveCoffee(coffeeId, null); }

	@DeleteMapping("/api/admin/coffees/{coffeeId}/permanent")
	public ResponseEntity<AdminCommandAcceptedResponse> permanentlyDeleteCoffee(@PathVariable UUID coffeeId, Authentication authentication) {
		var commandId = UUID.randomUUID();
		var now = java.time.Instant.now();
		commandBus.dispatch(new DeleteCoffeeCommand(commandId, coffeeId, now));
		audit(authentication, "COFFEE_DELETED", coffeeId, commandId, "ACCEPTED", now);
		return ResponseEntity.accepted().body(AdminCommandAcceptedResponse.pending(commandId));
	}

	public ResponseEntity<AdminCommandAcceptedResponse> permanentlyDeleteCoffee(UUID coffeeId) {
		return permanentlyDeleteCoffee(coffeeId, null);
	}

	@PostMapping("/api/admin/coffees/{coffeeId}/publish")
	public ResponseEntity<AdminCommandAcceptedResponse> publishCoffee(@PathVariable UUID coffeeId, Authentication authentication) {
		var commandId = UUID.randomUUID(); var now = java.time.Instant.now();
		commandBus.dispatch(new PublishCoffeeCommand(commandId, coffeeId, now));
		audit(authentication, "COFFEE_PUBLISHED", coffeeId, commandId, "ACCEPTED", now);
		return ResponseEntity.accepted().body(AdminCommandAcceptedResponse.pending(commandId));
	}

	@PutMapping("/api/admin/coffees/{coffeeId}/details")
	public ResponseEntity<AdminCommandAcceptedResponse> editDetails(@PathVariable UUID coffeeId,
			@RequestBody EditCoffeeDetailsRequest request, Authentication authentication) {
		var commandId = UUID.randomUUID();
		var now = java.time.Instant.now();
		commandBus.dispatch(new EditCoffeeDetailsCommand(commandId, coffeeId, request.name(),
				request.addressLine1(), request.city(), request.postalCode(), request.country(),
				request.latitude(), request.longitude(), request.phoneNumber(), request.website(), request.tags(), now));
		audit(authentication, "COFFEE_DETAILS_EDITED", coffeeId, commandId, "ACCEPTED", now);
		return ResponseEntity.accepted().body(AdminCommandAcceptedResponse.pending(commandId));
	}

	@PutMapping("/api/admin/coffees/{coffeeId}/opening-hours")
	public ResponseEntity<AdminCommandAcceptedResponse> updateOpeningHours(@PathVariable UUID coffeeId,
			@RequestBody UpdateCoffeeOpeningHoursRequest request, Authentication authentication) {
		var commandId = UUID.randomUUID();
		var now = java.time.Instant.now();
		var schedules = request.schedules().stream().map(schedule -> new UpdateCoffeeOpeningHoursCommand.DaySchedule(
				schedule.dayCode(), schedule.windows().stream().map(window -> new UpdateCoffeeOpeningHoursCommand.TimeWindow(
						window.startMinute(), window.endMinute())).toList())).toList();
		commandBus.dispatch(new UpdateCoffeeOpeningHoursCommand(commandId, coffeeId, schedules, now));
		audit(authentication, "COFFEE_OPENING_HOURS_UPDATED", coffeeId, commandId, "ACCEPTED", now);
		return ResponseEntity.accepted().body(AdminCommandAcceptedResponse.pending(commandId));
	}

	@PostMapping(value = "/api/admin/coffees/{coffeeId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AdminCommandAcceptedResponse> addPhoto(@PathVariable UUID coffeeId, @RequestPart("photo") MultipartFile photo, Authentication authentication)
			throws java.io.IOException {
		var commandId = UUID.randomUUID();
		var now = java.time.Instant.now();
		commandBus.dispatch(new AddCoffeePhotoCommand(
				commandId,
				coffeeId,
				photo.getOriginalFilename(),
				photo.getContentType(),
				photo.getBytes(),
				now));
		audit(authentication, "COFFEE_PHOTO_ADDED", coffeeId, commandId, "ACCEPTED", now);
		return ResponseEntity.accepted().body(AdminCommandAcceptedResponse.pending(commandId));
	}

	@DeleteMapping("/api/admin/coffees/{coffeeId}/photos/{photoId}")
	public ResponseEntity<AdminCommandAcceptedResponse> deletePhoto(@PathVariable UUID coffeeId, @PathVariable UUID photoId, Authentication authentication) {
		var commandId = UUID.randomUUID();
		var now = java.time.Instant.now();
		commandBus.dispatch(new DeleteCoffeePhotoCommand(
				commandId,
				coffeeId,
				photoId,
				now));
		audit(authentication, "COFFEE_PHOTO_DELETED", photoId, commandId, "ACCEPTED", now);
		return ResponseEntity.accepted().body(AdminCommandAcceptedResponse.pending(commandId));
	}

	@PutMapping("/api/admin/coffees/{coffeeId}/photos/order")
	public ResponseEntity<AdminCommandAcceptedResponse> arrangePhotos(@PathVariable UUID coffeeId,
			@RequestBody ArrangeCoffeePhotosRequest request, Authentication authentication) {
		var commandId=UUID.randomUUID(); var now=java.time.Instant.now();
		commandBus.dispatch(new ArrangeCoffeePhotosCommand(commandId, coffeeId, request.orderedPhotoIds(), request.coverPhotoId(), now));
		audit(authentication, "COFFEE_PHOTOS_ARRANGED", coffeeId, commandId, "ACCEPTED", now);
		return ResponseEntity.accepted().body(AdminCommandAcceptedResponse.pending(commandId));
	}

	public record ArrangeCoffeePhotosRequest(List<UUID> orderedPhotoIds, UUID coverPhotoId) { }

	public ResponseEntity<AdminCommandAcceptedResponse> addPhoto(UUID coffeeId, MultipartFile photo) throws java.io.IOException {
		return addPhoto(coffeeId, photo, null);
	}

	public ResponseEntity<AdminCommandAcceptedResponse> deletePhoto(UUID coffeeId, UUID photoId) {
		return deletePhoto(coffeeId, photoId, null);
	}

	private void audit(Authentication authentication, String action, UUID targetId, UUID commandId,
			String outcome, java.time.Instant occurredAt) {
		if (authentication != null) {
			adminAuditRecorder.record(UUID.fromString(authentication.getName()), action, "COFFEE", targetId,
					commandId, outcome, occurredAt);
		}
	}

	public record AdminCoffeeResponse(
			UUID id,
			String googleId,
			String name,
			CoffeeSummaryResponse.Location location,
			CoffeeSummaryResponse.Address address,
			String phoneNumber,
			String website,
			java.util.Set<String> tags,
			String publicationStatus,
			long version,
			java.time.Instant updatedAt,
			List<AdminCoffeePhotoResponse> photos,
			List<AdminCoffeeOpeningHoursResponse> openingHours) {
		static AdminCoffeeResponse from(CoffeeSummaryResponse summary,
				List<AdminCoffeePhotoResponse> photos,
				List<AdminCoffeeOpeningHoursResponse> openingHours) {
			return new AdminCoffeeResponse(
					summary.id(),
					summary.googleId(),
					summary.name(),
					summary.location(),
					summary.address(),
					summary.phoneNumber(),
					summary.website(),
					summary.tags(),
					summary.publicationStatus(),
					summary.version(),
					summary.updatedAt(),
					photos,
					openingHours
			);
		}
	}

	public record AdminCoffeePhotoResponse(UUID id, String photoUri, boolean cover, int sortOrder) {
		static AdminCoffeePhotoResponse from(CoffeePhotoView view, CoffeePhotoUriResolver photoUriResolver) {
			return new AdminCoffeePhotoResponse(view.id(), photoUriResolver.resolve(view.photoUri()), view.cover(), view.sortOrder());
		}
	}

	public record EditCoffeeDetailsRequest(String name, String addressLine1, String city, String postalCode,
			String country, double latitude, double longitude, String phoneNumber, String website,
			java.util.Set<String> tags) { }
	public record UpdateCoffeeOpeningHoursRequest(List<DayScheduleRequest> schedules) {
		public UpdateCoffeeOpeningHoursRequest { schedules = schedules == null ? List.of() : List.copyOf(schedules); }
	}
	public record DayScheduleRequest(int dayCode, List<TimeWindowRequest> windows) {
		public DayScheduleRequest { windows = windows == null ? List.of() : List.copyOf(windows); }
	}
	public record TimeWindowRequest(int startMinute, int endMinute) { }

	public record AdminCoffeeOpeningHoursResponse(UUID id, String weekdayDescription) {
		static AdminCoffeeOpeningHoursResponse from(CoffeeOpeningHoursView view) {
			return new AdminCoffeeOpeningHoursResponse(view.id(), view.weekdayDescription());
		}
	}
}
