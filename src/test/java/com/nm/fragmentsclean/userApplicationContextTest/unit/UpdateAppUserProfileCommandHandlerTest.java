package com.nm.fragmentsclean.userApplicationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.UpdateAppUserProfileCommand;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.UpdateAppUserProfileCommandHandler;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateAppUserProfileCommandHandlerTest {
  private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID COMMAND_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private FakeAppUserRepository repository;
  private FakeDomainEventPublisher events;
  private UpdateAppUserProfileCommandHandler handler;

  @BeforeEach
  void setUp() {
    repository = new FakeAppUserRepository(existingUser());
    events = new FakeDomainEventPublisher();
    handler =
        new UpdateAppUserProfileCommandHandler(
            repository, events, new DeterministicDateTimeProvider());
  }

  @Test
  void updates_the_owner_profile_and_publishes_the_domain_event() {
    handler.execute(new UpdateAppUserProfileCommand(COMMAND_ID, USER_ID, "Nicolas Maldiney"));

    assertThat(repository.saved).isNotNull();
    assertThat(repository.saved.displayName()).isEqualTo("Nicolas Maldiney");
    assertThat(events.published).singleElement().isInstanceOf(AppUserProfileUpdatedEvent.class);
  }

  @Test
  void an_already_satisfied_update_succeeds_without_persistence_or_event() {
    handler.execute(new UpdateAppUserProfileCommand(COMMAND_ID, USER_ID, "Nicolas"));

    assertThat(repository.saved).isNull();
    assertThat(events.published).isEmpty();
  }

  @Test
  void rejects_an_invalid_name_with_a_stable_business_code() {
    assertThatThrownBy(
            () -> handler.execute(new UpdateAppUserProfileCommand(COMMAND_ID, USER_ID, "x")))
        .isInstanceOfSatisfying(
            BusinessCommandRejectedException.class,
            rejection -> assertThat(rejection.rejectionCode()).isEqualTo("INVALID_DISPLAY_NAME"));
  }

  @Test
  void rejects_an_unknown_authenticated_user() {
    repository.user = null;

    assertThatThrownBy(
            () ->
                handler.execute(
                    new UpdateAppUserProfileCommand(COMMAND_ID, USER_ID, "Nicolas Maldiney")))
        .isInstanceOf(BusinessCommandRejectedException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void rejects_a_profile_update_after_account_deletion_was_requested() {
    repository.user.requestAccountDeletion(
        UUID.randomUUID(), Instant.parse("2026-09-11T11:00:00Z"));

    assertThatThrownBy(
            () ->
                handler.execute(
                    new UpdateAppUserProfileCommand(COMMAND_ID, USER_ID, "Nicolas Maldiney")))
        .isInstanceOfSatisfying(
            BusinessCommandRejectedException.class,
            rejection -> assertThat(rejection.rejectionCode()).isEqualTo("ACCOUNT_NOT_ACTIVE"));
  }

  private static AppUser existingUser() {
    var at = Instant.parse("2026-09-11T10:00:00Z");
    return new AppUser(USER_ID, USER_ID, "Nicolas", null, at, at, 0L);
  }

  private static final class FakeAppUserRepository implements AppUserRepository {
    private AppUser user;
    private AppUser saved;

    private FakeAppUserRepository(AppUser user) {
      this.user = user;
    }

    @Override
    public Optional<AppUser> findByAuthUserId(UUID authUserId) {
      return Optional.ofNullable(user).filter(value -> value.authUserId().equals(authUserId));
    }

    @Override
    public Optional<AppUser> findById(UUID userId) {
      return Optional.ofNullable(user).filter(value -> value.id().equals(userId));
    }

    @Override
    public AppUser save(AppUser user) {
      this.saved = user;
      return user;
    }
  }
}
