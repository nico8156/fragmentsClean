package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.Article;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.Title;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;

import java.util.ArrayList;
import java.util.UUID;

public class CreateArticlecCommandHandler implements CommandHandler<CreateArticleCommand> {
    private final ArticleRepository articleRepository;

    public CreateArticlecCommandHandler(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    @Override
    public void execute(CreateArticleCommand command) {
        UUID id = UUID.randomUUID();
        Title title = new Title(command.title());
        ArrayList<UUID> contentBlocksIds = new ArrayList<>();

        Article article = new Article(id, title, contentBlocksIds);

        articleRepository.save(article);
    }
}
