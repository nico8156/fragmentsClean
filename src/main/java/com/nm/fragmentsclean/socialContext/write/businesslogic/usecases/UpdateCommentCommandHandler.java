package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentContentPolicy;
import jakarta.transaction.Transactional;

@Transactional
public class UpdateCommentCommandHandler implements CommandHandler<UpdateCommentCommand> {

    private final CommentRepository commentRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;
    private final CommentContentPolicy contentPolicy;

    public UpdateCommentCommandHandler(CommentRepository commentRepository,
                                       DomainEventPublisher eventPublisher,
                                       DateTimeProvider dateTimeProvider,
                                       CommentContentPolicy contentPolicy) {
        this.commentRepository = commentRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
        this.contentPolicy = contentPolicy;
    }

    @Override
    public void execute(UpdateCommentCommand cmd) {

        var now = dateTimeProvider.now();

        Comment comment = commentRepository.byId(cmd.commentId())
                .orElseThrow(() -> new BusinessCommandRejectedException(
                        "COMMENT_NOT_FOUND", "Comment does not exist"));

        if (!comment.toSnapshot().authorId().equals(cmd.userId())) {
            throw new BusinessCommandRejectedException(
                    "COMMENT_NOT_OWNED", "Only the comment author can update it");
        }

        boolean changed = comment.applyBodyEdit(contentPolicy.validateAndNormalize(cmd.newBody()), now);

        // état persistant
        commentRepository.save(comment);

        if (changed) {
            comment.registerUpdatedEvent(
                    cmd.commandId(),
                    cmd.clientAt(),
                    now
            );
        }

        comment.domainEvents().forEach(eventPublisher::publish);
        comment.clearDomainEvents();
    }
}
