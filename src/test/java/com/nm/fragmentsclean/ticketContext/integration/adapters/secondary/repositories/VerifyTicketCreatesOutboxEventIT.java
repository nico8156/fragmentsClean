package com.nm.fragmentsclean.ticketContext.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.ticketContext.integration.AbstractTicketOutboxJpaIntegrationTest;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommand;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.VerifyTicketCommandHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class VerifyTicketCreatesOutboxEventIT extends AbstractTicketOutboxJpaIntegrationTest {

    private static final UUID TICKET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CMD_ID    = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired
    private VerifyTicketCommandHandler handler;

    @Autowired
    private SpringOutboxEventRepository outboxRepository;

    @Test
    void verify_ticket_persists_outbox_event_pending() {
        // WHEN
        handler.execute(new VerifyTicketCommand(
                CMD_ID,
                TICKET_ID,
                USER_ID,
                null,
                "s3://bucket/tickets/111.png",
                Instant.parse("2023-10-01T09:59:00Z")
        ));

        // THEN
        var all = outboxRepository.findAll();
        assertThat(all).hasSize(1);

        var e = all.getFirst();
        assertThat(e.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(e.getAggregateType()).isEqualTo("Ticket");
        assertThat(e.getAggregateId()).isEqualTo(TICKET_ID.toString());
        assertThat(e.getStreamKey()).isEqualTo("user:" + USER_ID);

        // eventType = FQCN (comme ton publisher)
        assertThat(e.getEventType()).contains("TicketVerifyAcceptedEvent");

        // payload minimalement cohérent
        assertThat(e.getPayloadJson()).contains(TICKET_ID.toString());
        assertThat(e.getPayloadJson()).contains(USER_ID.toString());

        // occurredAt = serverNow (dans l’event), createdAt = dateTimeProvider.now()
        assertThat(e.getOccurredAt()).isNotNull();
        assertThat(e.getCreatedAt()).isNotNull();
        assertThat(e.getRetryCount()).isEqualTo(0);
    }
}
