package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.CoffeePhotoStorageProperties;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.S3CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.GooglePlacePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3CoffeePhotoStorageTest {

	@Test
	void stores_photo_under_fragments_prefix_and_returns_signed_url() throws Exception {
		var properties = new CoffeePhotoStorageProperties();
		properties.setS3Bucket("anchor-assets-prod-851725375299");
		properties.setS3Prefix("/fragments/staging/coffees/");
		var s3Client = mock(S3Client.class);
		var presigner = mock(S3Presigner.class);
		var presignedRequest = mock(PresignedGetObjectRequest.class);
		when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
				.thenReturn(PutObjectResponse.builder().build());
		when(presignedRequest.url()).thenReturn(new URL("https://signed.fragments.test/photo.jpg"));
		when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
		var storage = new S3CoffeePhotoStorage(properties, s3Client, presigner);
		var coffeeId = new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

		var stored = storage.store(
				coffeeId,
				new GooglePlaceId("places/google-1"),
				new GooglePlacePhoto("places/google-1/photos/photo-1", "image/jpeg", "jpeg-bytes".getBytes()));

		assertThat(stored.photoId()).isNotNull();
		assertThat(stored.photoUri()).isEqualTo("https://signed.fragments.test/photo.jpg");
		var requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
		assertThat(requestCaptor.getValue().bucket()).isEqualTo("anchor-assets-prod-851725375299");
		assertThat(requestCaptor.getValue().key())
				.startsWith("fragments/staging/coffees/11111111-1111-1111-1111-111111111111/photos/")
				.endsWith(".jpg");
		assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/jpeg");
	}
}
