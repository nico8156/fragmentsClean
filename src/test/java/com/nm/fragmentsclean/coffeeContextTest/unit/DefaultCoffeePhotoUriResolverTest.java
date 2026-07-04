package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.storage.DefaultCoffeePhotoUriResolver;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.CoffeePhotoStorageProperties;

import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class DefaultCoffeePhotoUriResolverTest {

	@Test
	void leaves_non_s3_photo_uri_unchanged() {
		var resolver = new DefaultCoffeePhotoUriResolver(new CoffeePhotoStorageProperties(), null);

		assertThat(resolver.resolve("https://images.example/photo.jpg"))
				.isEqualTo("https://images.example/photo.jpg");
	}

	@Test
	void resolves_s3_reference_to_fresh_signed_url() throws Exception {
		var properties = new CoffeePhotoStorageProperties();
		var presigner = new RecordingS3Presigner(new URL("https://signed.fragments.test/photo.jpg"));
		var resolver = new DefaultCoffeePhotoUriResolver(properties, presigner.proxy);

		var resolved = resolver.resolve("s3://anchor-assets-prod-851725375299/fragments/staging/coffees/coffee-1/photos/photo-1.jpg");

		assertThat(resolved).isEqualTo("https://signed.fragments.test/photo.jpg");
		assertThat(presigner.request.getObjectRequest().bucket()).isEqualTo("anchor-assets-prod-851725375299");
		assertThat(presigner.request.getObjectRequest().key())
				.isEqualTo("fragments/staging/coffees/coffee-1/photos/photo-1.jpg");
	}

	private static class RecordingS3Presigner {
		private GetObjectPresignRequest request;
		private final S3Presigner proxy;

		private RecordingS3Presigner(URL signedUrl) {
			this.proxy = (S3Presigner) Proxy.newProxyInstance(
					S3Presigner.class.getClassLoader(),
					new Class<?>[] { S3Presigner.class },
					(_proxy, method, args) -> {
						if ("presignGetObject".equals(method.getName())) {
							this.request = (GetObjectPresignRequest) args[0];
							return PresignedGetObjectRequest.builder()
									.expiration(Instant.parse("2026-07-03T10:00:00Z"))
									.isBrowserExecutable(true)
									.signedHeaders(Map.of("host", List.of("signed")))
									.httpRequest(SdkHttpFullRequest.builder()
											.method(SdkHttpMethod.GET)
											.uri(java.net.URI.create(signedUrl.toString()))
											.build())
									.build();
						}
						if ("close".equals(method.getName())) {
							return null;
						}
						if ("toString".equals(method.getName())) {
							return "RecordingS3Presigner";
						}
						throw new UnsupportedOperationException(method.getName());
					});
		}
	}
}
