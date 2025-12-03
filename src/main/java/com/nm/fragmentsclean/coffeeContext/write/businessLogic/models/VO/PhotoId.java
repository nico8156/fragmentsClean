package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;

import java.util.Objects;
import java.util.UUID;

public record PhotoId(UUID value) {
    public PhotoId {
        Objects.requireNonNull(value, "photo id cannot be null");
    }

    public static PhotoId newId() {
        return new PhotoId(UUID.randomUUID());
    }

    public static PhotoId fromString(String raw) {
        return new PhotoId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
