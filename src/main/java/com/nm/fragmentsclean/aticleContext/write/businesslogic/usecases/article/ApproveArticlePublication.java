package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleReviewApprovalValidator;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleAuthoringSagaRepository;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

import jakarta.transaction.Transactional;

@Component
public final class ApproveArticlePublication {
	private final ArticleReviewApprovalValidator approvalTokens;
	private final CommandBus commandBus;
	private final DateTimeProvider clock;
	private final ArticleAuthoringSagaRepository sagas;

	public ApproveArticlePublication(
			ArticleReviewApprovalValidator approvalTokens,
			CommandBus commandBus,
			DateTimeProvider clock,
			ArticleAuthoringSagaRepository sagas) {
		this.approvalTokens = approvalTokens;
		this.commandBus = commandBus;
		this.clock = clock;
		this.sagas = sagas;
	}

	@Transactional
	public UUID execute(String token) {
		Instant now = clock.now();
		var approval = approvalTokens.validate(token, now);
		if (!approvalTokens.consume(approval.approvalId(), now)) {
			throw new IllegalArgumentException("Approval token is no longer active");
		}
		var saga = sagas.byId(approval.sagaId())
				.orElseThrow(() -> new IllegalArgumentException("Article authoring saga is missing"));
		var snapshot = saga.snapshot();
		if (!snapshot.articleId().equals(approval.articleId()) || !snapshot.revisionId().equals(approval.revisionId())) {
			throw new IllegalArgumentException("Approval does not match the article authoring saga");
		}
		saga.requestPublication(now);
		commandBus.dispatch(new SubmitArticleRevisionForReviewCommand(
				UUID.randomUUID(), now, approval.articleId()));
		UUID commandId = UUID.randomUUID();
		commandBus.dispatch(new PublishArticleRevisionCommand(
				commandId, now, approval.articleId(), approval.revisionId()));
		saga.markPublished(now);
		sagas.save(saga);
		return commandId;
	}
}
