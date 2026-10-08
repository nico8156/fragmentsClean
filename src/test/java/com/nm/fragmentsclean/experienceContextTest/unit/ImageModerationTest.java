package com.nm.fragmentsclean.experienceContextTest.unit;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMedia;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMedia;
class ImageModerationTest {
  private final Instant now=Instant.parse("2026-10-08T00:00:00Z");
  @Test void flagged_experience_photo_is_stored_but_not_available() {
    var item=ExperienceMedia.pending(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"image/jpeg",100,"pending",now);
    item.confirmForReview("private.jpg","image/jpeg",80,100,100,"hash",now);
    assertThat(item.snapshot().status().name()).isEqualTo("REVIEW_REQUIRED");
    assertThat(item.snapshot().objectKey()).isEqualTo("private.jpg");
  }
  @Test void flagged_avatar_is_stored_but_not_available() {
    var item=AvatarMedia.pending(UUID.randomUUID(),UUID.randomUUID(),"image/jpeg",100,"pending",now);
    item.confirmForReview("private.jpg","image/jpeg",80,100,100,"hash",now);
    assertThat(item.snapshot().status().name()).isEqualTo("REVIEW_REQUIRED");
  }
  @Test void human_approval_is_idempotent_and_rejection_cannot_be_reversed_by_upload_retry() {
    var item=ExperienceMedia.pending(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"image/jpeg",100,"pending",now);
    item.confirmForReview("private.jpg","image/jpeg",80,100,100,"hash",now);
    assertThat(item.confirmForReview("private.jpg","image/jpeg",80,100,100,"hash",now)).isFalse();
    assertThat(item.review(true,now)).isTrue();
    assertThat(item.review(true,now)).isFalse();
    assertThat(item.snapshot().status().name()).isEqualTo("AVAILABLE");
    var rejected=AvatarMedia.pending(UUID.randomUUID(),UUID.randomUUID(),"image/jpeg",100,"pending",now);
    rejected.confirmForReview("private.jpg","image/jpeg",80,100,100,"hash",now);
    rejected.review(false,now);
    assertThat(rejected.snapshot().status().name()).isEqualTo("REJECTED");
    assertThatThrownBy(()->rejected.confirm("private.jpg","image/jpeg",80,100,100,"hash",now)).isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class);
    assertThatThrownBy(()->rejected.review(true,now)).isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class);
  }
  @Test void deletion_while_waiting_cannot_be_undone_by_review() {
    var item=ExperienceMedia.pending(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"image/jpeg",100,"pending",now);
    item.confirmForReview("private.jpg","image/jpeg",80,100,100,"hash",now);
    item.requestDeletion(now);
    assertThatThrownBy(()->item.review(true,now)).isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class);
  }
}
