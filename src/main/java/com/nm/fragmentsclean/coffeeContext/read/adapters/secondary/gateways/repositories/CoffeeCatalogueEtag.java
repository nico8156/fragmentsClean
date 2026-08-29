package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;

final class CoffeeCatalogueEtag {
	private CoffeeCatalogueEtag() { }

	static String from(String search, List<CoffeeSummaryView> items) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			update(digest, search);
			for (CoffeeSummaryView item : items) {
				update(digest, item.id().toString());
				update(digest, Long.toString(item.version()));
				update(digest, item.updatedAt().toString());
			}
			return "\"coffee-catalogue-" + HexFormat.of().formatHex(digest.digest(), 0, 12) + "\"";
		} catch (NoSuchAlgorithmException impossible) {
			throw new IllegalStateException("SHA-256 is unavailable", impossible);
		}
	}

	static String fromRevisionRows(String search, List<String> revisionRows) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			update(digest, search);
			for (String row : revisionRows) update(digest, row);
			return "\"coffee-catalogue-" + HexFormat.of().formatHex(digest.digest(), 0, 12) + "\"";
		} catch (NoSuchAlgorithmException impossible) {
			throw new IllegalStateException("SHA-256 is unavailable", impossible);
		}
	}

	private static void update(MessageDigest digest, String value) {
		digest.update((value == null ? "<null>" : value).getBytes(StandardCharsets.UTF_8));
		digest.update((byte) 0);
	}
}
