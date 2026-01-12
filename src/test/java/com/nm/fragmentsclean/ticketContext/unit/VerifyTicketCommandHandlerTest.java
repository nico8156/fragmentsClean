package com.nm.fragmentsclean.ticketContext.unit;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.fake.FakeTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommand;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public class VerifyTicketCommandHandlerTest {

	private final UUID TICKET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private final UUID CMD_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	FakeTicketRepository ticketRepository = new FakeTicketRepository();
	FakeDomainEventPublisher domainEventPublisher = new FakeDomainEventPublisher();
	DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();

	VerifyTicketCommandHandler handler;

	@BeforeEach
	void setup() {
		handler = new VerifyTicketCommandHandler(ticketRepository, domainEventPublisher, dateTimeProvider);
	}

	@Test
	void should_create_new_ticket_in_analyzing_and_publish_verifyAccepted_event() {
		// WHEN
		handler.execute(new VerifyTicketCommand(
				CMD_ID,
				TICKET_ID,
				USER_ID,
				"s3://bucket/tickets/111.png",
				null,
				Instant.parse("2023-10-01T09:59:00Z")));

		// THEN : état ticket
		var snaps = ticketRepository.allSnapshots();
		assertThat(snaps).hasSize(1);

		var snap = snaps.getFirst();

		assertThat(snap.ticketId()).isEqualTo(TICKET_ID);
		assertThat(snap.userId()).isEqualTo(USER_ID);
		assertThat(snap.status().toString()).isEqualTo(Ticket.TicketStatus.ANALYZING.toString());
		assertThat(snap.ocrText()).isNull();
		assertThat(snap.imageRef()).isEqualTo("s3://bucket/tickets/111.png");
		assertThat(snap.createdAt()).isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));
		assertThat(snap.updatedAt()).isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));
		assertThat(snap.version()).isEqualTo(0L);

		// THEN : event publié
		assertThat(domainEventPublisher.published).hasSize(1);
		var evt = (TicketVerifyAcceptedEvent) domainEventPublisher.published.getFirst();

		assertThat(evt.commandId()).isEqualTo(CMD_ID);
		assertThat(evt.ticketId()).isEqualTo(TICKET_ID);
		assertThat(evt.userId()).isEqualTo(USER_ID);
		assertThat(evt.ocrText()).isNull();
		assertThat(evt.imageRef()).isEqualTo("s3://bucket/tickets/111.png");
		assertThat(evt.status().toString()).isEqualTo(Ticket.TicketStatus.ANALYZING.toString());
		assertThat(evt.version()).isEqualTo(0L);
		assertThat(evt.occurredAt()).isEqualTo(Instant.parse("2024-01-01T10:00:00Z"));
		assertThat(evt.clientAt()).isEqualTo(Instant.parse("2023-10-01T09:59:00Z"));
	}
}
