package com.nm.fragmentsclean.articleContextTest.endtoend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.eventDispatcher.OutboxEventDispatcher;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
public class ArticleReadIT extends AbstractBaseE2E {

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ARTICLE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTHOR_ID  = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final String SLUG   = "quest-ce-que-le-cafe-de-specialite";
    private static final String LOCALE = "fr-FR";

    @Autowired
    ObjectMapper objectMapper;


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
    void creating_article_then_reading_by_slug_and_list() throws Exception {
        // GIVEN
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

        // WHEN : on déclenche la chaîne outbox → eventBus → projection
        outboxEventDispatcher.dispatchPending();

        // THEN 1 : GET /api/articles/{slug}?locale=fr-FR
        var result = mockMvc.perform(
                        get("/api/articles/{slug}", SLUG)
                                .param("locale", LOCALE)
                )
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();

// soit tu parses en JsonNode
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());


// soit tu mappes direct sur ta projection ArticleView
// ArticleView view = objectMapper.readValue(json, ArticleView.class);

// Exemple avec JsonNode :
        assertThat(root.path("id").asText()).isEqualTo(ARTICLE_ID.toString());
        assertThat(root.path("slug").asText()).isEqualTo(SLUG);
        assertThat(root.path("locale").asText()).isEqualTo(LOCALE);
        assertThat(root.path("title").asText()).isEqualTo("Qu'est-ce que le café de spécialité ?");
        assertThat(root.path("intro").asText()).isEqualTo("Le café de spécialité désigne une approche où chaque étape vise l’excellence.");
        assertThat(root.path("readingTimeMin").asInt()).isEqualTo(6);
        assertThat(root.path("status").asText()).isEqualTo("published");

// champs imbriqués
        JsonNode author = root.path("author");
        assertThat(author.path("id").asText()).isEqualTo(AUTHOR_ID.toString());
        assertThat(author.path("name").asText()).isEqualTo("Hélène Martin");

// tu peux même vérifier les blocks / cover / tags si tu veux :
        assertThat(root.path("blocks").isArray()).isTrue();
        assertThat(root.path("blocks")).hasSize(1);
        assertThat(root.path("blocks").get(0).path("heading").asText())
                .isEqualTo("Une histoire de terroir");

//                .andExpect(jsonPath("$.id").value(ARTICLE_ID.toString()))
//                .andExpect(jsonPath("$.slug").value(SLUG))
//                .andExpect(jsonPath("$.locale").value(LOCALE))
//                .andExpect(jsonPath("$.title").value("Qu'est-ce que le café de spécialité ?"))
//                .andExpect(jsonPath("$.intro").value("Le café de spécialité désigne une approche où chaque étape vise l’excellence."))
//                .andExpect(jsonPath("$.authorId").value(AUTHOR_ID.toString()))
//                .andExpect(jsonPath("$.authorName").value("Hélène Martin"))
//                .andExpect(jsonPath("$.readingTimeMin").value(6))
//                .andExpect(jsonPath("$.status").value("published"));

        // THEN 2 : GET /api/articles?locale=fr-FR&limit=10
        var mvcResult = mockMvc.perform(
                        get("/api/articles")
                                .param("locale", LOCALE)
                                .param("limit", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(ARTICLE_ID.toString()))
                .andExpect(jsonPath("$.items[0].slug").value(SLUG))
                .andExpect(jsonPath("$.items[0].locale").value(LOCALE))
                .andReturn();

        // Optionnel : tu peux aussi vérifier en base que la projection est là (sanity check)
        var rows = jdbcTemplate.queryForList("SELECT * FROM articles_projection");
        assertThat(rows).hasSize(1);
    }
}
