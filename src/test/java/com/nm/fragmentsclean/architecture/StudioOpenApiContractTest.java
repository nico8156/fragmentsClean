package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StudioOpenApiContractTest {
    private static final Path CONTRACT = Path.of("contracts/studio-api/v1/openapi.json");

    @Test void article_generation_contract_keeps_direction_optional_with_the_historical_default() throws Exception {
        JsonNode root = new ObjectMapper().readTree(Files.readString(CONTRACT));
        var schemas = root.path("components").path("schemas");
        var expected = java.util.Arrays.stream(com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleArtDirection.values()).map(Enum::name).toList();
        var values = new java.util.ArrayList<String>();
        schemas.path("ArticleArtDirection").path("enum").forEach(value -> values.add(value.asText()));
        assertThat(values).containsExactlyElementsOf(expected);
        assertThat(schemas.path("StudioArticleGenerationRequest").path("properties").path("artDirection").path("default").asText()).isEqualTo("ORIGINAL");
        assertThat(schemas.path("StudioArticleGenerationRequest").path("required").toString()).doesNotContain("artDirection");
        assertThat(root.path("paths").has("/api/admin/studio/article-generations")).isTrue();
    }

    @Test
    void experience_moderation_uses_its_domain_status_in_requests_and_audit() throws Exception {
        JsonNode schemas = new ObjectMapper().readTree(Files.readString(CONTRACT)).path("components").path("schemas");
        var expected = java.util.Arrays.stream(com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceModerationStatus.values())
                .map(Enum::name).toList();
        for (String schema : java.util.List.of("ModerateExperience", "ExperienceModerationAction")) {
            var values = new java.util.ArrayList<String>();
            schemas.path(schema).path("properties").path("decision").path("enum").forEach(value -> values.add(value.asText()));
            assertThat(values).containsExactlyInAnyOrderElementsOf(expected);
        }
        assertThat(schemas.path("ExperienceModerationReport").path("properties").path("actions").path("items").path("$ref").asText())
                .isEqualTo("#/components/schemas/ExperienceModerationAction");
    }

    @Test
    void coffee_admin_contract_keeps_lifecycle_and_command_fields_explicit() throws Exception {
        JsonNode root = new ObjectMapper().readTree(Files.readString(CONTRACT));
        assertThat(root.path("openapi").asText()).isEqualTo("3.1.0");
        assertThat(root.path("info").path("version").asText()).isEqualTo("1.0.0");
        assertThat(root.path("paths").has("/api/admin/coffees")).isTrue();
        assertThat(root.path("paths").has("/api/admin/commands/{commandId}")).isTrue();

        JsonNode coffee = root.path("components").path("schemas").path("AdminCoffee");
        assertThat(coffee.path("required").toString())
                .contains("\"publicationStatus\"", "\"version\"", "\"photos\"", "\"openingHours\"");
        assertThat(coffee.path("properties").path("publicationStatus").path("enum").toString())
                .isEqualTo("[\"DRAFT\",\"PUBLISHED\",\"ARCHIVED\"]");
        JsonNode status = root.path("components").path("schemas").path("CommandStatus");
        assertThat(status.path("properties").path("status").path("enum").toString())
                .isEqualTo("[\"PENDING\",\"APPLIED\",\"REJECTED\"]");
    }

    @Test
    void editorial_contract_exposes_retained_topic_handoff_and_durable_calendar() throws Exception {
        JsonNode root = new ObjectMapper().readTree(Files.readString(CONTRACT));
        assertThat(root.path("paths").has("/api/admin/editorial/topic-candidates/{candidateId}/start-authoring")).isTrue();
        assertThat(root.path("paths").has("/api/admin/editorial/sources/analysis")).isTrue();
        assertThat(root.path("paths").has("/api/admin/editorial/calendar")).isTrue();
        assertThat(root.path("paths").has("/api/admin/editorial/calendar/{scheduleId}/cancel")).isTrue();
        assertThat(root.path("components").path("schemas").path("EditorialCalendarItem")
                .path("properties").path("status").path("enum").toString())
                .contains("\"DISPATCHED\"", "\"REJECTED\"", "\"COMPLETED\"");
    }

    @Test
    void moderation_contract_exposes_queue_decision_and_auditable_shapes() throws Exception {
        JsonNode root = new ObjectMapper().readTree(Files.readString(CONTRACT));
        assertThat(root.path("paths").has("/api/admin/moderation/reports")).isTrue();
        assertThat(root.path("paths").has("/api/admin/moderation/reports/{reportId}/decision")).isTrue();
        assertThat(root.path("components").path("schemas").path("ModerationReport")
                .path("required").toString()).contains("\"reportCount\"", "\"actions\"");
        assertThat(root.path("components").path("schemas").path("ModerateComment")
                .path("properties").path("decision").path("enum").toString())
                .isEqualTo("[\"HIDDEN\",\"PUBLISHED\"]");
		assertThat(root.path("components").path("schemas").path("ExperienceModerationReport")
				.path("required").toString()).contains("\"media\"");
		assertThat(root.path("components").path("schemas").path("ExperienceMedia")
				.path("properties").has("url")).isTrue();
    }
}
