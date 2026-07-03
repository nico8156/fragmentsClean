package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeePreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlacePhotoPreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceSearchResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.PreviewGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchGooglePlacesForCoffee;

@RestController
@RequestMapping("/api/admin/import/places")
public class AdminImportPlacesController {
	private final SearchGooglePlacesForCoffee searchGooglePlacesForCoffee;
	private final PreviewGooglePlaceCoffee previewGooglePlaceCoffee;
	private final ImportGooglePlaceCoffee importGooglePlaceCoffee;

	public AdminImportPlacesController(SearchGooglePlacesForCoffee searchGooglePlacesForCoffee,
			PreviewGooglePlaceCoffee previewGooglePlaceCoffee,
			ImportGooglePlaceCoffee importGooglePlaceCoffee) {
		this.searchGooglePlacesForCoffee = searchGooglePlacesForCoffee;
		this.previewGooglePlaceCoffee = previewGooglePlaceCoffee;
		this.importGooglePlaceCoffee = importGooglePlaceCoffee;
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
	public ResponseEntity<AdminImportPlaceImportResponse> importPlace(@PathVariable String googlePlaceId) {
		var imported = importGooglePlaceCoffee.execute(googlePlaceId);
		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(new AdminImportPlaceImportResponse(
						imported.commandId(),
						imported.coffeeId(),
						imported.googlePlaceId()
				));
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
				preview.name(),
				preview.formattedAddress(),
				preview.addressLine1(),
				preview.city(),
				preview.postalCode(),
				preview.country(),
				preview.latitude(),
				preview.longitude(),
				preview.phoneNumber(),
				preview.website(),
				preview.openingHours(),
				preview.photos().stream().map(this::toPhotoResponse).toList()
		);
	}

	private AdminImportPlacePhotoResponse toPhotoResponse(GooglePlacePhotoPreview photo) {
		return new AdminImportPlacePhotoResponse(
				photo.name(),
				photo.widthPx(),
				photo.heightPx(),
				photo.authorAttributions()
		);
	}
}
