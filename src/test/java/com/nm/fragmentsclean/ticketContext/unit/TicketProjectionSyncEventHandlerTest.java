package com.nm.fragmentsclean.ticketContext.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcTicketStatusProjectionRepository;
import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcUserEntitlementsProjectionRepository;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerificationCompletedEventHandler;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketProjectionSyncEventHandlerTest {
	private static final UUID COMMAND_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID TICKET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final Instant NOW = Instant.parse("2023-10-01T11:00:00Z");
	private static final Instant CLIENT_AT = Instant.parse("2023-10-01T09:59:00Z");

	@Test
	void accepted_projection_publishes_ticket_projection_updated_after_projection_update() {
		var repository = new RecordingTicketProjectionRepository();
		var publisher = new RecordingProjectionSyncPublisher(repository.operations);
		var handler = new TicketVerifyAcceptedEventHandler(repository, publisher);

		handler.handle(new TicketVerifyAcceptedEvent(
				UUID.fromString("44444444-4444-4444-4444-444444444444"),
				COMMAND_ID,
				TICKET_ID,
				USER_ID,
				"CAFE\nTOTAL 4,00 EUR",
				"s3://bucket/tickets/111.png",
				Ticket.TicketStatus.ANALYZING.name(),
				0L,
				NOW,
				CLIENT_AT));

		assertThat(repository.operations).containsExactly("projection", "sync");
		assertThat(publisher.events).hasSize(1);
		ProjectionSyncEvent event = publisher.events.getFirst();
		assertThat(event.eventName()).isEqualTo("projection.updated");
		assertThat(event.projection()).isEqualTo("tickets");
		assertThat(event.scope()).isEqualTo("entity");
		assertThat(event.entityId()).isEqualTo(TICKET_ID.toString());
		assertThat(event.version()).isZero();
		assertThat(event.changedAt()).isEqualTo(NOW);
		assertThat(event.hints()).containsExactly("status", "analyzing");
	}

	@Test
	void completed_projection_publishes_ticket_projection_updated_after_projection_update() {
		var repository = new RecordingTicketProjectionRepository();
		var entitlementsRepository = new RecordingUserEntitlementsProjectionRepository(repository.operations);
		var publisher = new RecordingProjectionSyncPublisher(repository.operations);
		var handler = new TicketVerificationCompletedEventHandler(repository, entitlementsRepository, publisher);

		handler.handle(new TicketVerificationCompletedEvent(
				UUID.fromString("55555555-5555-5555-5555-555555555555"),
				COMMAND_ID,
				TICKET_ID,
				USER_ID,
				TicketVerificationCompletedEvent.Outcome.APPROVED,
				1L,
				NOW,
				CLIENT_AT,
				new TicketVerificationCompletedEvent.Approved(
						400,
						"EUR",
						null,
						"CAFE",
						null,
						null,
						List.of()),
				null,
				"ticketEngine",
				"tv:ok"));

		assertThat(repository.operations).containsExactly("projection", "sync", "entitlementsProjection", "sync");
		assertThat(publisher.events).hasSize(2);
		ProjectionSyncEvent event = publisher.events.getFirst();
		assertThat(event.eventName()).isEqualTo("projection.updated");
		assertThat(event.projection()).isEqualTo("tickets");
		assertThat(event.scope()).isEqualTo("entity");
		assertThat(event.entityId()).isEqualTo(TICKET_ID.toString());
		assertThat(event.version()).isEqualTo(1L);
		assertThat(event.changedAt()).isEqualTo(NOW);
		assertThat(event.hints()).containsExactly("status", "approved");

		ProjectionSyncEvent entitlementsEvent = publisher.events.get(1);
		assertThat(entitlementsEvent.eventName()).isEqualTo("projection.updated");
		assertThat(entitlementsEvent.projection()).isEqualTo("entitlements");
		assertThat(entitlementsEvent.scope()).isEqualTo("user");
		assertThat(entitlementsEvent.entityId()).isEqualTo(USER_ID.toString());
		assertThat(entitlementsEvent.version()).isEqualTo(1L);
		assertThat(entitlementsEvent.changedAt()).isEqualTo(NOW);
		assertThat(entitlementsEvent.hints()).containsExactly("confirmedTickets");
	}

	private static class RecordingTicketProjectionRepository extends JdbcTicketStatusProjectionRepository {
		private final List<String> operations = new ArrayList<>();

		private RecordingTicketProjectionRepository() {
			super(null);
		}

		@Override
		public void applyAnalyzing(TicketVerifyAcceptedEvent evt) {
			operations.add("projection");
		}

		@Override
		public void applyCompleted(TicketVerificationCompletedEvent evt) {
			operations.add("projection");
		}
	}

	private static class RecordingUserEntitlementsProjectionRepository extends JdbcUserEntitlementsProjectionRepository {
		private final List<String> operations;

		private RecordingUserEntitlementsProjectionRepository(List<String> operations) {
			super(null);
			this.operations = operations;
		}

		@Override
		public com.nm.fragmentsclean.ticketContext.read.projections.UserEntitlementsView refreshFromTicketStatus(
				UUID userId,
				long version,
				Instant updatedAt) {
			operations.add("entitlementsProjection");
			return new com.nm.fragmentsclean.ticketContext.read.projections.UserEntitlementsView(
					userId,
					1,
					version,
					updatedAt);
		}
	}

	private static class RecordingProjectionSyncPublisher implements ProjectionSyncPublisher {
		private final List<String> operations;
		private final List<ProjectionSyncEvent> events = new ArrayList<>();

		private RecordingProjectionSyncPublisher(List<String> operations) {
			this.operations = operations;
		}

		@Override
		public void publish(ProjectionSyncEvent event) {
			operations.add("sync");
			events.add(event);
		}
	}
}
