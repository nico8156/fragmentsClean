package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class CoffeeOpeningHoursReadController {

	private final CoffeeOpeningHoursProjectionRepository repo;

	CoffeeOpeningHoursReadController(CoffeeOpeningHoursProjectionRepository repo) {
		this.repo = repo;
	}

	@GetMapping("/api/coffees/opening-hours")
	public List<CoffeeOpeningHoursView> listAllOpeningHours() {
		return repo.findAll();
	}
}
