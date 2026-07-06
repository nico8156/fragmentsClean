package com.nm.fragmentsclean.ticketContext.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.fake.FakeTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationProcessManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketVerificationProcessManagerTest {
	private static final UUID COMMAND_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID TICKET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final Instant CLIENT_AT = Instant.parse("2023-10-01T09:59:00Z");

	private FakeTicketRepository ticketRepository;
	private FakeDomainEventPublisher eventPublisher;
	private DeterministicDateTimeProvider dateTimeProvider;

	@BeforeEach
	void setUp() {
		ticketRepository = new FakeTicketRepository();
		eventPublisher = new FakeDomainEventPublisher();
		dateTimeProvider = new DeterministicDateTimeProvider();
		ticketRepository.save(Ticket.createNewAnalyzing(
				TICKET_ID,
				USER_ID,
				"CAFE\nTOTAL 4,00 EUR",
				"s3://bucket/tickets/111.png",
				dateTimeProvider.now()));
	}

	@Test
	void business_rejection_publishes_completed_event() {
		var handler = new TicketVerificationProcessManager(
				ticketRepository,
				(ocrText, imageRef) -> new TicketVerificationProvider.Rejected(
						"PARTIAL_VERIFICATION",
						"ticketverify returned a partial result",
						"tv:test"),
				eventPublisher,
				dateTimeProvider);

		handler.handle(acceptedEvent());

		assertThat(eventPublisher.published).hasSize(1);
		var event = (TicketVerificationCompletedEvent) eventPublisher.published.getFirst();
		assertThat(event.outcome()).isEqualTo(TicketVerificationCompletedEvent.Outcome.REJECTED);
		assertThat(event.rejected().reasonCode()).isEqualTo("PARTIAL_VERIFICATION");
		assertThat(ticketRepository.byId(TICKET_ID).orElseThrow().toSnapshot().status())
				.isEqualTo(Ticket.TicketStatus.REJECTED);
	}

	@Test
	void retryable_technical_failure_throws_so_sqs_can_redeliver() {
		var handler = new TicketVerificationProcessManager(
				ticketRepository,
				(ocrText, imageRef) -> new TicketVerificationProvider.FailedRetryable("timeout", "tv:timeout"),
				eventPublisher,
				dateTimeProvider);

		assertThatThrownBy(() -> handler.handle(acceptedEvent()))
				.isInstanceOf(TicketVerificationProcessManager.TicketVerificationRetryableException.class)
				.hasMessageContaining(TICKET_ID.toString());

		assertThat(eventPublisher.published).isEmpty();
		assertThat(ticketRepository.byId(TICKET_ID).orElseThrow().toSnapshot().status())
				.isEqualTo(Ticket.TicketStatus.ANALYZING);
	}

	@Test
	void approved_result_publishes_completed_event() {
		var handler = new TicketVerificationProcessManager(
				ticketRepository,
				(ocrText, imageRef) -> new TicketVerificationProvider.Approved(
						400,
						"EUR",
						null,
						"CAFE",
						null,
						null,
						List.of(),
						"tv:ok"),
				eventPublisher,
				dateTimeProvider);

		handler.handle(acceptedEvent());

		assertThat(eventPublisher.published).hasSize(1);
		var event = (TicketVerificationCompletedEvent) eventPublisher.published.getFirst();
		assertThat(event.outcome()).isEqualTo(TicketVerificationCompletedEvent.Outcome.APPROVED);
		assertThat(event.approved().amountCents()).isEqualTo(400);
		assertThat(ticketRepository.byId(TICKET_ID).orElseThrow().toSnapshot().status())
				.isEqualTo(Ticket.TicketStatus.CONFIRMED);
	}

	private TicketVerifyAcceptedEvent acceptedEvent() {
		return new TicketVerifyAcceptedEvent(
				UUID.fromString("44444444-4444-4444-4444-444444444444"),
				COMMAND_ID,
				TICKET_ID,
				USER_ID,
				"CAFE\nTOTAL 4,00 EUR",
				"s3://bucket/tickets/111.png",
				Ticket.TicketStatus.ANALYZING.name(),
				0L,
				dateTimeProvider.now(),
				CLIENT_AT);
	}
}
