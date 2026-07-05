package com.nm.fragmentsclean.socialContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CurrentUserProvider;
import com.nm.fragmentsclean.socialContext.read.GetLikeStatusQueryHandler;
import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcLikeProjectionRepository;
import com.nm.fragmentsclean.socialContext.read.projections.GetLikeStatusQuery;
import com.nm.fragmentsclean.socialContext.read.projections.LikeStatusView;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetLikeStatusQueryHandlerTest {
	private static final UUID TARGET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Test
	void reads_like_status_from_projection_repository() {
		var projectionRepository = new RecordingLikeProjectionRepository();
		var dateTimeProvider = new DeterministicDateTimeProvider();
		var handler = new GetLikeStatusQueryHandler(
				projectionRepository,
				currentUserProvider(),
				dateTimeProvider);

		LikeStatusView view = handler.handle(new GetLikeStatusQuery(TARGET_ID));

		assertThat(projectionRepository.requestedTargetId).isEqualTo(TARGET_ID);
		assertThat(projectionRepository.requestedUserId).isEqualTo(USER_ID);
		assertThat(projectionRepository.requestedServerTime).isEqualTo(Instant.parse("2023-10-01T11:00:00Z"));
		assertThat(view.count()).isEqualTo(2L);
		assertThat(view.me()).isTrue();
		assertThat(view.version()).isEqualTo(5L);
	}

	private CurrentUserProvider currentUserProvider() {
		return new CurrentUserProvider() {
			@Override
			public UUID currentUserId() {
				return USER_ID;
			}

			@Override
			public String currentUserName() {
				return USER_ID.toString();
			}
		};
	}

	private static class RecordingLikeProjectionRepository extends JdbcLikeProjectionRepository {
		private UUID requestedTargetId;
		private UUID requestedUserId;
		private Instant requestedServerTime;

		private RecordingLikeProjectionRepository() {
			super(null);
		}

		@Override
		public LikeStatusView statusFor(UUID targetId, UUID currentUserId, Instant serverTime) {
			this.requestedTargetId = targetId;
			this.requestedUserId = currentUserId;
			this.requestedServerTime = serverTime;
			return new LikeStatusView(2L, true, 5L, serverTime.toString());
		}
	}
}
