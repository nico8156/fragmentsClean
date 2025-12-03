package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;

public record WebsiteUrl(String value) {
    public WebsiteUrl {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException("website url cannot be blank");
        }
        // tu peux plus tard ajouter une mini validation URL si tu veux
    }

    @Override
    public String toString() {
        return value;
    }
}
