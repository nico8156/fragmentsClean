package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.ImageContentAnalyzer;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Provider DTOs are validated here; no raw payload, URL or credential is logged. */
public final class OpenAiImageContentAnalyzer implements ImageContentAnalyzer {
  private final RestClient client;
  private final String apiKey;
  private final String model;
  public OpenAiImageContentAnalyzer(RestClient client, String apiKey, String model) {
    this.client=client; this.apiKey=apiKey; this.model=model;
  }
  @Override public boolean flagged(URI image) {
    if (apiKey == null || apiKey.isBlank()) throw unavailable();
    try {
      String response = client.post().uri("/v1/moderations")
          .header("Authorization", "Bearer " + apiKey).contentType(MediaType.APPLICATION_JSON)
          .body(Map.of("model", model, "input", List.of(Map.of("type", "image_url", "image_url", Map.of("url", image.toString())))))
          .retrieve().body(String.class);
      JsonNode body = response == null ? null : new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
      if (body == null || !body.path("results").isArray() || body.path("results").size() != 1) throw unavailable();
      JsonNode result = body.path("results").get(0);
      if (!result.path("flagged").isBoolean() || !result.path("categories").isObject()
          || !result.path("category_scores").isObject() || !result.path("category_applied_input_types").isObject()) throw unavailable();
      // Require image coverage rather than interpreting missing/unsupported categories as safe.
      for (String category : List.of("sexual", "violence", "violence/graphic")) {
        var score = result.path("category_scores").path(category);
        var types = result.path("category_applied_input_types").path(category);
        boolean imageSupported = false;
        if (types.isArray()) for (var type : types) if ("image".equals(type.asText())) imageSupported=true;
        if (!result.path("categories").path(category).isBoolean() || !score.isNumber()
            || !Double.isFinite(score.asDouble()) || score.asDouble()<0 || score.asDouble()>1 || !imageSupported) throw unavailable();
      }
      boolean flagged = result.path("flagged").booleanValue();
      for (var category : result.path("categories")) {
        if (!category.isBoolean()) throw unavailable();
        flagged |= category.booleanValue();
      }
      return flagged;
    } catch (Exception failure) {
      // Includes 401/429/5xx/timeouts/invalid provider JSON; all remain retryable.
      throw unavailable();
    }
  }
  private static IllegalStateException unavailable() {
    return new IllegalStateException("Image moderation unavailable; publication remains pending");
  }
}
