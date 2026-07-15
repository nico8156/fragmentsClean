package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffee;

import java.util.Optional;
import java.util.UUID;

public interface SavedCoffeeRepository {
	Optional<SavedCoffee> byId(UUID savedCoffeeId);

	Optional<SavedCoffee> byUserIdAndCoffeeId(UUID userId, UUID coffeeId);

	void save(SavedCoffee savedCoffee);
}
