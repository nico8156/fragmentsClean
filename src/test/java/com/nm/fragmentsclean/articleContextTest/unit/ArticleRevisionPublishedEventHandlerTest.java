package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleRevisionPublishedEventHandler;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleProjectionRow;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleRevisionPublishedEventHandlerTest {

    private static final UUID ARTICLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID REVISION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID EVENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID COMMAND_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final Instant OCCURRED_AT = Instant.parse("2026-08-27T10:00:00Z");

    @Test
    void updates_mobile_projection_then_publishes_projection_freshness_signal() {
        var repository = new RecordingProjectionRepository();
        var publisher = new RecordingProjectionSyncPublisher();
        var handler = new ArticleRevisionPublishedEventHandler(repository, publisher);

        handler.handle(new ArticleRevisionPublishedIntegrationEvent(
                EVENT_ID, COMMAND_ID, ARTICLE_ID, REVISION_ID, 3L, OCCURRED_AT,
                OCCURRED_AT.minusSeconds(30)));

        assertThat(repository.publishedEvent.articleId()).isEqualTo(ARTICLE_ID);
        assertThat(publisher.event.eventName()).isEqualTo("projection.updated");
        assertThat(publisher.event.projection()).isEqualTo("articles");
        assertThat(publisher.event.entityId()).isEqualTo(ARTICLE_ID.toString());
        assertThat(publisher.event.version()).isEqualTo(3L);
        assertThat(publisher.event.hints()).containsExactly("content", "publicationStatus");
    }

    private static final class RecordingProjectionRepository implements ArticleProjectionRepository {
        private ArticleRevisionPublishedIntegrationEvent publishedEvent;

        @Override
        public void apply(ArticleCreatedEvent event) {
            throw new AssertionError("legacy projection path must not be used");
        }

        @Override
        public void apply(ArticleRevisionPublishedIntegrationEvent event) {
            this.publishedEvent = event;
        }

		@Override
		public void apply(com.nm.fragmentsclean.platform.eventing.contracts.ArticleArchivedIntegrationEvent event) {
			throw new AssertionError("archive projection path must not be used");
		}

        @Override
        public long count() {
            return 0;
        }

        @Override
        public void insertSeed(ArticleProjectionRow row) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class RecordingProjectionSyncPublisher implements ProjectionSyncPublisher {
        private ProjectionSyncEvent event;

        @Override
        public void publish(ProjectionSyncEvent event) {
            this.event = event;
        }
    }
}
