package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO;

public record Address(
        String line1,
        String city,
        String postalCode,
        String country // ex: "FR"
) {
    public Address {
        // on reste assez tolérant : null possible sur certains champs
        if (country != null && country.length() > 3) {
            throw new IllegalArgumentException("country should be ISO code or short");
        }
    }

    public static Address empty() {
        return new Address(null, null, null, null);
    }
}
