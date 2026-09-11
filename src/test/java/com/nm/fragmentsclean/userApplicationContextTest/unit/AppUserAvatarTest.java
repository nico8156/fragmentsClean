package com.nm.fragmentsclean.userApplicationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppUserAvatarTest {
  @Test
  void replaces_then_removes_owned_avatar_and_emits_profile_facts() {
    Instant now = Instant.parse("2026-09-11T18:00:00Z");
    UUID userId = UUID.randomUUID();
    var user = new AppUser(userId, userId, "Ada", null, now, now, 0);

    assertThat(user.replaceAvatar("media:avatar:avatars/a.jpg", now.plusSeconds(1))).isTrue();
    assertThat(user.avatarUrl()).isEqualTo("media:avatar:avatars/a.jpg");
    assertThat(user.removeAvatar(now.plusSeconds(2))).isTrue();
    assertThat(user.avatarUrl()).isNull();
    assertThat(user.domainEvents()).hasSize(2);
  }
}
