package com.nm.fragmentsclean.articleContextTest.endtoend;

import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.SpringArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleStatus;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WriteArticleControllerIT extends AbstractBaseE2E {

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ARTICLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID AUTHOR_ID  = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID COFFEE_ID  = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringArticleRepository springArticleRepository;

    @Autowired
    private DateTimeProvider dateTimeProvider;


    @BeforeEach
    void setup() {
        springArticleRepository.deleteAll();

        // instant "serveur" utilisé par le domaine (DateTimeProvider)
        ((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow =
                Instant.parse("2024-01-01T10:00:00Z");
    }

    @Test
    void can_create_article() throws Exception {
        var clientAt = "2024-01-01T09:00:00Z";

        mockMvc.perform(
                        post("/api/articles") // mapping du ArticleWriteController
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "commandId": "%s",
                                          "articleId": "%s",
                                          "authorId": "%s",
                                          "authorName": "Jane Doe",
                                          "slug": "why-fragments-rock",
                                          "locale": "fr-FR",
                                          "title": "Pourquoi Fragments va changer ta façon de boire du café",
                                          "intro": "Intro de l’article",
                                          "blocksJson": "[{\\"heading\\":\\"Une sélection exigeante\\",\\"paragraph\\":\\"Nous filtrons les meilleurs coffee shops.\\"}]",
                                          "conclusion": "Conclusion de l’article",
                                          "coverUrl": "https://example.com/cover.jpg",
                                          "coverWidth": 1600,
                                          "coverHeight": 900,
                                          "coverAlt": "Une belle tasse de café",
                                          "tags": ["coffee", "guide"],
                                          "readingTimeMin": 5,
                                          "coffeeIds": ["%s"],
                                          "at": "%s"
                                        }
                                        """.formatted(
                                                COMMAND_ID,
                                                ARTICLE_ID,
                                                AUTHOR_ID,
                                                COFFEE_ID,
                                                clientAt
                                        )
                                )
                )
                .andExpect(status().isAccepted()); // comme pour le write comment

        var now = Instant.parse("2024-01-01T10:00:00Z"); // date "serveur"

        var entities = springArticleRepository.findAll();
        assertThat(entities).hasSize(1);

        var entity = entities.getFirst();

        // On ne teste pas ici le contenu exact du JSON (blocks/tags/coffeeIds),
        // juste les champs “simples” et le fait qu’ils soient bien remplis.
        assertThat(entity.getArticleId()).isEqualTo(ARTICLE_ID);
        assertThat(entity.getSlug()).isEqualTo("why-fragments-rock");
        assertThat(entity.getLocale()).isEqualTo("fr-FR");
        assertThat(entity.getAuthorId()).isEqualTo(AUTHOR_ID);
        assertThat(entity.getAuthorName()).isEqualTo("Jane Doe");
        assertThat(entity.getTitle()).isEqualTo("Pourquoi Fragments va changer ta façon de boire du café");
        assertThat(entity.getIntro()).isEqualTo("Intro de l’article");
        assertThat(entity.getConclusion()).isEqualTo("Conclusion de l’article");
        assertThat(entity.getCoverUrl()).isEqualTo("https://example.com/cover.jpg");
        assertThat(entity.getCoverWidth()).isEqualTo(1600);
        assertThat(entity.getCoverHeight()).isEqualTo(900);
        assertThat(entity.getCoverAlt()).isEqualTo("Une belle tasse de café");
        assertThat(entity.getReadingTimeMin()).isEqualTo(5);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getPublishedAt()).isEqualTo(now);
        assertThat(entity.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(entity.getVersion()).isEqualTo(0L);
    }
}
