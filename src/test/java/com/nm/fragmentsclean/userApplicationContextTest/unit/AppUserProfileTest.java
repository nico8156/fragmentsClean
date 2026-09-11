package com.nm.fragmentsclean.userApplicationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.InactiveAppUserException;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.InvalidDisplayNameException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AppUserProfileTest {
  private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final Instant CREATED_AT = Instant.parse("2026-09-11T10:00:00Z");
  private static final Instant UPDATED_AT = Instant.parse("2026-09-11T11:00:00Z");

  @Test
  void updates_the_public_display_name_and_records_a_domain_fact() {
    var user = existingUser("Nicolas");

    assertThat(user.updateDisplayName("  Nicolas   Maldiney  ", UPDATED_AT)).isTrue();

    assertThat(user.displayName()).isEqualTo("Nicolas Maldiney");
    assertThat(user.updatedAt()).isEqualTo(UPDATED_AT);
    assertThat(user.version()).isEqualTo(4L);
    assertThat(user.domainEvents())
        .containsExactly(
            new AppUserProfileUpdatedEvent(
                ((AppUserProfileUpdatedEvent) user.domainEvents().getFirst()).eventId(),
                USER_ID,
                "Nicolas Maldiney",
                "https://example.test/avatar.jpg",
                4L,
                UPDATED_AT));
  }

  @Test
  void an_identical_normalized_name_is_idempotent() {
    var user = existingUser("Nicolas Maldiney");

    assertThat(user.updateDisplayName(" Nicolas   Maldiney ", UPDATED_AT)).isFalse();

    assertThat(user.version()).isEqualTo(3L);
    assertThat(user.domainEvents()).isEmpty();
  }

  @Test
  void rejects_names_that_are_empty_too_short_too_long_or_contain_control_characters() {
    var user = existingUser("Nicolas");

    assertThatThrownBy(() -> user.updateDisplayName(" ", UPDATED_AT))
        .isInstanceOf(InvalidDisplayNameException.class);
    assertThatThrownBy(() -> user.updateDisplayName("N", UPDATED_AT))
        .isInstanceOf(InvalidDisplayNameException.class);
    assertThatThrownBy(() -> user.updateDisplayName("x".repeat(51), UPDATED_AT))
        .isInstanceOf(InvalidDisplayNameException.class);
    assertThatThrownBy(() -> user.updateDisplayName("Nico\nlas", UPDATED_AT))
        .isInstanceOf(InvalidDisplayNameException.class);
  }

  @Test
  void refuses_profile_mutation_after_account_deletion_was_requested() {
    var user = existingUser("Nicolas");
    user.requestAccountDeletion(UUID.randomUUID(), UPDATED_AT);

    assertThatThrownBy(() -> user.updateDisplayName("Nicolas Maldiney", UPDATED_AT.plusSeconds(1)))
        .isInstanceOf(InactiveAppUserException.class);
  }

  private AppUser existingUser(String displayName) {
    return new AppUser(
        USER_ID,
        USER_ID,
        displayName,
        "https://example.test/avatar.jpg",
        CREATED_AT,
        CREATED_AT,
        3L);
  }
}
