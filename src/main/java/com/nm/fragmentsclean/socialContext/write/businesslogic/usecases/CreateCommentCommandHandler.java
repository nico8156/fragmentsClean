package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import jakarta.transaction.Transactional;

import java.util.Objects;

@Transactional
public class CreateCommentCommandHandler implements CommandHandler<CreateCommentCommand> {
	private final CommentRepository commentRepository;
	private final DomainEventPublisher eventPublisher;
	private final DateTimeProvider dateTimeProvider;

	public CreateCommentCommandHandler(CommentRepository commentRepository,
			DomainEventPublisher eventPublisher,
			DateTimeProvider dateTimeProvider) {
		this.commentRepository = commentRepository;
		this.eventPublisher = eventPublisher;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public void execute(CreateCommentCommand cmd) {
		var now = dateTimeProvider.now();

		// idempotence simple : si le commentaire existe déjà, on ne recrée pas
		var existing = commentRepository.byId(cmd.commentId());
		if (existing.isPresent()) {
			var snapshot = existing.get().toSnapshot();
			if (!snapshot.authorId().equals(cmd.authorId())
					|| !snapshot.targetId().equals(cmd.targetId())
					|| !Objects.equals(snapshot.parentId(), cmd.parentId())
					|| !snapshot.body().equals(cmd.body())) {
				throw new BusinessCommandRejectedException(
						"COMMENT_ID_CONFLICT", "Comment id belongs to another comment");
			}
			return;
		}
		if (cmd.body() == null || cmd.body().isBlank()) {
			throw new BusinessCommandRejectedException("COMMENT_BODY_EMPTY", "Comment body cannot be empty");
		}

		var comment = Comment.createNew(
				cmd.commentId(),
				cmd.targetId(),
				cmd.authorId(),
				cmd.parentId(),
				cmd.body(),
				now);

		commentRepository.save(comment);

		comment.registerCreatedEvent(
				cmd.commandId(),
				cmd.clientAt(),
				now);

		comment.domainEvents().forEach(eventPublisher::publish);
		comment.clearDomainEvents();
	}
}
