package com.nm.fragmentsclean.ticketContext.e2e.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.ticketContext.e2e.AbstractTicketBaseE2E;
import com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.kafka.TicketEventsKafkaListener;
import com.nm.fragmentsclean.ticketContext.write.adapters.primary.springboot.kafka.TicketVerificationRequestsKafkaListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.context.EmbeddedKafka;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("auth_test")
@EmbeddedKafka(partitions = 1, topics = {
        "ticket-events",
        "ticket-verification-requested"
})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "logging.level.org.springframework.kafka=INFO",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql"
})
public class TicketVerificationPipelineIT extends AbstractTicketBaseE2E {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @Autowired SpringOutboxEventRepository outboxRepo;
    @Autowired OutboxEventDispatcher outboxEventDispatcher;
    @Autowired KafkaListenerEndpointRegistry registry;

    @Autowired javax.sql.DataSource ds;


    @Autowired(required = false)
    TicketEventsKafkaListener ticketReadListener;

    @Autowired(required = false)
    TicketVerificationRequestsKafkaListener ticketWorkerListener;

    @BeforeEach
    void setup() {
        // projection
        jdbcTemplate.update("DELETE FROM ticket_status_projection");

        // domain tables (si besoin : adapte à ton schema réel)
        // jdbcTemplate.update("DELETE FROM tickets");

        // outbox
        outboxRepo.deleteAll();
    }

    @Test
    void listeners_are_loaded() {
        assertThat(ticketReadListener).isNotNull();
        assertThat(ticketWorkerListener).isNotNull();
    }

    @Test
    void debug_db() throws Exception {
        System.out.println("DS = " + ds.getConnection().getMetaData().getURL());
        System.out.println("Driver = " + ds.getConnection().getMetaData().getDriverName());
    }

    @Test
    void verify_ticket_goes_analyzing_then_completed_and_projection_is_updated() throws Exception {
        // GIVEN
        UUID userId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        Instant clientAt = Instant.parse("2023-10-01T09:59:00Z");

        // WHEN: HTTP verify
        mockMvc.perform(
                post("/api/tickets/verify")
                        .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                        .contentType("application/json")
                        .content("""
                            {
                              "commandId": "%s",
                              "ticketId": "%s",
                              "ocrText": null,
                              "imageRef": "s3://bucket/tickets/111.png",
                              "clientAt": "%s"
                            }
                            """.formatted(commandId, ticketId, clientAt))
        ).andExpect(status().isAccepted());
        var rows = jdbcTemplate.queryForList("""
  SELECT id, aggregate_type, event_type, stream_key, status
  FROM outbox_events
  ORDER BY id ASC
""");
        System.out.println("OUTBOX=" + rows);

        // THEN: outbox has Ticket event(s) (accepted)
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM outbox_events WHERE aggregate_type = 'Ticket'",
                    Integer.class
            );
            assertThat(count).isNotNull();
            assertThat(count).isGreaterThan(0);
        });

        Instant now = Instant.parse("2024-01-01T10:00:00Z");
//try {
//
//
//    jdbcTemplate.update("""
//  INSERT INTO ticket_status_projection (
//    ticket_id, user_id, status, outcome,
//    image_ref, ocr_text,
//    amount_cents, currency, ticket_date,
//    merchant_name, merchant_address, payment_method,
//    rejection_reason, version, occurred_at
//  )
//  VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
//  ON CONFLICT (ticket_id) DO UPDATE SET
//    user_id = EXCLUDED.user_id,
//    status = EXCLUDED.status,
//    outcome = EXCLUDED.outcome,
//    image_ref = COALESCE(EXCLUDED.image_ref, ticket_status_projection.image_ref),
//    ocr_text  = COALESCE(EXCLUDED.ocr_text,  ticket_status_projection.ocr_text),
//    version = EXCLUDED.version,
//    occurred_at = EXCLUDED.occurred_at
//""",
//            ticketId, userId, "ANALYZING", null,
//            "s3://x", null,
//            null, null, null,
//            null, null, null,
//            null, 0L, now
//    );
//
//    Integer exists = jdbcTemplate.queryForObject("""
//  SELECT COUNT(*)
//  FROM information_schema.tables
//  WHERE table_schema='public'
//    AND table_name='ticket_status_projection'
//""", Integer.class);
//
//    System.out.println("exists=" + exists);
//
//    var cols = jdbcTemplate.queryForList("""
//  SELECT column_name, data_type
//  FROM information_schema.columns
//  WHERE table_schema='public'
//    AND table_name='ticket_status_projection'
//  ORDER BY ordinal_position
//""");
//    System.out.println("cols=" + cols);
//}catch (org.springframework.jdbc.BadSqlGrammarException e) {
//    System.out.println("=== BadSqlGrammarException ===");
//    System.out.println("SQLState=" + e.getSQLException().getSQLState());
//    System.out.println("ErrorCode=" + e.getSQLException().getErrorCode());
//    System.out.println("PSQL message=" + e.getSQLException().getMessage());
//    throw e;
//}

        // WHEN: dispatch outbox -> kafka (will duplicate accepted to ticket-events + ticket-verification-requested)
        outboxEventDispatcher.dispatchPending();

        // sanity: listeners running
        assertThat(registry.getListenerContainers()).isNotEmpty();
        for (MessageListenerContainer c : registry.getListenerContainers()) {
            System.out.println("KAFKA container: " + c.getListenerId() + " running=" + c.isRunning());
        }
        assertThat(registry.getListenerContainers().stream().anyMatch(MessageListenerContainer::isRunning)).isTrue();

        // THEN: projection already has ANALYZING (from TicketVerifyAcceptedEvent on ticket-events)
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer p = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ticket_status_projection WHERE ticket_id = ? AND status = 'ANALYZING'",
                    Integer.class,
                    ticketId
            );
            assertThat(p).isEqualTo(1);
        });

        Map<String, Object> analyzingRow = jdbcTemplate.queryForMap("""
            SELECT ticket_id, user_id, status, version, image_ref
            FROM ticket_status_projection
            WHERE ticket_id = ?
        """, ticketId);

        assertThat(analyzingRow.get("ticket_id")).isEqualTo(ticketId);
        assertThat(analyzingRow.get("user_id")).isEqualTo(userId);
        assertThat(analyzingRow.get("status")).isEqualTo("ANALYZING");
        //assertThat(analyzingRow.get("image_ref")).isEqualTo("s3://bucket/tickets/111.png");

        // At this point the worker listener should have consumed ticket-verification-requested
        // and produced TicketVerificationCompletedEvent into outbox.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Integer completedCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM outbox_events
                    WHERE aggregate_type = 'Ticket'
                      AND event_type LIKE '%TicketVerificationCompletedEvent%'
                """, Integer.class);
            assertThat(completedCount).isNotNull();
            assertThat(completedCount).isGreaterThan(0);
        });

        // WHEN: dispatch completed -> kafka (ticket-events) (+ ws via router if enabled)
        outboxEventDispatcher.dispatchPending();

        // THEN: projection moves to CONFIRMED (from TicketVerificationCompletedEvent)
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT status, outcome, amount_cents, currency, merchant_name, version
                FROM ticket_status_projection
                WHERE ticket_id = ?
            """, ticketId);

            assertThat(row.get("status")).isEqualTo("CONFIRMED");
            assertThat(row.get("outcome")).isEqualTo("APPROVED");
            assertThat(row.get("currency")).isEqualTo("EUR");
            assertThat(row.get("merchant_name")).isNotNull();
            assertThat(((Number) row.get("version")).longValue()).isGreaterThanOrEqualTo(1L);
        });
    }
}
