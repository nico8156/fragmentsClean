package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;
import java.time.Instant;
import java.time.Duration;
import java.util.Objects;
public final class CoffeePhotoRetirement {
    public static final Duration MINIMUM_RETENTION=Duration.ofDays(30);
    private final Photo photo;
    private final Instant retiredAt;
    public CoffeePhotoRetirement(Photo photo,Instant retiredAt){this.photo=Objects.requireNonNull(photo);this.retiredAt=Objects.requireNonNull(retiredAt);}
    public Photo photo(){return photo;}
    public Instant retiredAt(){return retiredAt;}
    public boolean eligibleForPurge(Instant now,boolean used){return !used && !Objects.requireNonNull(now).isBefore(retiredAt.plus(MINIMUM_RETENTION));}
}
