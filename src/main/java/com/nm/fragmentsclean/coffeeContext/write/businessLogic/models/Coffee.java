package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.*;

public final class Coffee extends AggregateRoot {

	private final CoffeeId coffeeId; // VO d'identité domaine

	private final GooglePlaceId googleId; // optionnel

	private CoffeeName name;
	private Address address;
	private GeoPoint location;
	private PhoneNumber phoneNumber;
	private WebsiteUrl website;
	private Set<Tag> tags;

	private List<Photo> photos;
	private OpeningHours openingHours;

	private int version;
	private Instant updatedAt;

	// ========= CTOR privé "complet" =========

	private Coffee(CoffeeId coffeeId,
			GooglePlaceId googleId,
			CoffeeName name,
			Address address,
			GeoPoint location,
			PhoneNumber phoneNumber,
			WebsiteUrl website,
			Set<Tag> tags,
			List<Photo> photos,
			OpeningHours openingHours,
			int version,
			Instant updatedAt) {

		super(coffeeId.value()); // <-- on remonte l'UUID brut à AggregateRoot
		this.coffeeId = Objects.requireNonNull(coffeeId, "coffee id required");

		this.googleId = googleId; // peut être null

		this.name = Objects.requireNonNull(name, "coffee name required");
		this.address = address != null ? address : Address.empty();
		this.location = Objects.requireNonNull(location, "location required");
		this.phoneNumber = phoneNumber;
		this.website = website;
		this.tags = tags != null ? Set.copyOf(tags) : Set.of();
		this.photos = photos != null ? new ArrayList<>(photos) : new ArrayList<>();
		this.openingHours = openingHours != null ? openingHours : OpeningHours.empty();
		this.version = version;
		this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
	}

	// ========= Factory "createNew" (pour les commands) =========

	public static Coffee createNew(
			CoffeeId coffeeId,
			GooglePlaceId googleId,
			CoffeeName name,
			Address address,
			GeoPoint location,
			PhoneNumber phoneNumber,
			WebsiteUrl website,
			Set<Tag> tags,
			Instant now) {
		return new Coffee(
				coffeeId != null ? coffeeId : CoffeeId.newId(),
				googleId,
				name,
				address,
				location,
				phoneNumber,
				website,
				tags,
				/* photos */ List.of(),
				/* openingHours */ OpeningHours.empty(),
				/* version */ 0,
				now);
	}

	// ========= Factory "rehydrate" (pour les repos JPA) =========

	public static Coffee rehydrate(
			CoffeeId coffeeId,
			GooglePlaceId googleId,
			CoffeeName name,
			Address address,
			GeoPoint location,
			PhoneNumber phoneNumber,
			WebsiteUrl website,
			Set<Tag> tags,
			List<Photo> photos,
			OpeningHours openingHours,
			int version,
			Instant updatedAt) {
		return new Coffee(
				coffeeId,
				googleId,
				name,
				address,
				location,
				phoneNumber,
				website,
				tags,
				photos,
				openingHours,
				version,
				updatedAt);
	}

	// ========= Getters domaine =========

	public CoffeeId coffeeId() {
		return coffeeId;
	}

	public Optional<GooglePlaceId> googleId() {
		return Optional.ofNullable(googleId);
	}

	public CoffeeName name() {
		return name;
	}

	public Address address() {
		return address;
	}

	public GeoPoint location() {
		return location;
	}

	public PhoneNumber phoneNumber() {
		return phoneNumber;
	}

	public WebsiteUrl website() {
		return website;
	}

	public Set<Tag> tags() {
		return Collections.unmodifiableSet(tags);
	}

	public List<Photo> photos() {
		return Collections.unmodifiableList(photos);
	}

	public OpeningHours openingHours() {
		return openingHours;
	}

	public int version() {
		return version;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	// ========= Behavior =========

	public void rename(CoffeeName newName, Instant now) {
		this.name = Objects.requireNonNull(newName);
		touch(now);
	}

	public void changeAddress(Address newAddress, Instant now) {
		this.address = Objects.requireNonNull(newAddress);
		touch(now);
	}

	public void moveTo(GeoPoint newLocation, Instant now) {
		this.location = Objects.requireNonNull(newLocation);
		touch(now);
	}

	public void changeContact(PhoneNumber phone, WebsiteUrl website, Instant now) {
		this.phoneNumber = phone;
		this.website = website;
		touch(now);
	}

	public void setTags(Set<Tag> newTags, Instant now) {
		this.tags = newTags != null ? Set.copyOf(newTags) : Set.of();
		touch(now);
	}

	public void replacePhotos(List<Photo> newPhotos, Instant now) {
		this.photos = new ArrayList<>(newPhotos != null ? newPhotos : List.of());
		touch(now);
	}

	public void setOpeningHours(OpeningHours newOpeningHours, Instant now) {
		this.openingHours = Objects.requireNonNull(newOpeningHours);
		touch(now);
	}

	private void touch(Instant now) {
		this.version += 1;
		this.updatedAt = now != null ? now : Instant.now();
	}
}
