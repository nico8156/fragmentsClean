package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

public record Title (String value) {
    public Title {
        if (value == null || value.isBlank() || value.isEmpty() || value.trim().isEmpty() || value.trim().isBlank()) {
            throw new IllegalArgumentException("Title must not be null");
        }
    }
}
