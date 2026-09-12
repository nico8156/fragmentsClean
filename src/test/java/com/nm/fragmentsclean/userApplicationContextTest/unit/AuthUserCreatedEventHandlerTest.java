package com.nm.fragmentsclean.userApplicationContextTest.unit;

import static org.assertj.core.api.Assertions.*;

import com.nm.fragmentsclean.platform.eventing.contracts.AuthUserCreatedIntegrationEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.AuthUserCreatedEventHandler;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class AuthUserCreatedEventHandlerTest {
  @Test
  void failed_persistence_is_not_acknowledged_and_redelivery_can_create_the_profile() {
    var repository = new FailingOnceAppUserRepository();
    var now = Instant.parse("2026-09-12T10:00:00Z");
    var handler = new AuthUserCreatedEventHandler(repository, () -> now);
    var event = new AuthUserCreatedIntegrationEvent(UUID.randomUUID(), UUID.randomUUID(),
        "GOOGLE", "provider-id", "test@example.invalid", true, "Camille", null, now);

    assertThatThrownBy(() -> handler.handle(event)).isSameAs(repository.failure);
    assertThat(repository.saved).isNull();

    handler.handle(event);
    handler.handle(event);
    assertThat(repository.saved.authUserId()).isEqualTo(event.authUserId());
    assertThat(repository.saved.displayName()).isEqualTo("Camille");
    assertThat(repository.successfulSaves).isEqualTo(1);
  }

  private static class FailingOnceAppUserRepository implements AppUserRepository {
    final DataIntegrityViolationException failure = new DataIntegrityViolationException("Concurrent write");
    boolean failNext = true;
    AppUser saved;
    int successfulSaves;
    public Optional<AppUser> findByAuthUserId(UUID id) { return findById(id); }
    public Optional<AppUser> findById(UUID id) { return Optional.ofNullable(saved); }
    public AppUser save(AppUser user) {
      if (failNext) { failNext = false; throw failure; }
      saved = user;
      successfulSaves++;
      return saved;
    }
  }
}
