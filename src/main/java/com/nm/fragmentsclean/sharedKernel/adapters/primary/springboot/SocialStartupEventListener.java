package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot;


import com.nm.fragmentsclean.aticleContext.read.ListArticlesQueryHandler;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.GetLikeStatusQueryHandler;
import com.nm.fragmentsclean.socialContext.read.GetLikeSummaryQueryHandler;
import com.nm.fragmentsclean.socialContext.read.ListCommentsQueryHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.CommentRepository;
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
    private final GetLikeStatusQueryHandler getLikeStatusQueryHandler;
    private final ListCommentsQueryHandler listCommentsQueryHandler;
    private final ListArticlesQueryHandler listArticlesQueryHandler;
    private final CreateArticleCommandHandler createArticleCommandHandler;
    public SocialStartupEventListener(CommandBus commandBus,
                                      QuerryBus querryBus,
                                      MakeLikeCommandHandler makeLikeCommandHandler,
                                      CreateCommentCommandHandler createCommentCommandHandler,
                                      UpdateCommentCommandHandler updateCommentCommandHandler,
                                      DeleteCommentCommandHandler deleteCommentCommandHandler,
                                      GetLikeSummaryQueryHandler getLikeSummaryQueryHandler,
                                      GetLikeStatusQueryHandler getLikeStatusQueryHandler,
                                      ListCommentsQueryHandler listCommentsQueryHandler,
                                      ListArticlesQueryHandler listArticlesQueryHandler,
                                      CreateArticleCommandHandler createArticleCommandHandler
                                      ) {
        this.commandBus = commandBus;
        this.querryBus = querryBus;
        this.makeLikeCommandHandler = makeLikeCommandHandler;
        this.createCommentCommandHandler = createCommentCommandHandler;
        this.updateCommentCommandHandler = updateCommentCommandHandler;
        this.deleteCommentCommandHandler = deleteCommentCommandHandler;
        this.getLikeSummaryQueryHandler = getLikeSummaryQueryHandler;
        this.getLikeStatusQueryHandler = getLikeStatusQueryHandler;
        this.listCommentsQueryHandler = listCommentsQueryHandler;
        this.listArticlesQueryHandler = listArticlesQueryHandler;
        this.createArticleCommandHandler = createArticleCommandHandler;

    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        List<CommandHandler<?>> handlers = List.of(
                makeLikeCommandHandler,
                createCommentCommandHandler,
                updateCommentCommandHandler,
                deleteCommentCommandHandler,
                createArticleCommandHandler

        );
        commandBus.registerCommandHandlers(handlers);

        List<QueryHandler<?,?>> queryHandlers = List.of(
                getLikeSummaryQueryHandler,
                listCommentsQueryHandler,
                getLikeStatusQueryHandler,
                listArticlesQueryHandler
        );
        querryBus.registerQuerryHandlers(queryHandlers);
    }
}
