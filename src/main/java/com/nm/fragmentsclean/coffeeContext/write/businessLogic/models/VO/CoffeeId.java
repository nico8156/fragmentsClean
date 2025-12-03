package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;

import java.util.Objects;
import java.util.UUID;

public record CoffeeId(UUID value) {
    public CoffeeId {
        Objects.requireNonNull(value, "coffee id cannot be null");
    }

    public static CoffeeId newId() {
        return new CoffeeId(UUID.randomUUID());
    }

    public static CoffeeId fromString(String raw) {
        return new CoffeeId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
