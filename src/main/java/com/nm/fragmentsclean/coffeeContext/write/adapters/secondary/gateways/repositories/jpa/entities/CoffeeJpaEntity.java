package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coffees")
public class CoffeeJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "google_place_id")
    private String googlePlaceId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "city")
    private String city;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "country")
    private String country;

    @Column(name = "lat", nullable = false)
    private double lat;

    @Column(name = "lon", nullable = false)
    private double lon;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "website")
    private String website;

    // tags stockés en CSV simple pour commencer: "espresso,chain"
    @Column(name = "tags_csv")
    private String tagsCsv;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CoffeeJpaEntity() {
        // for JPA
    }

    public CoffeeJpaEntity(UUID id,
                           String googlePlaceId,
                           String name,
                           String addressLine1,
                           String city,
                           String postalCode,
                           String country,
                           double lat,
                           double lon,
                           String phoneNumber,
                           String website,
                           String tagsCsv,
                           int version,
                           Instant updatedAt) {
        this.id = id;
        this.googlePlaceId = googlePlaceId;
        this.name = name;
        this.addressLine1 = addressLine1;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.lat = lat;
        this.lon = lon;
        this.phoneNumber = phoneNumber;
        this.website = website;
        this.tagsCsv = tagsCsv;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getGooglePlaceId() {
        return googlePlaceId;
    }

    public String getName() {
        return name;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getWebsite() {
        return website;
    }

    public String getTagsCsv() {
        return tagsCsv;
    }

    public int getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
