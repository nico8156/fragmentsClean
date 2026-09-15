package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.articleContext.read.projections.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ArticleWithdrawnEventHandlerTest {
    @Test
    void withdraws_public_projection_before_signalling_mobile_to_reload_the_snapshot() {
        var repository = new RecordingRepository();
        var publisher = new RecordingPublisher();
        var event = new ArticleWithdrawnIntegrationEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 4,
                Instant.parse("2026-09-15T10:00:00Z"), Instant.parse("2026-09-15T10:00:00Z"));

        new ArticleWithdrawnEventHandler(repository, publisher).handle(event);

        assertThat(repository.event).isEqualTo(event);
        assertThat(publisher.event.projection()).isEqualTo("articles");
        assertThat(publisher.event.hints()).contains("draft", "publicationStatus");
    }

    private static final class RecordingRepository implements ArticleProjectionRepository {
        private ArticleWithdrawnIntegrationEvent event;
        public void apply(ArticleWithdrawnIntegrationEvent event) { this.event = event; }
        public void apply(ArticleFeaturedRankChangedIntegrationEvent event) { throw new AssertionError(); }
        public void apply(ArticleArchivedIntegrationEvent event) { throw new AssertionError(); }
        public void apply(ArticleRevisionPublishedIntegrationEvent event) { throw new AssertionError(); }
        public void apply(ArticleCreatedEvent event) { throw new AssertionError(); }
        public long count() { return 0; }
        public void insertSeed(ArticleProjectionRow row) { throw new AssertionError(); }
    }
    private static final class RecordingPublisher implements ProjectionSyncPublisher {
        private ProjectionSyncEvent event;
        public void publish(ProjectionSyncEvent event) { this.event = event; }
    }
}
