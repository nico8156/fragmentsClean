package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleReviewApprovalTokenService;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

import jakarta.transaction.Transactional;

@Component
public final class ApproveArticlePublication {
	private final ArticleReviewApprovalTokenService approvalTokens;
	private final CommandBus commandBus;
	private final DateTimeProvider clock;

	public ApproveArticlePublication(
			ArticleReviewApprovalTokenService approvalTokens,
			CommandBus commandBus,
			DateTimeProvider clock) {
		this.approvalTokens = approvalTokens;
		this.commandBus = commandBus;
		this.clock = clock;
	}

	@Transactional
	public UUID execute(String token) {
		Instant now = clock.now();
		var approval = approvalTokens.validate(token, now);
		if (!approvalTokens.consume(approval.approvalId(), now)) {
			throw new IllegalArgumentException("Approval token is no longer active");
		}
		UUID commandId = UUID.randomUUID();
		commandBus.dispatch(new PublishArticleRevisionCommand(
				commandId, now, approval.articleId(), approval.revisionId()));
		return commandId;
	}
}
