package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.entities.TicketJpaEntity;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaTicketRepository implements TicketRepository {

    private final SpringTicketRepository springTicketRepository;
    private final ObjectMapper objectMapper;

    public JpaTicketRepository(SpringTicketRepository springTicketRepository, ObjectMapper objectMapper) {
        this.springTicketRepository = springTicketRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Ticket> byId(UUID ticketId) {
        return springTicketRepository.findById(ticketId)
                .map(this::toDomain);
    }

    @Override
    public void save(Ticket ticket) {
        springTicketRepository.save(toJpa(ticket));
    }

    // ----- mapping -----

    private Ticket toDomain(TicketJpaEntity e) {
        return Ticket.fromSnapshot(
                new Ticket.TicketSnapshot(
                        e.getTicketId(),
                        e.getUserId(),
                        e.getStatus(),
                        e.getOcrText(),
                        e.getImageRef(),
                        e.getAmountCents(),
                        e.getCurrency(),
                        e.getTicketDate(),
                        e.getMerchantName(),
                        e.getMerchantAddress(),
                        e.getPaymentMethod(),
                        deserializeLineItems(e.getLineItemsJson()),
                        e.getRejectionReason(),
                        e.getCreatedAt(),
                        e.getUpdatedAt(),
                        e.getVersion()
                )
        );
    }

    private TicketJpaEntity toJpa(Ticket ticket) {
        var s = ticket.toSnapshot();

        return new TicketJpaEntity(
                s.ticketId(),
                s.userId(),
                s.status(),
                s.ocrText(),
                s.imageRef(),
                s.amountCents(),
                s.currency(),
                s.ticketDate(),
                s.merchantName(),
                s.merchantAddress(),
                s.paymentMethod(),
                serializeLineItems(s.lineItems()),
                s.rejectionReason(),
                s.createdAt(),
                s.updatedAt(),
                s.version()
        );
    }

    private String serializeLineItems(List<Ticket.TicketLineItem> items) {
        if (items == null) return null;
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize ticket lineItems", ex);
        }
    }

    private List<Ticket.TicketLineItem> deserializeLineItems(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            var type = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, Ticket.TicketLineItem.class);
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot deserialize ticket lineItems", ex);
        }
    }
}
