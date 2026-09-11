package com.nm.fragmentsclean.ticketContext.e2e;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.SpringTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.entities.TicketJpaEntity;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WriteTicketCommandReceiptIT extends AbstractTicketBaseE2E {
    private static final UUID USER_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID TICKET_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID COMMAND_ID = UUID.fromString("33333333-3333-4333-8333-333333333333");

    @Autowired MockMvc mockMvc;
    @Autowired SpringTicketRepository tickets;
    @Autowired SpringOutboxEventRepository outbox;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        tickets.deleteAll();
        outbox.deleteAll();
        jdbc.update("DELETE FROM command_status");
    }

    @Test
    void accepted_ticket_command_is_applied_before_the_ticket_business_outcome() throws Exception {
        submitTicket().andExpect(status().isAccepted());

        mockMvc.perform(get("/commands/{commandId}", COMMAND_ID).with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));
        assertThat(tickets.findById(TICKET_ID).orElseThrow().getStatus())
                .isEqualTo(Ticket.TicketStatus.ANALYZING);
    }

    @Test
    void already_rejected_ticket_does_not_turn_the_transport_command_into_rejected() throws Exception {
        Instant now = Instant.parse("2026-09-11T10:00:00Z");
        tickets.save(new TicketJpaEntity(
                TICKET_ID, USER_ID, Ticket.TicketStatus.REJECTED,
                "receipt", null, null, "EUR", null, null, null, null, null,
                "NOT_A_COFFEE_TICKET", now, now, 1L));

        submitTicket().andExpect(status().isAccepted());

        mockMvc.perform(get("/commands/{commandId}", COMMAND_ID).with(userJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"));
        assertThat(tickets.findById(TICKET_ID).orElseThrow().getStatus())
                .isEqualTo(Ticket.TicketStatus.REJECTED);
        assertThat(outbox.findAll()).isEmpty();
    }

    private org.springframework.test.web.servlet.ResultActions submitTicket() throws Exception {
        return mockMvc.perform(post("/api/tickets/verify")
                .with(userJwt())
                .contentType("application/json")
                .content("""
                        {
                          "commandId": "%s",
                          "ticketId": "%s",
                          "imageRef": null,
                          "ocrText": "CAFE FRAGMENTS TOTAL 4.00 EUR",
                          "clientAt": "2026-09-11T09:59:00Z"
                        }
                        """.formatted(COMMAND_ID, TICKET_ID)));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor userJwt() {
        return jwt().jwt(token -> token.subject(USER_ID.toString()).claim("roles", java.util.List.of("USER")));
    }
}
