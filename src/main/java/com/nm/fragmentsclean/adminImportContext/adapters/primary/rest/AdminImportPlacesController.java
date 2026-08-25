package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeePreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlacePhotoPreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceSearchResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.PreviewGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchGooglePlacesForCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RecordAdminAudit;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

@RestController
@RequestMapping("/api/admin/import/places")
public class AdminImportPlacesController {
	private final SearchGooglePlacesForCoffee searchGooglePlacesForCoffee;
	private final PreviewGooglePlaceCoffee previewGooglePlaceCoffee;
	private final ImportGooglePlaceCoffee importGooglePlaceCoffee;
	private final RecordAdminAudit recordAdminAudit;
	private final DateTimeProvider dateTimeProvider;

	public AdminImportPlacesController(SearchGooglePlacesForCoffee searchGooglePlacesForCoffee,
			PreviewGooglePlaceCoffee previewGooglePlaceCoffee,
			ImportGooglePlaceCoffee importGooglePlaceCoffee,
			RecordAdminAudit recordAdminAudit,
			DateTimeProvider dateTimeProvider) {
		this.searchGooglePlacesForCoffee = searchGooglePlacesForCoffee;
		this.previewGooglePlaceCoffee = previewGooglePlaceCoffee;
		this.importGooglePlaceCoffee = importGooglePlaceCoffee;
		this.recordAdminAudit = recordAdminAudit;
		this.dateTimeProvider = dateTimeProvider;
	}

	public AdminImportPlacesController(SearchGooglePlacesForCoffee searchGooglePlacesForCoffee,
			PreviewGooglePlaceCoffee previewGooglePlaceCoffee,
			ImportGooglePlaceCoffee importGooglePlaceCoffee) {
		this(searchGooglePlacesForCoffee, previewGooglePlaceCoffee, importGooglePlaceCoffee,
				new RecordAdminAudit(entry -> {}), Instant::now);
	}

	@GetMapping
	public List<AdminImportPlaceSearchResponse> search(@RequestParam String query) {
		return searchGooglePlacesForCoffee.execute(query).stream()
				.map(this::toSearchResponse)
				.toList();
	}

	@GetMapping("/{googlePlaceId}/preview")
	public AdminImportPlacePreviewResponse preview(@PathVariable String googlePlaceId) {
		return toPreviewResponse(previewGooglePlaceCoffee.execute(googlePlaceId));
	}

	@PostMapping("/{googlePlaceId}/import")
	public ResponseEntity<AdminImportPlaceImportResponse> importPlace(@PathVariable String googlePlaceId, Authentication authentication) {
		var imported = importGooglePlaceCoffee.execute(googlePlaceId);
		if (authentication != null) {
			recordAdminAudit.execute(UUID.fromString(authentication.getName()), "COFFEE_IMPORTED", "COFFEE",
					imported.coffeeId(), imported.commandId(), imported.status().name(), dateTimeProvider.now());
		}
		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(new AdminImportPlaceImportResponse(
						imported.commandId(),
						imported.coffeeId(),
						imported.googlePlaceId(),
						imported.status().name()
				));
	}

	public ResponseEntity<AdminImportPlaceImportResponse> importPlace(String googlePlaceId) {
		return importPlace(googlePlaceId, null);
	}

	private AdminImportPlaceSearchResponse toSearchResponse(GooglePlaceSearchResult result) {
		return new AdminImportPlaceSearchResponse(
				result.googlePlaceId(),
				result.name(),
				result.formattedAddress(),
				result.latitude(),
				result.longitude()
		);
	}

	private AdminImportPlacePreviewResponse toPreviewResponse(GooglePlaceCoffeePreview preview) {
		return new AdminImportPlacePreviewResponse(
				preview.googlePlaceId(),
				new AdminImportPlaceInfoPreviewResponse(
						preview.name(),
						preview.formattedAddress(),
						preview.latitude(),
						preview.longitude(),
						preview.phoneNumber(),
						preview.website(),
						null,
						null
				),
				new AdminImportOpeningHoursPreviewResponse(
						preview.openingHours(),
						List.of()
				),
				preview.photos().stream().map(this::toPhotoResponse).toList()
		);
	}

	private AdminImportPlacePhotoResponse toPhotoResponse(GooglePlacePhotoPreview photo) {
		return new AdminImportPlacePhotoResponse(
				photo.name(),
				photo.widthPx(),
				photo.heightPx(),
				photo.temporaryPhotoUri(),
				photo.authorAttributions()
		);
	}
}
