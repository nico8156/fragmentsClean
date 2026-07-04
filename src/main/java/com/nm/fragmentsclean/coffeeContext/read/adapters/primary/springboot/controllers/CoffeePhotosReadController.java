package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoUriResolver;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class CoffeePhotosReadController {

	private final CoffeePhotoProjectionRepository repo;
	private final CoffeePhotoUriResolver photoUriResolver;

	CoffeePhotosReadController(CoffeePhotoProjectionRepository repo, CoffeePhotoUriResolver photoUriResolver) {
		this.repo = repo;
		this.photoUriResolver = photoUriResolver;
	}

	@GetMapping("/api/coffees/photos")
	public List<CoffeePhotoResponse> listAllPhotos() {
		return repo.findAll().stream()
				.map(photo -> new CoffeePhotoResponse(
						photo.id(),
						photo.coffeeId(),
						photoUriResolver.resolve(photo.photoUri())))
				.toList();
	}

	record CoffeePhotoResponse(java.util.UUID id, java.util.UUID coffeeId, String photoUri) {
	}
}
