package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;

import java.util.HashSet;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleAuthoringSagaRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.GeneratedArticleRevisionRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleContent;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleDomainException;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleGeneratedRevisionEditedEvent;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleImageRef;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleIntroduction;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleParagraph;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleSection;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleTitle;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleEditorialTag;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringSagaState;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;

import jakarta.transaction.Transactional;

@Component
@Transactional
public class EditGeneratedArticleRevisionCommandHandler
		implements CommandHandler<EditGeneratedArticleRevisionCommand> {
	private final ArticleAuthoringSagaRepository sagas;
	private final GeneratedArticleRevisionRepository revisions;
	private final DomainEventPublisher events;
	private final DateTimeProvider clock;
	private final CommandStatusRecorder statuses;

	public EditGeneratedArticleRevisionCommandHandler(
			ArticleAuthoringSagaRepository sagas,
			GeneratedArticleRevisionRepository revisions,
			DomainEventPublisher events,
			DateTimeProvider clock,
			CommandStatusRecorder statuses) {
		this.sagas = sagas;
		this.revisions = revisions;
		this.events = events;
		this.clock = clock;
		this.statuses = statuses;
	}

	@Override
	public void execute(EditGeneratedArticleRevisionCommand command) {
		if (statuses.isApplied(command.commandId())) {
			return;
		}
		var saga = sagas.byId(command.sagaId()).orElseThrow();
		assertWorkingRevision(command, saga.snapshot().articleId(), saga.snapshot().revisionId());
		if (saga.snapshot().state() == ArticleAuthoringSagaState.READY_FOR_REVIEW) {
			saga.beginEditing(clock.now());
		} else if (saga.snapshot().state() != ArticleAuthoringSagaState.EDITING) {
			throw new IllegalStateException("Article is not editable from " + saga.snapshot().state());
		}

		var sections = command.sections().stream()
				.map(section -> ArticleSection.draft(section.heading())
						.withParagraph(ArticleParagraph.from(section.paragraph()))
						.withImage(ArticleImageRef.from(
								section.storageReference(),
								section.width(),
								section.height(),
								section.alt())))
				.toList();
		var content = ArticleContent.draft(
				ArticleTitle.from(command.title()),
				ArticleIntroduction.from(command.introduction()),
				sections,
				ArticleParagraph.from(command.conclusion()));
		content.validateForReview();
		var cover = ArticleImageRef.from(
				command.cover().storageReference(),
				command.cover().width(),
				command.cover().height(),
				command.cover().alt());
		var tags = command.tags().stream().map(ArticleEditorialTag::fromProvider).toList();
		if (tags.isEmpty() || tags.size() > 3 || new HashSet<>(tags).size() != tags.size()) {
			throw new ArticleDomainException("Un article contient entre 1 et 3 tags distincts.");
		}

		var now = clock.now();
		revisions.replace(command.articleId(), command.revisionId(), content, cover, tags, now);
		sagas.save(saga);
		events.publish(new ArticleGeneratedRevisionEditedEvent(
				UUID.randomUUID(),
				command.commandId(),
				command.sagaId(),
				command.articleId(),
				command.revisionId(),
				now,
				command.clientAt()));
		statuses.markApplied(
				command.commandId(),
				"Article",
				command.articleId().toString(),
				"article.generated_revision.edited",
				now);
	}

	private void assertWorkingRevision(
			EditGeneratedArticleRevisionCommand command,
			UUID articleId,
			UUID revisionId) {
		if (!articleId.equals(command.articleId()) || !revisionId.equals(command.revisionId())) {
			throw new IllegalArgumentException("Saga revision mismatch");
		}
	}
}
