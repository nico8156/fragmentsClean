package com.nm.fragmentsclean.ticketContext.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.SpringTicketRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(properties = {
        "fragments.rate-limit.enabled=true",
        "fragments.rate-limit.ticket-per-minute=1"
})
class TicketReleaseRateLimitIT extends AbstractTicketBaseE2E {
    private static final UUID USER_A = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID USER_B = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Autowired MockMvc mockMvc;
    @Autowired SpringTicketRepository tickets;

    @BeforeEach
    void clean() {
        tickets.deleteAll();
    }

    @Test
    void limits_the_authenticated_ticket_intention_without_sharing_quota_between_users() throws Exception {
        submit(USER_A, UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1"),
                UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb1"))
                .andExpect(status().isAccepted());

        submit(USER_A, UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa2"),
                UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb2"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("RATE_LIMITED"));

        submit(USER_B, UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa3"),
                UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbb3"))
                .andExpect(status().isAccepted());

        assertThat(tickets.count()).isEqualTo(2);
    }

    private org.springframework.test.web.servlet.ResultActions submit(UUID userId, UUID commandId, UUID ticketId)
            throws Exception {
        return mockMvc.perform(post("/api/tickets/verify")
                .with(jwt().jwt(token -> token.subject(userId.toString()).claim("roles", List.of("USER"))))
                .contentType("application/json")
                .content("""
                        {
                          "commandId": "%s",
                          "ticketId": "%s",
                          "imageRef": null,
                          "ocrText": "CAFE FRAGMENTS USER %s TOTAL 4.00 EUR",
                          "clientAt": "2026-09-12T09:59:00Z"
                        }
                        """.formatted(commandId, ticketId, userId)));
    }
}
