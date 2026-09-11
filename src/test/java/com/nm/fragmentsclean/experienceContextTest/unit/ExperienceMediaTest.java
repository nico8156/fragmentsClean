package com.nm.fragmentsclean.experienceContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMedia;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaStatus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExperienceMediaTest {
  private static final Instant NOW = Instant.parse("2026-09-11T18:00:00Z");

  @Test
  void pending_media_becomes_available_only_with_normalized_server_metadata() {
    var media = ExperienceMedia.pending(
        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
        "image/png", 1234, "pending/key", NOW);

    media.confirm("final/key.jpg", "image/jpeg", 900, 1200, 800, "abc", NOW.plusSeconds(1));

    assertThat(media.snapshot().status()).isEqualTo(ExperienceMediaStatus.AVAILABLE);
    assertThat(media.snapshot().objectKey()).isEqualTo("final/key.jpg");
  }

  @Test
  void another_user_cannot_delete_media() {
    UUID owner = UUID.randomUUID();
    var media = ExperienceMedia.pending(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), owner,
        "image/jpeg", 1234, "pending/key", NOW);

    assertThatThrownBy(() -> media.requestDeletion(UUID.randomUUID(), NOW))
        .isInstanceOf(BusinessCommandRejectedException.class)
        .extracting("rejectionCode")
        .isEqualTo("EXPERIENCE_MEDIA_FORBIDDEN");
  }
}
