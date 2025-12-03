package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;

import java.util.Objects;

public record Tag(String value) {
    public Tag {
        Objects.requireNonNull(value, "tag cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("tag cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
