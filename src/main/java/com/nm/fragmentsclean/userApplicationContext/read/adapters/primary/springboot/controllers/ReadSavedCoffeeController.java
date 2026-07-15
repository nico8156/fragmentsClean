package com.nm.fragmentsclean.userApplicationContext.read.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.userApplicationContext.read.projections.GetSavedCoffeesQuery;
import com.nm.fragmentsclean.userApplicationContext.read.projections.SavedCoffeeListView;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users/me/saved-coffees")
public class ReadSavedCoffeeController {
	private final QueryBus queryBus;

	public ReadSavedCoffeeController(QueryBus queryBus) {
		this.queryBus = queryBus;
	}

	@GetMapping
	public ResponseEntity<SavedCoffeeListView> list(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		return ResponseEntity.ok(queryBus.dispatch(new GetSavedCoffeesQuery(userId)));
	}
}
