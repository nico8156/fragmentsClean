package com.nm.fragmentsclean.userApplicationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AccountDeletionProcessRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserDeletionRequestedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.processManagers.AccountDeletionProcess;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.RequestAccountDeletionCommand;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.RequestAccountDeletionCommandHandler;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestAccountDeletionCommandHandlerTest {
  private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID COMMAND_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

  @Test
  void records_the_process_anonymizes_the_public_profile_and_publishes_the_request() {
    var userRepository = new FakeUserRepository(activeUser());
    var processRepository = new FakeProcessRepository();
    var events = new FakeDomainEventPublisher();
    var handler =
        new RequestAccountDeletionCommandHandler(
            userRepository, processRepository, events, new DeterministicDateTimeProvider());

    handler.execute(new RequestAccountDeletionCommand(COMMAND_ID, USER_ID));

    assertThat(userRepository.user.displayName()).isEqualTo("Compte supprimé");
    assertThat(userRepository.user.avatarUrl()).isNull();
    assertThat(processRepository.process.requestId()).isEqualTo(COMMAND_ID);
    assertThat(events.published).singleElement().isInstanceOf(AppUserDeletionRequestedEvent.class);
  }

  private static AppUser activeUser() {
    var at = Instant.parse("2026-09-11T09:00:00Z");
    return new AppUser(USER_ID, USER_ID, "Nicolas", "avatar", at, at, 0L);
  }

  private static final class FakeUserRepository implements AppUserRepository {
    private AppUser user;

    private FakeUserRepository(AppUser user) {
      this.user = user;
    }

    @Override
    public Optional<AppUser> findByAuthUserId(UUID id) {
      return findById(id);
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
      return Optional.ofNullable(user).filter(value -> value.id().equals(id));
    }

    @Override
    public AppUser save(AppUser value) {
      user = value;
      return value;
    }
  }

  private static final class FakeProcessRepository implements AccountDeletionProcessRepository {
    private AccountDeletionProcess process;

    @Override
    public Optional<AccountDeletionProcess> findByUserId(UUID userId) {
      return Optional.ofNullable(process).filter(value -> value.userId().equals(userId));
    }

    @Override
    public Optional<AccountDeletionProcess> findByRequestId(UUID requestId) {
      return Optional.ofNullable(process).filter(value -> value.requestId().equals(requestId));
    }

    @Override
    public AccountDeletionProcess save(AccountDeletionProcess value) {
      process = value;
      return value;
    }
  }
}
