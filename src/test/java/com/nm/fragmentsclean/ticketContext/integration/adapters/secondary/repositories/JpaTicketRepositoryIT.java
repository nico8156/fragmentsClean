package com.nm.fragmentsclean.ticketContext.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.ticketContext.integration.AbstractTicketJpaIntegrationTest;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.SpringTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.entities.TicketJpaEntity;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket.TicketSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

public class JpaTicketRepositoryIT extends AbstractTicketJpaIntegrationTest {

    private static final UUID TICKET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private TicketRepository ticketRepository; // port DDD

    @Autowired
    private SpringTicketRepository springTicketRepository; // repo Spring Data brut

    @Test
    void repositories_are_injected() {
        assertThat(ticketRepository).isNotNull();
        assertThat(springTicketRepository).isNotNull();
    }

    @Test
    void can_save_a_ticket() {
        var now = Instant.parse("2024-01-01T10:00:00Z");

        var snapshot = new TicketSnapshot(
                TICKET_ID,
                USER_ID,
                Ticket.TicketStatus.ANALYZING,
                null,                       // ocrText
                "s3://bucket/tickets/111.png",
                null,                       // amountCents
                "EUR",
                null,                       // ticketDate
                null,                       // merchantName
                null,                       // merchantAddress
                null,                       // paymentMethod
                null,                       // lineItems
                null,                       // rejectionReason
                now,
                now,
                0L
        );

        ticketRepository.save(Ticket.fromSnapshot(snapshot));

        assertThat(springTicketRepository.findAll()).containsExactly(
                new TicketJpaEntity(
                        TICKET_ID,
                        USER_ID,
                        Ticket.TicketStatus.ANALYZING,
                        null,
                        "s3://bucket/tickets/111.png",
                        null,
                        "EUR",
                        null,
                        null,
                        null,
                        null,
                        null,  // lineItemsJson
                        null,
                        now,
                        now,
                        0L
                )
        );
    }

    @Test
    void can_update_a_ticket() {
        var createdAt = Instant.parse("2024-01-01T10:00:00Z");

        // 1) save initial
        ticketRepository.save(Ticket.createNewAnalyzing(
                TICKET_ID,
                USER_ID,
                null,
                "s3://bucket/tickets/111.png",
                createdAt
        ));

        // 2) reload + enrich + save
        var loaded = ticketRepository.byId(TICKET_ID).orElseThrow();
        var updatedAt = Instant.parse("2024-01-01T11:00:00Z");
        var changed = loaded.markAnalyzingIfPossible("OCR TEXT", null, updatedAt);
        assertThat(changed).isTrue();

        ticketRepository.save(loaded);

        // 3) verify JPA raw
        var entity = springTicketRepository.findById(TICKET_ID).orElseThrow();
        assertThat(entity.getOcrText()).isEqualTo("OCR TEXT");
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(entity.getVersion()).isEqualTo(1L); // create=0, touch() => +1
    }
}
