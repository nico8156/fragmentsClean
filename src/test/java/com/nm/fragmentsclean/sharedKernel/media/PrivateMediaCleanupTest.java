package com.nm.fragmentsclean.sharedKernel.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.fake.FakeExperienceMediaRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMedia;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaStatus;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.CleanExperienceMediaObjects;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.CompleteExperienceMediaDeletion;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore;
import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.fake.FakeAvatarMediaRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMedia;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMediaStatus;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.CleanAvatarMediaObjects;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.CompleteAvatarMediaDeletion;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PrivateMediaCleanupTest {
  private static final Instant NOW = Instant.parse("2026-09-11T10:00:00Z");

  @Test
  void experience_cleanup_is_retryable_when_object_storage_temporarily_fails() {
    var repository = new FakeExperienceMediaRepository();
    var item = ExperienceMedia.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "image/jpeg", 128, "pending/key", NOW.minus(Duration.ofDays(2)));
    repository.save(item);
    var store = new FailingOnceStore();
    var completion = new CompleteExperienceMediaDeletion(repository, new FakeDomainEventPublisher(), () -> NOW);
    var cleanup = new CleanExperienceMediaObjects(repository, store, completion, () -> NOW, Duration.ofHours(24));

    assertThatThrownBy(() -> cleanup.run(10)).isInstanceOf(IllegalStateException.class);
    assertThat(repository.inspect(item.id()).orElseThrow().status()).isEqualTo(ExperienceMediaStatus.PENDING);
    cleanup.run(10);

    assertThat(repository.inspect(item.id()).orElseThrow().status()).isEqualTo(ExperienceMediaStatus.DELETED);
  }

  @Test
  void avatar_cleanup_marks_an_abandoned_upload_deleted() {
    var repository = new FakeAvatarMediaRepository();
    var item = AvatarMedia.pending(UUID.randomUUID(), UUID.randomUUID(), "image/png", 256,
        "pending/avatar", NOW.minus(Duration.ofDays(2)));
    repository.save(item);
    var completion = new CompleteAvatarMediaDeletion(repository, () -> NOW);
    new CleanAvatarMediaObjects(repository, new FailingOnceStore(false), completion, () -> NOW,
        Duration.ofHours(24)).run(10);

    assertThat(repository.inspect(item.id()).orElseThrow().status()).isEqualTo(AvatarMediaStatus.DELETED);
  }

  private static final class FailingOnceStore implements PrivateImageStore {
    private boolean fail;
    private FailingOnceStore() { this(true); }
    private FailingOnceStore(boolean fail) { this.fail = fail; }
    public UploadTarget presignUpload(String key,String type,Duration ttl,Instant at){return new UploadTarget(URI.create("https://upload.test"),"PUT",Map.of(),at.plus(ttl));}
    public ProcessedImage normalize(String pending,String target,String type,ImageRules rules){throw new UnsupportedOperationException();}
    public URI presignDownload(String key,Duration ttl){return URI.create("https://download.test");}
    public void delete(String key){if(fail){fail=false;throw new IllegalStateException("S3 unavailable");}}
  }
}
