package com.nm.fragmentsclean.ticketContext.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.ticketContext.read.GetTicketStatusQueryHandler;
import com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.controllers.ReadTicketController;
import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcTicketStatusReadRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TicketOwnershipHttpIT {
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.1");
    private static final UUID OWNER = UUID.randomUUID(), OTHER = UUID.randomUUID(), TICKET = UUID.randomUUID();
    private static JdbcTicketStatusReadRepository repository;
    private MockMvc mvc;

    @BeforeAll static void database() throws Exception {
        postgres.start();
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        String schema = new ClassPathResource("schema.sql").getContentAsString(StandardCharsets.UTF_8);
        int start = schema.indexOf("create table if not exists ticket_status_projection (");
        jdbc.execute(schema.substring(start, schema.indexOf(';', start) + 1));
        jdbc.update("""
            INSERT INTO ticket_status_projection(ticket_id,user_id,status,ocr_text,merchant_name,version,occurred_at)
            VALUES (?,?,'CONFIRMED','private receipt','private merchant',1,now())
            """, TICKET, OWNER);
        repository = new JdbcTicketStatusReadRepository(jdbc);
    }

    @AfterAll static void stopDatabase() { postgres.stop(); }
    @BeforeEach void web() {
        var bus = new QueryBus();
        bus.registerQueryHandlers(List.of(new GetTicketStatusQueryHandler(repository)));
        mvc = MockMvcBuilders.standaloneSetup(new ReadTicketController(bus))
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }
    @AfterEach void clearIdentity() { SecurityContextHolder.clearContext(); }
    private void authenticatedAs(UUID userId) {
        Jwt jwt = Jwt.withTokenValue("test").header("alg", "none").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test void owner_can_read_receipt() throws Exception {
        authenticatedAs(OWNER);
        mvc.perform(get("/api/tickets/{id}/status", TICKET)).andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(OWNER.toString()))
            .andExpect(jsonPath("$.ocrText").value("private receipt"));
    }
    @Test void foreign_and_missing_receipts_are_indistinguishable() throws Exception {
        authenticatedAs(OTHER);
        mvc.perform(get("/api/tickets/{id}/status", TICKET)).andExpect(status().isNotFound())
            .andExpect(content().string(""));
        mvc.perform(get("/api/tickets/{id}/status", UUID.randomUUID())).andExpect(status().isNotFound())
            .andExpect(content().string(""));
        assertThat(repository.findByIdAndUserId(TICKET, OTHER)).isNull();
    }
    @Test void anonymous_cannot_read_receipt() throws Exception {
        mvc.perform(get("/api/tickets/{id}/status", TICKET)).andExpect(status().isUnauthorized());
    }
    @Test void admin_read_remains_available() {
        assertThat(repository.findById(TICKET).ocrText()).isEqualTo("private receipt");
        assertThat(repository.list()).hasSize(1);
    }
}
