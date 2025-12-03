package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;

import java.util.Objects;

public record GooglePlaceId(String value) {
    public GooglePlaceId {
        Objects.requireNonNull(value, "google place id cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("google place id cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
