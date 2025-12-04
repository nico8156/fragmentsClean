package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration;


import com.nm.fragmentsclean.aticleContext.read.GetArticleBySlugQueryHandler;
import com.nm.fragmentsclean.aticleContext.read.ListArticlesQueryHandler;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleCreatedEventHandler;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommandHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeCreatedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.ListCoffeesQueryHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.EventBus;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QuerryBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.GetLikeStatusQueryHandler;
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
    private final EventBus eventBus;
    private final MakeLikeCommandHandler makeLikeCommandHandler;
    private final CreateCommentCommandHandler createCommentCommandHandler;
    private final UpdateCommentCommandHandler updateCommentCommandHandler;
    private final DeleteCommentCommandHandler deleteCommentCommandHandler;
    private final GetLikeSummaryQueryHandler getLikeSummaryQueryHandler;
    private final GetLikeStatusQueryHandler getLikeStatusQueryHandler;
    private final ListCommentsQueryHandler listCommentsQueryHandler;
    private final ListArticlesQueryHandler listArticlesQueryHandler;
    private final CreateArticleCommandHandler createArticleCommandHandler;
    private final ArticleCreatedEventHandler articleCreatedProjectionHandler;
    private final GetArticleBySlugQueryHandler getArticleBySlugQueryHandler;
    private final CreateCoffeeCommandHandler createCoffeeCommandHandler;
    private final CoffeeCreatedEventHandler coffeeCreatedProjectionHandler;
    private final ListCoffeesQueryHandler listCoffeesQueryHandler;

    public SocialStartupEventListener(CommandBus commandBus,
                                      QuerryBus querryBus,
                                      EventBus eventBus,
                                      MakeLikeCommandHandler makeLikeCommandHandler,
                                      CreateCommentCommandHandler createCommentCommandHandler,
                                      UpdateCommentCommandHandler updateCommentCommandHandler,
                                      DeleteCommentCommandHandler deleteCommentCommandHandler,
                                      GetLikeSummaryQueryHandler getLikeSummaryQueryHandler,
                                      GetLikeStatusQueryHandler getLikeStatusQueryHandler,
                                      ListCommentsQueryHandler listCommentsQueryHandler,
                                      ListArticlesQueryHandler listArticlesQueryHandler,
                                      CreateArticleCommandHandler createArticleCommandHandler,
                                      ArticleCreatedEventHandler articleCreatedProjectionHandler,
                                      GetArticleBySlugQueryHandler getArticleBySlugQueryHandler,
                                      CreateCoffeeCommandHandler createCoffeeCommandHandler,
                                      CoffeeCreatedEventHandler coffeeCreatedProjectionHandler,
                                      ListCoffeesQueryHandler listCoffeesQueryHandler
    ) {
        this.commandBus = commandBus;
        this.querryBus = querryBus;
        this.eventBus = eventBus;
        this.makeLikeCommandHandler = makeLikeCommandHandler;
        this.createCommentCommandHandler = createCommentCommandHandler;
        this.updateCommentCommandHandler = updateCommentCommandHandler;
        this.deleteCommentCommandHandler = deleteCommentCommandHandler;
        this.getLikeSummaryQueryHandler = getLikeSummaryQueryHandler;
        this.getLikeStatusQueryHandler = getLikeStatusQueryHandler;
        this.listCommentsQueryHandler = listCommentsQueryHandler;
        this.listArticlesQueryHandler = listArticlesQueryHandler;
        this.createArticleCommandHandler = createArticleCommandHandler;
        this.articleCreatedProjectionHandler = articleCreatedProjectionHandler;
        this.getArticleBySlugQueryHandler = getArticleBySlugQueryHandler;
        this.createCoffeeCommandHandler = createCoffeeCommandHandler;
        this.coffeeCreatedProjectionHandler = coffeeCreatedProjectionHandler;
        this.listCoffeesQueryHandler = listCoffeesQueryHandler;
    }

    @EventListener
    public void onApplicationReady(ApplicationReadyEvent event) {
        List<CommandHandler<?>> handlers = List.of(
                makeLikeCommandHandler,
                createCommentCommandHandler,
                updateCommentCommandHandler,
                deleteCommentCommandHandler,
                createArticleCommandHandler,
                createCoffeeCommandHandler

        );
        commandBus.registerCommandHandlers(handlers);

        List<QueryHandler<?,?>> queryHandlers = List.of(
                getLikeSummaryQueryHandler,
                listCommentsQueryHandler,
                getLikeStatusQueryHandler,
                listArticlesQueryHandler,
                getArticleBySlugQueryHandler,
                listCoffeesQueryHandler
        );
        querryBus.registerQuerryHandlers(queryHandlers);

        List<EventHandler<?>> eventHandlers = List.of(
                articleCreatedProjectionHandler,
                coffeeCreatedProjectionHandler
        );
        eventBus.registerEventHandlers(eventHandlers);
    }
}
