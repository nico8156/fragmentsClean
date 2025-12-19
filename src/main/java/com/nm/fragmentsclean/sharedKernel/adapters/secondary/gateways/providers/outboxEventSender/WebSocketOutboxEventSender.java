package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventSender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventSender;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketOutboxEventSender implements OutboxEventSender {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public WebSocketOutboxEventSender(SimpMessagingTemplate messagingTemplate,
                                      ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void send(OutboxEventJpaEntity outboxEvent) throws Exception {

        String streamKey = outboxEvent.getStreamKey();
        boolean isUserQueue = streamKey != null && streamKey.startsWith("user:");
        String userId = isUserQueue ? streamKey.substring("user:".length()) : null;

        String json = buildEnvelopeJson(outboxEvent);

        if (isUserQueue) {
            // ✅ client subscribe "/user/queue/acks"
            messagingTemplate.convertAndSendToUser(userId, "/queue/acks", json);
            System.out.println("[WS] SEND to user " + userId + " payload=" + json);
        } else {
            // fallback : broadcast
            String destination = "/topic/" + streamKey;
            messagingTemplate.convertAndSend(destination, json);
            System.out.println("[WS] SEND to topic " + destination + " payload=" + json);
        }
    }

    private String buildEnvelopeJson(OutboxEventJpaEntity e) throws Exception {
        String eventType = e.getEventType();
        String payload = e.getPayloadJson();

        // -------------------------
        // SOCIAL
        // -------------------------

        // ✅ LikeSetEvent -> LikeAck envelope
        if (eventType.equals(LikeSetEvent.class.getName())) {
            LikeSetEvent evt = objectMapper.readValue(payload, LikeSetEvent.class);

            String type = evt.active()
                    ? "social.like.added_ack"
                    : "social.like.removed_ack";

            WsLikeAckEnvelope env = new WsLikeAckEnvelope(
                    type,
                    evt.commandId(),
                    evt.targetId().toString(),
                    evt.count(),
                    evt.active(), // me = état serveur après traitement
                    evt.version(),
                    evt.occurredAt() != null ? evt.occurredAt().toString() : null
            );

            return objectMapper.writeValueAsString(env);
        }

        // ✅ CommentCreatedEvent -> comment.created_ack
        if (eventType.equals(CommentCreatedEvent.class.getName())) {
            CommentCreatedEvent evt = objectMapper.readValue(payload, CommentCreatedEvent.class);

            WsCommentAckEnvelope env = new WsCommentAckEnvelope(
                    "social.comment.created_ack",
                    evt.commandId().toString(),
                    evt.targetId().toString(),
                    evt.commentId().toString(),
                    evt.version(),
                    evt.occurredAt() != null ? evt.occurredAt().toString() : null
            );
            return objectMapper.writeValueAsString(env);
        }

        // ✅ CommentUpdatedEvent -> comment.updated_ack
        if (eventType.equals(CommentUpdatedEvent.class.getName())) {
            CommentUpdatedEvent evt = objectMapper.readValue(payload, CommentUpdatedEvent.class);

            WsCommentAckEnvelope env = new WsCommentAckEnvelope(
                    "social.comment.updated_ack",
                    evt.commandId().toString(),
                    evt.targetId().toString(),
                    evt.commentId().toString(),
                    evt.version(),
                    evt.occurredAt() != null ? evt.occurredAt().toString() : null
            );
            return objectMapper.writeValueAsString(env);
        }

        // ✅ CommentDeletedEvent -> comment.deleted_ack
        if (eventType.equals(CommentDeletedEvent.class.getName())) {
            CommentDeletedEvent evt = objectMapper.readValue(payload, CommentDeletedEvent.class);

            WsCommentAckEnvelope env = new WsCommentAckEnvelope(
                    "social.comment.deleted_ack",
                    evt.commandId().toString(),
                    evt.targetId().toString(),
                    evt.commentId().toString(),
                    evt.version(),
                    evt.occurredAt() != null ? evt.occurredAt().toString() : null
            );
            return objectMapper.writeValueAsString(env);
        }

        // -------------------------
        // TICKET
        // -------------------------

        // ✅ TicketVerifyAcceptedEvent -> ticket.verify.accepted_ack
        if (eventType.equals(TicketVerifyAcceptedEvent.class.getName())) {
            TicketVerifyAcceptedEvent evt = objectMapper.readValue(payload, TicketVerifyAcceptedEvent.class);

            WsTicketVerifyAcceptedAckEnvelope env = new WsTicketVerifyAcceptedAckEnvelope(
                    "ticket.verify.accepted_ack",
                    evt.commandId().toString(),
                    evt.ticketId().toString(),
                    evt.status(), // "ANALYZING"
                    evt.version(),
                    evt.occurredAt() != null ? evt.occurredAt().toString() : null
            );

            return objectMapper.writeValueAsString(env);
        }

        // ✅ TicketVerificationCompletedEvent -> ticket.verification.completed_ack
        if (eventType.equals(TicketVerificationCompletedEvent.class.getName())) {
            TicketVerificationCompletedEvent evt = objectMapper.readValue(payload, TicketVerificationCompletedEvent.class);

            // type stable + outcome explicite, le front n'a pas besoin du class name Java
            WsTicketVerificationCompletedAckEnvelope env = new WsTicketVerificationCompletedAckEnvelope(
                    "ticket.verification.completed_ack",
                    evt.commandId().toString(),
                    evt.ticketId().toString(),
                    evt.outcome().name(), // APPROVED / REJECTED / FAILED_*
                    evt.version(),
                    evt.occurredAt() != null ? evt.occurredAt().toString() : null,
                    payload // tu gardes le payload complet (utile front + debug)
            );

            return objectMapper.writeValueAsString(env);
        }

        // -------------------------
        // fallback : enveloppe générique pour debug
        // -------------------------
        WsRawEnvelope raw = new WsRawEnvelope(
                "unknown",
                e.getEventId(),
                payload
        );
        return objectMapper.writeValueAsString(raw);
    }

    // --- envelopes ---

    public record WsLikeAckEnvelope(
            String type,
            String commandId,
            String targetId,
            long count,
            boolean me,
            long version,
            String updatedAt
    ) {}

    public record WsCommentAckEnvelope(
            String type,
            String commandId,
            String targetId,
            String commentId,
            long version,
            String updatedAt
    ) {}

    public record WsTicketVerifyAcceptedAckEnvelope(
            String type,
            String commandId,
            String ticketId,
            String status,
            long version,
            String updatedAt
    ) {}

    public record WsTicketVerificationCompletedAckEnvelope(
            String type,
            String commandId,
            String ticketId,
            String outcome,
            long version,
            String updatedAt,
            String payloadJson
    ) {}

    public record WsRawEnvelope(
            String type,
            String eventId,
            String payloadJson
    ) {}
}
