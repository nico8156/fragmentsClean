package com.nm.fragmentsclean.socialContextTest.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender.WebSocketOutboxEventSender;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class WebSocketOutboxEventSenderTest {

	@Test
	void sends_like_ack_envelope_to_topic_with_streamKey() throws Exception {
		// GIVEN
		SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
		ObjectMapper om = new ObjectMapper()
				.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

		WebSocketOutboxEventSender sender = new WebSocketOutboxEventSender(messagingTemplate, om);

		String streamKey = "social:e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3";
		UUID eventId = UUID.randomUUID();
		UUID commandId = UUID.randomUUID();
		UUID likeId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID targetId = UUID.randomUUID();

		String payload = """
				{
				  "eventId":"%s",
				  "commandId":"%s",
				  "likeId":"%s",
				  "userId":"%s",
				  "targetId":"%s",
				  "version":1,
				  "count":7,
				  "active":true,
				  "clientAt":"2024-01-01T10:00:00Z",
				  "occurredAt":"2024-01-01T10:00:00Z"
				}
				""".formatted(
				eventId,
				commandId,
				likeId,
				userId,
				targetId);

		OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(
				"event-id-123",
				LikeSetEvent.class.getName(),
				"Like",
				"f47b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3",
				streamKey,
				payload,
				Instant.parse("2024-01-01T10:00:00Z"),
				Instant.parse("2024-01-01T10:00:00Z"),
				OutboxStatus.PENDING,
				0);

		// WHEN
		sender.send(outboxEvent);

		// THEN: destination OK
		ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
		verify(messagingTemplate).convertAndSend(eq("/topic/" + streamKey), jsonCaptor.capture());

		// THEN: envelope JSON OK (on parse la réponse)
		WebSocketOutboxEventSender.WsLikeAckEnvelope env = om.readValue(jsonCaptor.getValue(),
				WebSocketOutboxEventSender.WsLikeAckEnvelope.class);

		assertThat(env.type()).isEqualTo("social.like.added_ack");
		assertThat(env.commandId()).isEqualTo(commandId.toString());

		assertThat(env.count()).isEqualTo(7);
		assertThat(env.me()).isTrue();
		assertThat(env.version()).isEqualTo(1);
		assertThat(env.updatedAt()).isEqualTo("2024-01-01T10:00:00Z");
	}
}
