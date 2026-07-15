package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.controllers;

public record SavedCoffeeRequestDto(
		String commandId,
		String savedCoffeeId,
		String coffeeId,
		boolean value,
		String at
) {
}
