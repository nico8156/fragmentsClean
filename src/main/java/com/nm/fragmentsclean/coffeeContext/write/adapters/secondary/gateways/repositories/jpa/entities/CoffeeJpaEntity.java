package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@Entity(name = "coffees")
@ToString
@EqualsAndHashCode
@Getter
public class CoffeeJpaEntity {
    @Id
    private UUID id;
    private  String googleId;
    private  String displayName;
    private  String formattedAddress;
    private  String nationalPhoneNumber;
    private  String websiteUri;
    private  double latitude;
    private  double longitude;

    public CoffeeJpaEntity(
            UUID id,
            String googleId,
            String displayName,
            String formattedAddress,
            String nationalPhoneNumber,
            String websiteUri,
            double latitude,
            double longitude
    ) {
        this.id = id;
        this.googleId = googleId;
        this.displayName = displayName;
        this.formattedAddress = formattedAddress;
        this.nationalPhoneNumber = nationalPhoneNumber;
        this.websiteUri = websiteUri;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public CoffeeJpaEntity() {

    }
}
