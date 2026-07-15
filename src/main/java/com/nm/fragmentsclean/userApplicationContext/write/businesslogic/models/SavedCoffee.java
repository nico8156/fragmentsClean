package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class SavedCoffee extends AggregateRoot {
	private final UUID userId;
	private final UUID coffeeId;
	private boolean active;
	private Instant updatedAt;
	private long version;

	private SavedCoffee(
			UUID savedCoffeeId,
			UUID userId,
			UUID coffeeId,
			boolean active,
			Instant updatedAt,
			long version) {
		super(savedCoffeeId);
		this.userId = userId;
		this.coffeeId = coffeeId;
		this.active = active;
		this.updatedAt = updatedAt;
		this.version = version;
	}

	public static SavedCoffee createNew(UUID savedCoffeeId, UUID userId, UUID coffeeId, Instant now) {
		return new SavedCoffee(savedCoffeeId, userId, coffeeId, false, now, 0L);
	}

	public static SavedCoffee fromSnapshot(SavedCoffeeSnapshot snapshot) {
		return new SavedCoffee(
				snapshot.savedCoffeeId(),
				snapshot.userId(),
				snapshot.coffeeId(),
				snapshot.active(),
				snapshot.updatedAt(),
				snapshot.version());
	}

	public boolean applyState(boolean value, Instant now) {
		if (this.active == value) {
			return false;
		}
		this.active = value;
		this.updatedAt = now;
		this.version++;
		return true;
	}

	public void registerSavedCoffeeSetEvent(UUID commandId, Instant clientAt, Instant serverNow) {
		registerEvent(new SavedCoffeeSetEvent(
				UUID.randomUUID(),
				commandId,
				this.id,
				this.userId,
				this.coffeeId,
				this.active,
				this.version,
				serverNow,
				clientAt));
	}

	public SavedCoffeeSnapshot toSnapshot() {
		return new SavedCoffeeSnapshot(id, userId, coffeeId, active, updatedAt, version);
	}

	public record SavedCoffeeSnapshot(
			UUID savedCoffeeId,
			UUID userId,
			UUID coffeeId,
			boolean active,
			Instant updatedAt,
			long version) {
	}
}
