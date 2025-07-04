package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.Article;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.fake.FakeArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommand;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticlecCommandHandler;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class CreateArticleCommandHandlerTest {

    FakeArticleRepository articleRepository = new FakeArticleRepository();

    @Test
    public void shouldCreateAnArticle(){

        new CreateArticlecCommandHandler(articleRepository)
                .execute(new CreateArticleCommand("premier article de test!!!"));

        assertThat(articleRepository.articles.size()).isEqualTo(1);
        assertThat(articleRepository.articles.get(0).title()).isEqualTo("premier article de test!!!");
        assertThat(articleRepository.articles.get(0).contentBlocks().size()).isEqualTo(0);

        assertThat(articleRepository.articles.stream().map(Article::toSnapshot)).containsExactly(
                new Article.ArticleSnapshot(
                        "premier article de test!!!",
                        new ArrayList<>()
                )

        );
    }

    @Test
    public void shouldNotCreateAnArticleWhenTitleIsEmpty(){

        assertThatThrownBy(
                () -> new CreateArticlecCommandHandler(articleRepository)
                        .execute(new CreateArticleCommand(""))).isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Title must not be null");
    }
}
