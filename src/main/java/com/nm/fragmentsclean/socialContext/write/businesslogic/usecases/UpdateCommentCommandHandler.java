package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Comment;
import jakarta.transaction.Transactional;

@Transactional
public class UpdateCommentCommandHandler implements CommandHandler<UpdateCommentCommand> {

    private final CommentRepository commentRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;

    public UpdateCommentCommandHandler(CommentRepository commentRepository,
                                       DomainEventPublisher eventPublisher,
                                       DateTimeProvider dateTimeProvider) {
        this.commentRepository = commentRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void execute(UpdateCommentCommand cmd) {

        var now = dateTimeProvider.now();

        Comment comment = commentRepository.byId(cmd.commentId())
                .orElseThrow(() -> new IllegalStateException("Comment not found: " + cmd.commentId()));

        boolean changed = comment.applyBodyEdit(cmd.newBody(), now);

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

