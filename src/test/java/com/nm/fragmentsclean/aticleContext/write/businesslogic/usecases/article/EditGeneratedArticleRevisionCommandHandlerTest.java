package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleAuthoringSagaRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.GeneratedArticleRevisionRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleContent;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleImageRef;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.ArticleEditorialTag;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleAuthoringSaga;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleAuthoringSagaState;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;

class EditGeneratedArticleRevisionCommandHandlerTest {
	@Test
	void editsOnlyTheSagaWorkingRevisionAndMovesToEditing() {
		Instant now = Instant.parse("2026-08-27T20:00:00Z");
		UUID sagaId = UUID.randomUUID();
		UUID articleId = UUID.randomUUID();
		UUID revisionId = UUID.randomUUID();
		var saga = readyForReviewSaga(sagaId, articleId, revisionId, now);
		var sagas = new SagaRepositoryFake(saga);
		var revisions = new RevisionRepositoryFake();
		var status = new CommandStatusRecorderFake();
		DomainEventPublisher events = event -> {};
		var handler = new EditGeneratedArticleRevisionCommandHandler(
				sagas, revisions, events, () -> now.plusSeconds(1), status);
		var cover = new EditGeneratedArticleRevisionCommand.Cover(
				"s3://bucket/cover", 1024, 1536, "Couverture");
		var sections = List.of(section("A"), section("B"), section("C"));

		handler.execute(new EditGeneratedArticleRevisionCommand(
				UUID.randomUUID(),
				now,
				sagaId,
				articleId,
				revisionId,
				"Titre",
				"Introduction",
				"Conclusion",
				cover,
				sections,
				List.of("decouverte")));

		assertTrue(revisions.called);
		assertEquals(ArticleAuthoringSagaState.EDITING, sagas.saga.snapshot().state());
		assertTrue(status.applied);
	}

	private static ArticleAuthoringSaga readyForReviewSaga(
			UUID sagaId,
			UUID articleId,
			UUID revisionId,
			Instant now) {
		var saga = ArticleAuthoringSaga.request(
				sagaId, articleId, revisionId, "Sujet", ArticleAuthoringTrigger.MANUAL, now);
		saga.enqueueGeneration(now);
		saga.claimGeneration("worker", now, now.plusSeconds(60));
		saga.startValidation(now);
		saga.markReadyForReview(now);
		return saga;
	}

	private static EditGeneratedArticleRevisionCommand.Section section(String heading) {
		return new EditGeneratedArticleRevisionCommand.Section(
				heading, "Paragraphe", "s3://bucket/" + heading, 1536, 1024, heading);
	}

	private static final class SagaRepositoryFake implements ArticleAuthoringSagaRepository {
		private ArticleAuthoringSaga saga;

		private SagaRepositoryFake(ArticleAuthoringSaga saga) {
			this.saga = saga;
		}

		@Override
		public Optional<ArticleAuthoringSaga> byId(UUID id) {
			return Optional.of(saga);
		}

		@Override
		public void save(ArticleAuthoringSaga saga) {
			this.saga = saga;
		}
	}

	private static final class RevisionRepositoryFake implements GeneratedArticleRevisionRepository {
		private boolean called;

		@Override
		public void replace(
				UUID articleId,
				UUID revisionId,
				ArticleContent content,
				ArticleImageRef cover,
				List<ArticleEditorialTag> tags,
				Instant now) {
			called = true;
		}
	}

	private static final class CommandStatusRecorderFake implements CommandStatusRecorder {
		private boolean applied;

		@Override
		public void markApplied(UUID commandId, String aggregateType, String aggregateId, String event, Instant at) {
			applied = true;
		}
	}
}
