package com.nm.fragmentsclean.sharedKernel.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.fake.FakeExperienceMediaRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMedia;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaStatus;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.CleanExperienceMediaObjects;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.CompleteExperienceMediaDeletion;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.PrivateImageStorageProperties;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.S3PrivateImageStore;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.SafeImageNormalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@EnabledIf("dockerAvailable")
class PrivateMediaCleanupLocalStackTest {
    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.4.0");
    private static final Instant NOW = Instant.parse("2026-09-12T10:00:00Z");

    @Test
    void abandoned_experience_upload_is_deleted_from_s3_before_domain_completion() {
        try (var localStack = localStack();
             var client = s3Client(localStack);
             var presigner = s3Presigner(localStack)) {
            String bucket = "fragments-private-media-test";
            String objectKey = "fragments/staging/private-media/pending/abandoned.jpg";
            client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(objectKey).build(),
                    RequestBody.fromBytes(new byte[] {1, 2, 3}));

            var properties = new PrivateImageStorageProperties();
            properties.setBucket(bucket);
            properties.setRegion(localStack.getRegion());
            properties.setEndpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3));
            var store = new S3PrivateImageStore(properties, client, presigner, new SafeImageNormalizer());
            var repository = new FakeExperienceMediaRepository();
            var media = ExperienceMedia.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), "image/jpeg", 3, objectKey, NOW.minus(Duration.ofDays(2)));
            repository.save(media);
            var completion = new CompleteExperienceMediaDeletion(
                    repository, new FakeDomainEventPublisher(), () -> NOW);

            new CleanExperienceMediaObjects(repository, store, completion, () -> NOW, Duration.ofHours(24)).run(10);

            assertThat(repository.inspect(media.id()).orElseThrow().status()).isEqualTo(ExperienceMediaStatus.DELETED);
            assertThatThrownBy(() -> client.headObject(
                    HeadObjectRequest.builder().bucket(bucket).key(objectKey).build()))
                    .isInstanceOf(S3Exception.class);
        }
    }

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static LocalStackContainer localStack() {
        var localStack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(LocalStackContainer.Service.S3);
        localStack.start();
        return localStack;
    }

    private static S3Client s3Client(LocalStackContainer localStack) {
        return S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(credentials(localStack))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private static S3Presigner s3Presigner(LocalStackContainer localStack) {
        return S3Presigner.builder()
                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(credentials(localStack))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    private static StaticCredentialsProvider credentials(LocalStackContainer localStack) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey()));
    }
}
