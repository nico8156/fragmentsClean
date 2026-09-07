package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StudioOpenApiContractTest {
    private static final Path CONTRACT = Path.of("contracts/studio-api/v1/openapi.json");

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
}
