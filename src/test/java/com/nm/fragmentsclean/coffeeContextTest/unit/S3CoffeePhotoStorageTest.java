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

    @Test void deletes_exactly_the_managed_object_without_listing_the_coffee_prefix(){
        var properties=new CoffeePhotoStorageProperties();properties.setS3Bucket("coffee-media");properties.setS3Prefix("/managed/coffees/");var client=org.mockito.Mockito.mock(S3Client.class);var storage=new S3CoffeePhotoStorage(properties,client);var coffee=new CoffeeId(UUID.randomUUID());var photo=UUID.randomUUID();String key="managed/coffees/"+coffee.value()+"/photos/"+photo+".png";String reference="s3://coffee-media/"+key;
        assertThat(storage.canDeletePhoto(coffee,photo,reference)).isTrue();storage.deletePhoto(coffee,photo,reference);
        org.mockito.Mockito.verify(client).deleteObject(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.builder().bucket("coffee-media").key(key).build());org.mockito.Mockito.verifyNoMoreInteractions(client);
    }
    @Test void targeted_s3_delete_rejects_foreign_bucket_prefix_owner_id_and_key_suffix(){
        var props=new CoffeePhotoStorageProperties();props.setS3Bucket("coffee-media");props.setS3Prefix("managed/coffees");var client=org.mockito.Mockito.mock(S3Client.class);var storage=new S3CoffeePhotoStorage(props,client);var coffee=new CoffeeId(UUID.randomUUID());var id=UUID.randomUUID();String valid="s3://coffee-media/managed/coffees/"+coffee.value()+"/photos/"+id+".png";
        for(String bad:java.util.List.of(valid.replace("coffee-media","foreign"),valid.replace("managed/coffees","managed/coffees-foreign"),valid.replace(coffee.value().toString(),UUID.randomUUID().toString()),valid.replace(id.toString(),UUID.randomUUID().toString()),valid+"?key=other",valid+"/../other",valid.replace(".png",".exe"))){assertThat(storage.canDeletePhoto(coffee,id,bad)).isFalse();org.assertj.core.api.Assertions.assertThatThrownBy(()->storage.deletePhoto(coffee,id,bad)).isInstanceOf(com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.CoffeePhotoStorageException.class);}
        org.mockito.Mockito.verifyNoInteractions(client);
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
