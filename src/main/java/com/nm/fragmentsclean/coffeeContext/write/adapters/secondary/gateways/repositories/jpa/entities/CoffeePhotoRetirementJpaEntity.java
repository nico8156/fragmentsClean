package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="coffee_photo_retirements")
public class CoffeePhotoRetirementJpaEntity {
    @Id public UUID photoId;
    public UUID coffeeId;
    public String photoUri;
    public boolean wasCover;
    public int sortOrder;
    public Instant retiredAt;
    public String lifecycleStatus;
    public Instant purgeRequestedAt;
    public Instant purgedAt;
    public UUID purgeCommandId;
    public CoffeePhotoRetirementJpaEntity(){}
}
