package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment;
import jakarta.transaction.Transactional;

@Transactional
public class DeleteCommentCommandHandler implements CommandHandler<DeleteCommentCommand> {

    private final CommentRepository commentRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;
    private final CommandStatusRecorder commandStatusRecorder;

    public DeleteCommentCommandHandler(CommentRepository commentRepository,
                                       DomainEventPublisher eventPublisher,
                                       DateTimeProvider dateTimeProvider,
                                       CommandStatusRecorder commandStatusRecorder) {
        this.commentRepository = commentRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
        this.commandStatusRecorder = commandStatusRecorder;
    }

    @Override
    public void execute(DeleteCommentCommand cmd) {

        var now = dateTimeProvider.now();

        Comment comment = commentRepository.byId(cmd.commentId())
                .orElseThrow(() -> new IllegalStateException("Comment not found: " + cmd.commentId()));

        if (!comment.toSnapshot().authorId().equals(cmd.userId())) {
            throw new IllegalStateException("Only the comment author can delete it");
        }

        boolean changed = comment.softDelete(now);

        commentRepository.save(comment);

        if (changed) {
            comment.registerDeletedEvent(
                    cmd.commandId(),
                    cmd.clientAt(),
                    now
            );
        }

        comment.domainEvents().forEach(eventPublisher::publish);
        comment.clearDomainEvents();
        commandStatusRecorder.markApplied(
                cmd.commandId(),
                "Comment",
                cmd.commentId().toString(),
                "social.comment.deleted",
                now
        );
    }
}
