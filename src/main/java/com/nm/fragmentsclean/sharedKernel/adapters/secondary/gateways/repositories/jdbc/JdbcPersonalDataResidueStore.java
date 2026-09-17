package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.PersonalDataResidueStore;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Technical retention cleanup; it owns transport/projection copies, never business tables. */
public final class JdbcPersonalDataResidueStore implements PersonalDataResidueStore {
  private final JdbcTemplate jdbc;

  public JdbcPersonalDataResidueStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void purge(UUID userId, UUID authUserId) {
    // Inbox rows contain transport identifiers and processing state, not message payloads. Keeping
    // them preserves duplicate suppression and, critically, never deletes the row currently leased
    // by the account.data_erased consumer.
    jdbc.update(
        "DELETE FROM outbox_events WHERE payload_json LIKE '%' || ? || '%' OR payload_json LIKE '%' || ? || '%'",
        userId.toString(), authUserId.toString());
    jdbc.update(
        "DELETE FROM projection_sync_events WHERE payload_json::text LIKE '%' || ? || '%' OR payload_json::text LIKE '%' || ? || '%'",
        userId.toString(), authUserId.toString());
  }
}
