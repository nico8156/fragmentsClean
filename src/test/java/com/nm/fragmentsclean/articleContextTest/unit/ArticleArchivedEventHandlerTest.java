package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.aticleContext.read.projections.*;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleArchivedEventHandlerTest {
    @Test
    void archives_projection_before_signalling_collection_freshness() {
        var repository = new RecordingRepository();
        var publisher = new RecordingPublisher();
        var event = new ArticleArchivedIntegrationEvent(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), 4, Instant.parse("2026-08-28T12:00:00Z"),
                Instant.parse("2026-08-28T11:59:00Z"));

        new ArticleArchivedEventHandler(repository, publisher).handle(event);

        assertThat(repository.event).isEqualTo(event);
        assertThat(publisher.event.projection()).isEqualTo("articles");
        assertThat(publisher.event.hints()).containsExactly("archived", "publicationStatus");
        assertThat(publisher.event.version()).isEqualTo(4L);
    }

    private static final class RecordingRepository implements ArticleProjectionRepository {
        private ArticleArchivedIntegrationEvent event;
        public void apply(ArticleArchivedIntegrationEvent event) { this.event = event; }
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
