package com.nm.fragmentsclean.articleContextTest.endtoend;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class ArticleProjectionIT extends AbstractBaseE2E{

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ARTICLE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTHOR_ID  = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final String SLUG   = "quest-ce-que-le-cafe-de-specialite";
    private static final String LOCALE = "fr-FR";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SpringOutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventDispatcher outboxEventDispatcher;

    @BeforeEach
    void setup() {
        jdbcTemplate.update("DELETE FROM articles_projection");
        outboxEventRepository.deleteAll();
    }

    @Test
    void creating_article_populates_articles_projection_via_outbox_and_eventbus() throws Exception {
        // GIVEN : payload pour le create article (adapter à ton DTO si besoin)
        var clientAt = "2024-02-14T08:00:00Z";

        mockMvc.perform(
                        post("/api/articles")
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "commandId": "%s",
                                          "articleId": "%s",
                                          "authorId": "%s",
                                          "authorName": "Hélène Martin",
                                          "slug": "%s",
                                          "locale": "%s",
                                          "title": "Qu'est-ce que le café de spécialité ?",
                                          "intro": "Le café de spécialité désigne une approche où chaque étape vise l’excellence.",
                                          "blocksJson": "[{\\"heading\\":\\"Une histoire de terroir\\",\\"paragraph\\":\\"Un café de spécialité ne naît jamais par hasard.\\"}]",
                                          "conclusion": "Le café de spécialité n’est pas seulement un niveau de qualité.",
                                          "coverUrl": "https://images.unsplash.com/cafe.jpg",
                                          "coverWidth": 1600,
                                          "coverHeight": 1067,
                                          "coverAlt": "Branches de caféier chargées de cerises rouges.",
                                          "tags": ["Origines", "Qualité", "Terroir"],
                                          "readingTimeMin": 6,
                                          "coffeeIds": [],
                                          "at": "%s"
                                        }
                                        """.formatted(
                                                COMMAND_ID,
                                                ARTICLE_ID,
                                                AUTHOR_ID,
                                                SLUG,
                                                LOCALE,
                                                clientAt
                                        )
                                )
                )
                .andExpect(status().isAccepted());

        // WHEN : on déclenche manuellement le dispatcher outbox → eventBus → projection
        outboxEventDispatcher.dispatchPending();

        // THEN : la projection a bien été alimentée
        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList("SELECT * FROM articles_projection");

        assertThat(rows).hasSize(1);
        var row = rows.getFirst();

        assertThat(row.get("id")).isEqualTo(ARTICLE_ID);
        assertThat(row.get("slug")).isEqualTo(SLUG);
        assertThat(row.get("locale")).isEqualTo(LOCALE);
        assertThat(row.get("title")).isEqualTo("Qu'est-ce que le café de spécialité ?");
        assertThat(row.get("intro")).isEqualTo("Le café de spécialité désigne une approche où chaque étape vise l’excellence.");
        assertThat(row.get("author_id")).isEqualTo(AUTHOR_ID);
        assertThat(row.get("author_name")).isEqualTo("Hélène Martin");
        assertThat(row.get("reading_time_min")).isEqualTo(6);
        assertThat(row.get("status")).isEqualTo("published"); // si tu stockes en lowerCase
        // tu peux aussi vérifier blocks_json / cover_json / tags_json si tu veux aller plus loin
    }
}
