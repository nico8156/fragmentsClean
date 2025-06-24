package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import lombok.EqualsAndHashCode;

import java.util.UUID;
@EqualsAndHashCode
public class Coffee {
    private final UUID id ;
    private final String googleId;
    private final String displayName;
    private final String formattedAddress;
    private final String nationalPhoneNumber;
    private final String websiteUri;
    private final double latitude;
    private final double longitude;

    public Coffee(UUID id, String googleId, String displayName, String formattedAddress, String nationalPhoneNumber, String websiteUri, double latitude, double longitude) {
        this.id = id;
        this.googleId = googleId;
        this.displayName = displayName;
        this.formattedAddress = formattedAddress;
        this.nationalPhoneNumber = nationalPhoneNumber;
        this.websiteUri = websiteUri;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Coffee fromSnapShot( CoffeeSnapshot coffeeSnapshot) {
        return new Coffee(coffeeSnapshot.id(), coffeeSnapshot.googleId(), coffeeSnapshot.displayName(), coffeeSnapshot.formattedAddress(), coffeeSnapshot.nationalPhoneNumber(), coffeeSnapshot.websiteUri(), coffeeSnapshot.latitude(), coffeeSnapshot.longitude());
    }

    public static class Builder {
        private UUID id;
        private String googleId;
        private String displayName;
        private String formattedAddress;
        private String nationalPhoneNumber;
        private String websiteUri;
        private double latitude;
        private double longitude;

        public Builder Id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder GoogleId(String googleId) {
            this.googleId = googleId;
            return this;
        }
        public Builder DisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        public Builder FormattedAddress(String formattedAddress) {
            this.formattedAddress = formattedAddress;
            return this;
        }
        public Builder NationalPhoneNumber(String nationalPhoneNumber) {
            this.nationalPhoneNumber = nationalPhoneNumber;
            return this;
        }
        public Builder WebsiteUri(String websiteUri) {
            this.websiteUri = websiteUri;
            return this;
        }
        public Builder Latitude(double latitude) {
            this.latitude = latitude;
            return this;
        }
        public Builder Longitude(double longitude) {
            this.longitude = longitude;
            return this;
        }
        public Coffee build() {
            return new Coffee(id, googleId, displayName, formattedAddress, nationalPhoneNumber, websiteUri, latitude, longitude);
        }
    }
    public CoffeeSnapshot toSnapshot(){
        return new CoffeeSnapshot(id, googleId, displayName, formattedAddress, nationalPhoneNumber, websiteUri, latitude, longitude);
    }
    public Coffee fromSnapshot(CoffeeSnapshot snapshot){
        return new Coffee(
                snapshot.id(),
                snapshot.googleId(), snapshot.displayName(), snapshot.formattedAddress(), snapshot.nationalPhoneNumber(), snapshot.websiteUri(), snapshot.latitude(), snapshot.longitude());
    }
    public record CoffeeSnapshot(UUID id, String googleId, String displayName, String formattedAddress, String nationalPhoneNumber, String websiteUri, double latitude, double longitude){}
}
