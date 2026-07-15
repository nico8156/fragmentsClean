package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "saved_coffees")
@Getter
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class SavedCoffeeJpaEntity {
	@Id
	private UUID savedCoffeeId;

	private UUID userId;
	private UUID coffeeId;
	private boolean active;
	private Instant updatedAt;
	private long version;

	public SavedCoffeeJpaEntity(
			UUID savedCoffeeId,
			UUID userId,
			UUID coffeeId,
			boolean active,
			Instant updatedAt,
			long version) {
		this.savedCoffeeId = savedCoffeeId;
		this.userId = userId;
		this.coffeeId = coffeeId;
		this.active = active;
		this.updatedAt = updatedAt;
		this.version = version;
	}
}
