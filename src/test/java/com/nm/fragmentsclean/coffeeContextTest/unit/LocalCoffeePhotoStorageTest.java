package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.CoffeePhotoStorageProperties;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.LocalCoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.GooglePlacePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;

class LocalCoffeePhotoStorageTest {

	@TempDir
	java.nio.file.Path tempDir;

	@Test
	void stores_photo_bytes_and_returns_public_uri() throws Exception {
		var properties = new CoffeePhotoStorageProperties();
		properties.setDirectory(tempDir);
		properties.setPublicBaseUrl("https://fragments.test");
		var storage = new LocalCoffeePhotoStorage(properties);
		var coffeeId = new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
		var photo = new GooglePlacePhoto("places/google-1/photos/photo-1", "image/jpeg", "jpeg-bytes".getBytes());

		var stored = storage.store(coffeeId, new GooglePlaceId("places/google-1"), photo);

		assertThat(stored.photoId()).isNotNull();
		assertThat(stored.photoUri()).startsWith("https://fragments.test/api/coffees/photo-assets/");
		assertThat(stored.photoUri()).endsWith(".jpg");
		var fileName = stored.photoUri().substring(stored.photoUri().lastIndexOf('/') + 1);
		assertThat(Files.readAllBytes(tempDir.resolve(fileName))).isEqualTo("jpeg-bytes".getBytes());
	}

	@Test
	void uses_relative_public_uri_when_base_url_is_not_configured() {
		var properties = new CoffeePhotoStorageProperties();
		properties.setDirectory(tempDir);
		var storage = new LocalCoffeePhotoStorage(properties);

		var stored = storage.store(
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				new GooglePlaceId("places/google-1"),
				new GooglePlacePhoto("places/google-1/photos/photo-1", "image/png", "png-bytes".getBytes()));

		assertThat(stored.photoUri()).startsWith("/api/coffees/photo-assets/");
		assertThat(stored.photoUri()).endsWith(".png");
	}
}
