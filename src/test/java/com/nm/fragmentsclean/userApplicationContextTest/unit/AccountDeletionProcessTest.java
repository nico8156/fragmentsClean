package com.nm.fragmentsclean.userApplicationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.processManagers.AccountDeletionProcess;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountDeletionProcessTest {
  private static final UUID REQUEST_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final Instant REQUESTED_AT = Instant.parse("2026-09-11T10:00:00Z");
  private static final Instant COMPLETED_AT = Instant.parse("2026-09-11T10:05:00Z");

  @Test
  void completes_only_after_each_owning_context_has_erased_its_data() {
    var process = AccountDeletionProcess.start(REQUEST_ID, USER_ID, REQUESTED_AT);

    assertThat(process.acknowledge(AccountDeletionProcess.Context.USER_APPLICATION, COMPLETED_AT))
        .isTrue();
    assertThat(process.acknowledge(AccountDeletionProcess.Context.AUTHENTICATION, COMPLETED_AT))
        .isTrue();
    assertThat(process.acknowledge(AccountDeletionProcess.Context.SOCIAL, COMPLETED_AT)).isTrue();
    assertThat(process.status()).isEqualTo(AccountDeletionProcess.Status.IN_PROGRESS);
    assertThat(process.acknowledge(AccountDeletionProcess.Context.TICKET, COMPLETED_AT)).isTrue();

    assertThat(process.status()).isEqualTo(AccountDeletionProcess.Status.COMPLETED);
    assertThat(process.completedAt()).isEqualTo(COMPLETED_AT);
  }

  @Test
  void duplicate_acknowledgements_are_idempotent() {
    var process = AccountDeletionProcess.start(REQUEST_ID, USER_ID, REQUESTED_AT);
    process.acknowledge(AccountDeletionProcess.Context.SOCIAL, COMPLETED_AT);

    assertThat(process.acknowledge(AccountDeletionProcess.Context.SOCIAL, COMPLETED_AT)).isFalse();
    assertThat(process.version()).isEqualTo(1L);
  }
}
