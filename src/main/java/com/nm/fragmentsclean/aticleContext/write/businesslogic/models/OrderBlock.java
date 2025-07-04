package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

public record OrderBlock (int value) {
    public OrderBlock {
        if (value < 0) {
            throw new IllegalArgumentException("Order must be positive");
        }
    }
}
