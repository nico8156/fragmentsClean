package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class CoffeePhotosReadController {

	private final CoffeePhotoProjectionRepository repo;

	CoffeePhotosReadController(CoffeePhotoProjectionRepository repo) {
		this.repo = repo;
	}

	@GetMapping("/api/coffees/photos")
	public List<CoffeePhotoView> listAllPhotos() {
		return repo.findAll();
	}
}
