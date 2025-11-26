package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
public class OutboxEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;              // clé interne, sert aussi de cursor

    @Column(nullable = false, unique = true)
    private String eventId;       // UUID de l’event métier

    @Column(nullable = false)
    private String eventType;     // ex: "LikeSetEvent"

    @Column(nullable = false)
    private String aggregateType; // ex: "Like"

    @Column(nullable = false)
    private String aggregateId;   // ex: likeId.toString()

    @Column(nullable = false)
    private String streamKey;     // ex: "target:{targetId}" ou "user:{userId}"

    @Lob
    @Column(nullable = false)
    private String payloadJson;   // event sérialisé en JSON

    @Column(nullable = false)
    private Instant occurredAt;   // horodatage domaine

    @Column(nullable = false)
    private Instant createdAt;    // horodatage insertion outbox

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(nullable = false)
    private Integer retryCount;

    // --- Constructeurs ---

    public OutboxEventJpaEntity() {
        // requis par JPA
    }

    public OutboxEventJpaEntity(String eventId,
                                String eventType,
                                String aggregateType,
                                String aggregateId,
                                String streamKey,
                                String payloadJson,
                                Instant occurredAt,
                                Instant createdAt,
                                OutboxStatus status,
                                Integer retryCount) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.streamKey = streamKey;
        this.payloadJson = payloadJson;
        this.occurredAt = occurredAt;
        this.createdAt = createdAt;
        this.status = status;
        this.retryCount = retryCount;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    // --- Setters ---

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }
}
