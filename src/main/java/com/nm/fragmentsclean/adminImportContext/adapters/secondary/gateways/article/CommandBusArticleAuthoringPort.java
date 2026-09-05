package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCommand;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleEditorialTag;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.PublishArticleRevisionCommand;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.ArchiveArticleCommand;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.SubmitArticleRevisionForReviewCommand;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.UpsertArticleDraftCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;

public class CommandBusArticleAuthoringPort implements ArticleAuthoringPort {
	private final CommandBus commandBus;

	public CommandBusArticleAuthoringPort(CommandBus commandBus) {
		this.commandBus = commandBus;
	}

	@Override
	public void saveDraft(StudioArticleCommand command) {
		var sections = command.blocks().stream().map(block -> {
			var section = ArticleSection.draft(block.heading())
					.withParagraph(ArticleParagraph.from(block.paragraph()));
			if (block.photo() != null) section = section.withImage(image(block.photo()));
			return section;
		}).toList();
		var content = ArticleContent.draft(ArticleTitle.from(command.title()),
				ArticleIntroduction.from(command.intro()), sections,
				ArticleParagraph.from(command.conclusion()));
		var cover = command.coverUrl() == null ? null : ArticleImageRef.from(
				command.coverUrl(), command.coverWidth(), command.coverHeight(), command.coverAlt());
		var draft = ArticleRevisionDraft.editable(content, cover,
				command.tags().stream().map(ArticleEditorialTag::fromProvider).toList());
		commandBus.dispatch(new UpsertArticleDraftCommand(
				command.commandId(),
				command.clientAt(),
				command.articleId(),
				command.revisionId(),
				command.slug(),
				command.locale(),
				command.authorId(),
				command.authorName(),
				draft
		));
	}

	@Override
	public void submitForReview(java.util.UUID commandId, java.time.Instant clientAt, java.util.UUID articleId) {
		commandBus.dispatch(new SubmitArticleRevisionForReviewCommand(commandId, clientAt, articleId));
	}

	@Override
	public void publish(java.util.UUID commandId, java.time.Instant clientAt,
			java.util.UUID articleId, java.util.UUID revisionId) {
		commandBus.dispatch(new PublishArticleRevisionCommand(commandId, clientAt, articleId, revisionId));
	}

	@Override
	public void archive(java.util.UUID commandId, java.time.Instant clientAt, java.util.UUID articleId) {
		commandBus.dispatch(new ArchiveArticleCommand(commandId, clientAt, articleId));
	}

	private static ArticleImageRef image(com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageRef image) {
		return ArticleImageRef.from(image.url(), image.width(), image.height(), image.alt());
	}
}
