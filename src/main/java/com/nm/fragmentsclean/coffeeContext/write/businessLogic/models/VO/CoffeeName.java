package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;

import java.util.Objects;

public record CoffeeName(String value) {
    public CoffeeName {
        Objects.requireNonNull(value, "coffee name cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("coffee name is required");
        }
        if (value.length() > 200) {
            throw new IllegalArgumentException("coffee name too long");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
