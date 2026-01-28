package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.bootstrap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AddressParser {

	private AddressParser() {
	}

	// Ex: "2 All. Joseph Gemain, 35000 Rennes, France"
	private static final Pattern POSTAL_CITY = Pattern.compile("(?:(\\d{4,6})\\s+(.+))");

	static CoffeeAddressParts parse(String formattedAddress) {
		if (formattedAddress == null || formattedAddress.isBlank()) {
			return new CoffeeAddressParts(null, null, null, null);
		}

		var parts = formattedAddress.split(",");
		for (int i = 0; i < parts.length; i++)
			parts[i] = parts[i].trim();

		String line1 = parts.length >= 1 ? parts[0] : null;
		String country = parts.length >= 1 ? parts[parts.length - 1] : null;

		String postalCode = null;
		String city = null;

		if (parts.length >= 2) {
			String cityPostal = parts[parts.length - 2];
			Matcher m = POSTAL_CITY.matcher(cityPostal);
			if (m.matches()) {
				postalCode = m.group(1);
				city = m.group(2);
			} else {
				// fallback: on met tout en city si pas de postal code
				city = cityPostal;
			}
		}

		return new CoffeeAddressParts(line1, city, postalCode, country);
	}
}
