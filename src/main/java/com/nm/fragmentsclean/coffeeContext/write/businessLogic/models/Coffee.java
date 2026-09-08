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
	private Instant archivedAt;
	private CoffeePublicationStatus publicationStatus;

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
			Instant updatedAt,
			Instant archivedAt,
			CoffeePublicationStatus publicationStatus) {

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
		this.archivedAt = archivedAt;
		this.publicationStatus = publicationStatus != null ? publicationStatus :
				(archivedAt != null ? CoffeePublicationStatus.ARCHIVED : CoffeePublicationStatus.PUBLISHED);
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
		return createNew(coffeeId, googleId, name, address, location, phoneNumber, website, tags, now,
				CoffeePublicationStatus.PUBLISHED);
	}

	public static Coffee createNew(CoffeeId coffeeId, GooglePlaceId googleId, CoffeeName name,
			Address address, GeoPoint location, PhoneNumber phoneNumber, WebsiteUrl website,
			Set<Tag> tags, Instant now, CoffeePublicationStatus publicationStatus) {
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
				now,
			null, publicationStatus);
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
			Instant updatedAt,
			Instant archivedAt) {
		return rehydrate(coffeeId, googleId, name, address, location, phoneNumber, website, tags, photos,
				openingHours, version, updatedAt, archivedAt, null);
	}

	public static Coffee rehydrate(CoffeeId coffeeId, GooglePlaceId googleId, CoffeeName name,
			Address address, GeoPoint location, PhoneNumber phoneNumber, WebsiteUrl website, Set<Tag> tags,
			List<Photo> photos, OpeningHours openingHours, int version, Instant updatedAt, Instant archivedAt,
			CoffeePublicationStatus publicationStatus) {
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
				updatedAt,
				archivedAt, publicationStatus);
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

	public Optional<Instant> archivedAt() {
		return Optional.ofNullable(archivedAt);
	}

	public boolean isArchived() {
		return archivedAt != null;
	}

	public CoffeePublicationStatus publicationStatus() { return publicationStatus; }

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

	public void editDetails(CoffeeName newName, Address newAddress, GeoPoint newLocation,
			PhoneNumber newPhoneNumber, WebsiteUrl newWebsite, Set<Tag> newTags, Instant now) {
		if (isArchived()) throw new IllegalStateException("Archived coffee cannot be edited");
		this.name = Objects.requireNonNull(newName);
		this.address = Objects.requireNonNull(newAddress);
		this.location = Objects.requireNonNull(newLocation);
		this.phoneNumber = newPhoneNumber;
		this.website = newWebsite;
		this.tags = newTags != null ? Set.copyOf(newTags) : Set.of();
		touch(now);
	}

	public void replacePhotos(List<Photo> newPhotos, Instant now) {
		this.photos = normalizePhotos(newPhotos);
		touch(now);
	}

	public void addPhoto(Photo photo, Instant now) {
		Objects.requireNonNull(photo, "photo required");
		if (!photo.coffeeId().equals(coffeeId)) throw new IllegalArgumentException("Photo belongs to another coffee");
		if (photos.stream().anyMatch(existing -> existing.id().equals(photo.id()))) return;
		var next = new ArrayList<>(photos);
		next.add(new Photo(photo.id(), coffeeId, photo.uri(), next.isEmpty(), next.size()));
		this.photos = normalizePhotos(next);
		touch(now);
	}

	public void removePhoto(PhotoId photoId, Instant now) {
		var next = photos.stream().filter(photo -> !photo.id().equals(photoId)).toList();
		if (next.size() == photos.size()) throw new IllegalArgumentException("Photo not found: " + photoId.value());
		this.photos = normalizePhotos(next);
		touch(now);
	}

	public void arrangePhotos(List<PhotoId> orderedPhotoIds, PhotoId coverPhotoId, Instant now) {
		Objects.requireNonNull(orderedPhotoIds, "ordered photo ids required");
		if (orderedPhotoIds.size() != photos.size() || new HashSet<>(orderedPhotoIds).size() != photos.size())
			throw new IllegalArgumentException("Photo order must contain every photo exactly once");
		var byId = photos.stream().collect(java.util.stream.Collectors.toMap(Photo::id, photo -> photo));
		if (!byId.keySet().equals(new HashSet<>(orderedPhotoIds))) throw new IllegalArgumentException("Unknown photo in order");
		if (coverPhotoId != null && !byId.containsKey(coverPhotoId)) throw new IllegalArgumentException("Unknown cover photo");
		var cover = coverPhotoId != null ? coverPhotoId : orderedPhotoIds.stream().findFirst().orElse(null);
		var next = new ArrayList<Photo>();
		for (int index = 0; index < orderedPhotoIds.size(); index++) {
			var photo = byId.get(orderedPhotoIds.get(index));
			next.add(new Photo(photo.id(), coffeeId, photo.uri(), photo.id().equals(cover), index));
		}
		this.photos = next;
		touch(now);
	}

	private ArrayList<Photo> normalizePhotos(List<Photo> source) {
		var ordered = new ArrayList<>(source == null ? List.<Photo>of() : source);
		ordered.sort(Comparator.comparingInt(Photo::sortOrder));
		var normalized = new ArrayList<Photo>();
		var requestedCover = ordered.stream().filter(Photo::isCover).findFirst().map(Photo::id).orElse(null);
		for (int index = 0; index < ordered.size(); index++) {
			var photo = ordered.get(index);
			normalized.add(new Photo(photo.id(), coffeeId, photo.uri(),
					requestedCover == null ? index == 0 : photo.id().equals(requestedCover), index));
		}
		return normalized;
	}

	public void setOpeningHours(OpeningHours newOpeningHours, Instant now) {
		this.openingHours = Objects.requireNonNull(newOpeningHours);
		touch(now);
	}

	public void archive(Instant now) {
		if (isArchived()) {
			return;
		}
		touch(now);
		this.archivedAt = this.updatedAt;
		this.publicationStatus = CoffeePublicationStatus.ARCHIVED;
	}

	public void publish(Instant now) {
		if (isArchived()) throw new IllegalStateException("Archived coffee cannot be published");
		if (publicationStatus == CoffeePublicationStatus.PUBLISHED) return;
		touch(now);
		publicationStatus = CoffeePublicationStatus.PUBLISHED;
	}

	public void unpublish(Instant now) {
		if (isArchived()) throw new IllegalStateException("Archived coffee cannot be unpublished");
		if (publicationStatus == CoffeePublicationStatus.DRAFT) return;
		touch(now);
		publicationStatus = CoffeePublicationStatus.DRAFT;
	}

	private void touch(Instant now) {
		this.version += 1;
		this.updatedAt = now != null ? now : Instant.now();
	}
}
