package com.nm.fragmentsclean.adminImportContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminUserAccess;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.GrantAdminUser;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RevokeAdminUser;

class AdminUserAccessUseCasesTest {
	private static final Instant NOW = Instant.parse("2026-08-25T10:00:00Z");

	@Test
	void grants_admin_by_existing_email() {
		var userId = UUID.randomUUID();
		var grantedBy = UUID.randomUUID();
		var repository = new FakeRepository();
		repository.emailUserId = userId;

		var result = new GrantAdminUser(repository).execute(null, "Admin@Example.test", grantedBy, NOW);

		assertThat(result.userId()).isEqualTo(userId);
		assertThat(repository.grantedBy).isEqualTo(grantedBy);
	}

	@Test
	void refuses_to_revoke_the_last_persisted_admin() {
		var repository = new FakeRepository();
		repository.documents.put(UUID.randomUUID(), new AdminUserAccess(UUID.randomUUID(), "a@test", NOW, null, false));

		assertThatThrownBy(() -> new RevokeAdminUser(repository).execute(UUID.randomUUID()))
				.isInstanceOf(IllegalStateException.class);
	}

	private static class FakeRepository implements AdminUserAccessRepository {
		private final Map<UUID, AdminUserAccess> documents = new HashMap<>();
		private UUID emailUserId;
		private UUID grantedBy;

		@Override public List<AdminUserAccess> list() { return documents.values().stream().toList(); }
		@Override public Optional<AdminUserAccess> findByUserId(UUID userId) {
			return Optional.ofNullable(documents.get(userId));
		}
		@Override public Optional<UUID> findAuthUserIdByEmail(String email) { return Optional.ofNullable(emailUserId); }
		@Override public boolean isAllowed(UUID userId, String email) { return documents.containsKey(userId); }
		@Override public void grant(UUID userId, UUID by, Instant at) {
			grantedBy = by;
			documents.put(userId, new AdminUserAccess(userId, "admin@example.test", at, by, false));
		}
		@Override public void revoke(UUID userId) { documents.remove(userId); }
		@Override public long count() { return documents.size(); }
	}
}
