package com.nm.fragmentsclean.aticleContext;

public record ContentValue(String value) {
    public ContentValue {
        if (value == null) {
            throw new IllegalArgumentException("Content value must not be null");
        }
    }
}
