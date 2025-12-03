package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;

public record PhoneNumber(String value) {
    public PhoneNumber {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException("phone number cannot be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
