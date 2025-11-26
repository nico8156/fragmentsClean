package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot;


import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.GetLikeSummaryQueryHandler;
import com.nm.fragmentsclean.socialContext.read.ListCommentsQueryHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.CreateCommentCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.DeleteCommentCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.MakeLikeCommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.UpdateCommentCommandHandler;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SocialStartupEventListener {
    private final CommandBus commandBus;
    private final QuerryBus querryBus;
    private final MakeLikeCommandHandler makeLikeCommandHandler;
    private final CreateCommentCommandHandler createCommentCommandHandler;
    private final UpdateCommentCommandHandler updateCommentCommandHandler;
    private final DeleteCommentCommandHandler deleteCommentCommandHandler;
    private final GetLikeSummaryQueryHandler getLikeSummaryQueryHandler;
    private final ListCommentsQueryHandler listCommentsQueryHandler;
    public SocialStartupEventListener(CommandBus commandBus,
                                      QuerryBus querryBus,
                                      MakeLikeCommandHandler makeLikeCommandHandler,
                                      CreateCommentCommandHandler createCommentCommandHandler,
                                      UpdateCommentCommandHandler updateCommentCommandHandler,
                                      DeleteCommentCommandHandler deleteCommentCommandHandler,
                                      GetLikeSummaryQueryHandler getLikeSummaryQueryHandler,
                                      ListCommentsQueryHandler listCommentsQueryHandler) {
        this.commandBus = commandBus;
        this.querryBus = querryBus;
        this.makeLikeCommandHandler = makeLikeCommandHandler;
        this.createCommentCommandHandler = createCommentCommandHandler;
        this.updateCommentCommandHandler = updateCommentCommandHandler;
        this.deleteCommentCommandHandler = deleteCommentCommandHandler;
        this.getLikeSummaryQueryHandler = getLikeSummaryQueryHandler;
        this.listCommentsQueryHandler = listCommentsQueryHandler;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        List<CommandHandler<?>> handlers = List.of(
                makeLikeCommandHandler,
                createCommentCommandHandler,
                updateCommentCommandHandler,
                deleteCommentCommandHandler

        );
        commandBus.registerCommandHandlers(handlers);

        List<QueryHandler<?,?>> queryHandlers = List.of(
                getLikeSummaryQueryHandler,
                listCommentsQueryHandler
        );
        querryBus.registerQuerryHandlers(queryHandlers);
    }
}
