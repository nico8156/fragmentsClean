package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.CoffeePhotoStorageProperties;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.S3CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.GooglePlacePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3CoffeePhotoStorageTest {

	@Test
	void stores_photo_under_fragments_prefix_and_returns_stable_s3_reference() {
		var properties = new CoffeePhotoStorageProperties();
		properties.setS3Bucket("anchor-assets-prod-851725375299");
		properties.setS3Prefix("/fragments/staging/coffees/");
		var s3Client = new RecordingS3Client();
		var storage = new S3CoffeePhotoStorage(properties, s3Client);
		var coffeeId = new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

		var stored = storage.store(
				coffeeId,
				new GooglePlaceId("places/google-1"),
				new GooglePlacePhoto("places/google-1/photos/photo-1", "image/jpeg", "jpeg-bytes".getBytes()));

		assertThat(stored.photoId()).isNotNull();
		assertThat(stored.photoUri())
				.startsWith("s3://anchor-assets-prod-851725375299/fragments/staging/coffees/11111111-1111-1111-1111-111111111111/photos/")
				.endsWith(".jpg");
		assertThat(s3Client.request.bucket()).isEqualTo("anchor-assets-prod-851725375299");
		assertThat(s3Client.request.key())
				.startsWith("fragments/staging/coffees/11111111-1111-1111-1111-111111111111/photos/")
				.endsWith(".jpg");
		assertThat(s3Client.request.contentType()).isEqualTo("image/jpeg");
	}

	private static class RecordingS3Client implements S3Client {
		private PutObjectRequest request;

		@Override
		public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody requestBody) {
			this.request = putObjectRequest;
			return PutObjectResponse.builder().build();
		}

		@Override
		public String serviceName() {
			return "s3";
		}

		@Override
		public void close() {
		}
	}
}
