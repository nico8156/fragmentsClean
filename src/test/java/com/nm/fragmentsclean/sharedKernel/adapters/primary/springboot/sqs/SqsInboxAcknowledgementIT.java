package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.transport.SqsMessagingProperties;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.InboxMessageRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

class SqsInboxAcknowledgementIT {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:13.1");
    private static JdbcTemplate jdbc;

    @BeforeAll
    static void database() {
        POSTGRES.start();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()));
        jdbc.execute("""
                CREATE TABLE inbox_messages (
                  id bigserial primary key,
                  destination varchar(255) not null,
                  event_id varchar(50) not null,
                  event_type varchar(255) not null,
                  event_version integer not null,
                  received_at timestamptz not null,
                  processed_at timestamptz,
                  lease_until timestamptz,
                  lease_owner varchar(36),
                  status varchar(32) not null,
                  error_message text,
                  unique(destination,event_id))
                """);
    }

    @AfterAll
    static void stop() {
        POSTGRES.stop();
    }

    @BeforeEach
    void clearInbox() {
        jdbc.update("TRUNCATE inbox_messages");
    }

    @Test
    void active_claim_is_not_acknowledged_and_expired_claim_is_reprocessed_once() throws Exception {
        var envelope = envelope();
        var inbox = new InboxMessageRepository(jdbc);
        var staleClaim = inbox.claim(envelope);
        var handler = new RecordingHandler();
        var router = new SqsIntegrationEventRouter(inbox, List.of(handler));
        var sqs = new RecordingSqsClient();
        var message = Message.builder()
                .messageId("message-1")
                .receiptHandle("receipt-1")
                .body(JsonMapper.builder().addModule(new JavaTimeModule()).build().writeValueAsString(envelope))
                .build();
        var consumer = consumer(sqs, router);

        sqs.messages.add(message);
        consumer.pollDestination("coffees-events", "https://sqs.example/coffees");

        assertThat(handler.handled).isEmpty();
        assertThat(sqs.deleted).isEmpty();
        assertThat(sqs.visibilityChanges).hasSize(1);

        jdbc.update("UPDATE inbox_messages SET lease_until=now()-interval '1 second' WHERE event_id=?", envelope.eventId());
        sqs.messages.add(message);
        consumer.pollDestination("coffees-events", "https://sqs.example/coffees");

        assertThat(handler.handled).containsExactly(envelope);
        assertThat(sqs.deleted).hasSize(1);
        assertThat(jdbc.queryForObject(
                "SELECT status FROM inbox_messages WHERE event_id=?", String.class, envelope.eventId()))
                .isEqualTo("PROCESSED");
        assertThat(inbox.markProcessed(envelope, staleClaim.ownerToken())).isFalse();
    }

    private static SqsIntegrationEventConsumer consumer(
            SqsClient sqs,
            SqsIntegrationEventRouting router
    ) {
        var properties = new SqsMessagingProperties();
        properties.setQueues(new LinkedHashMap<>());
        properties.setMaxMessages(1);
        properties.setWaitTime(Duration.ZERO);
        properties.setVisibilityTimeout(Duration.ofSeconds(1));
        properties.setShutdownTimeout(Duration.ofMillis(10));
        return new SqsIntegrationEventConsumer(
                sqs,
                properties,
                JsonMapper.builder().addModule(new JavaTimeModule()).build(),
                router,
                workerCount -> {
                    throw new UnsupportedOperationException("The test polls explicitly");
                });
    }

    private static IntegrationEventEnvelope envelope() {
        return new IntegrationEventEnvelope(
                "event-1",
                "coffee.created",
                1,
                "source",
                "Coffee",
                "coffee-1",
                "coffee:coffee-1",
                "coffees-events",
                "{}",
                Instant.parse("2026-09-17T08:00:00Z"));
    }

    private static class RecordingHandler implements SqsIntegrationEventHandler {
        private final List<IntegrationEventEnvelope> handled = new ArrayList<>();

        @Override
        public SqsIntegrationEventRoute route() {
            return new SqsIntegrationEventRoute("coffees-events", "coffee.created");
        }

        @Override
        public void handle(IntegrationEventEnvelope envelope) {
            handled.add(envelope);
        }
    }

    private static class RecordingSqsClient implements SqsClient {
        private final Queue<Message> messages = new ConcurrentLinkedQueue<>();
        private final List<DeleteMessageRequest> deleted = new ArrayList<>();
        private final List<ChangeMessageVisibilityRequest> visibilityChanges = new ArrayList<>();

        @Override
        public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest request) {
            var message = messages.poll();
            return ReceiveMessageResponse.builder()
                    .messages(message == null ? List.of() : List.of(message))
                    .build();
        }

        @Override
        public DeleteMessageResponse deleteMessage(DeleteMessageRequest request) {
            deleted.add(request);
            return DeleteMessageResponse.builder().build();
        }

        @Override
        public ChangeMessageVisibilityResponse changeMessageVisibility(ChangeMessageVisibilityRequest request) {
            visibilityChanges.add(request);
            return ChangeMessageVisibilityResponse.builder().build();
        }

        @Override
        public String serviceName() {
            return "sqs";
        }

        @Override
        public void close() {
        }
    }
}
