package com.nm.fragmentsclean.platform.eventing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleCurationIntegrationPayloadTest {

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final IntegrationEventPayloadMapper mapper = new IntegrationEventPayloadMapper(json);

    @Test
    void withdrawalPayloadKeepsPrimitiveRevisionIdentifiers() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        UUID publishedRevisionId = UUID.randomUUID();
        UUID draftRevisionId = UUID.randomUUID();
        String payload = """
                {"eventId":"%s","commandId":"%s","articleId":"%s",
                 "publishedRevisionId":"%s","draftRevisionId":"%s","version":4,
                 "occurredAt":"2026-09-15T10:00:00Z","clientAt":"2026-09-15T09:59:00Z"}
                """.formatted(eventId, commandId, articleId, publishedRevisionId, draftRevisionId);

        var publicPayload = json.readTree(mapper.toPublicPayloadJson("article.withdrawn",
                outbox(eventId, articleId, "ArticleWithdrawnEvent", payload)));

        assertThat(publicPayload.get("articleId").asText()).isEqualTo(articleId.toString());
        assertThat(publicPayload.get("publishedRevisionId").asText()).isEqualTo(publishedRevisionId.toString());
        assertThat(publicPayload.get("draftRevisionId").asText()).isEqualTo(draftRevisionId.toString());
        assertThat(publicPayload.get("version").asLong()).isEqualTo(4L);
    }

    @Test
    void featuredPayloadSupportsRankRemovalWithoutLeakingDomainObjects() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        String payload = """
                {"eventId":"%s","commandId":"%s","articleId":"%s",
                 "featuredRank":null,"version":5,"occurredAt":"2026-09-15T10:00:00Z"}
                """.formatted(eventId, UUID.randomUUID(), articleId);

        var publicPayload = json.readTree(mapper.toPublicPayloadJson("article.featured_rank.changed",
                outbox(eventId, articleId, "ArticleFeaturedRankChangedEvent", payload)));

        assertThat(publicPayload.get("featuredRank").isNull()).isTrue();
        assertThat(publicPayload.get("articleId").asText()).isEqualTo(articleId.toString());
        assertThat(publicPayload.get("version").asLong()).isEqualTo(5L);
    }

    private static OutboxEventJpaEntity outbox(UUID eventId, UUID articleId, String eventType, String payload) {
        Instant now = Instant.parse("2026-09-15T10:00:00Z");
        return new OutboxEventJpaEntity(eventId.toString(), eventType, "Article", articleId.toString(),
                "article:" + articleId, payload, now, now, OutboxStatus.PENDING, 0);
    }
}
