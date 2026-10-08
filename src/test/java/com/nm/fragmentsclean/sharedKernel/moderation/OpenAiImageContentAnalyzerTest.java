package com.nm.fragmentsclean.sharedKernel.moderation;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.moderation.OpenAiImageContentAnalyzer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.*;
import java.net.URI;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.assertj.core.api.Assertions.*;
class OpenAiImageContentAnalyzerTest {
  final RestClient.Builder builder=RestClient.builder().baseUrl("https://api.openai.com");
  final MockRestServiceServer server=MockRestServiceServer.bindTo(builder).build();
  final OpenAiImageContentAnalyzer analyzer=new OpenAiImageContentAnalyzer(builder.build(),"test-secret","omni-moderation-latest");
  final URI image=URI.create("https://private.test/image.jpg?signature=test");
  String response(boolean flagged){return """
    {"results":[{"flagged":%s,"categories":{"sexual":false,"violence":false,"violence/graphic":false},
    "category_scores":{"sexual":0.01,"violence":0.02,"violence/graphic":0.01},
    "category_applied_input_types":{"sexual":["image"],"violence":["image"],"violence/graphic":["image"]}}]}
    """.formatted(flagged);}
  void reply(String body){server.expect(requestTo("https://api.openai.com/v1/moderations"))
    .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization","Bearer test-secret"))
    .andExpect(jsonPath("$.model").value("omni-moderation-latest"))
    .andExpect(jsonPath("$.input[0].image_url.url").value(image.toString()))
    .andRespond(withSuccess(body,MediaType.APPLICATION_JSON));}
  @Test void unflagged_image_with_supported_categories_passes(){reply(response(false));assertThat(analyzer.flagged(image)).isFalse();server.verify();}
  @Test void flagged_image_needs_review(){reply(response(true));assertThat(analyzer.flagged(image)).isTrue();}
  @Test void category_flag_is_conservative_even_if_global_flag_is_false(){reply(response(false).replace("\"sexual\":false","\"sexual\":true"));assertThat(analyzer.flagged(image)).isTrue();}
  @Test void missing_verdict_does_not_publish(){reply("{\"results\":[{}]}");assertThatThrownBy(()->analyzer.flagged(image)).isInstanceOf(IllegalStateException.class);}
  @Test void missing_image_coverage_does_not_publish(){reply(response(false).replace("[\"image\"]","[\"text\"]"));assertThatThrownBy(()->analyzer.flagged(image)).isInstanceOf(IllegalStateException.class);}
  @Test void invalid_score_does_not_publish(){reply(response(false).replace("0.01","2.0"));assertThatThrownBy(()->analyzer.flagged(image)).isInstanceOf(IllegalStateException.class);}
  @Test void rate_limit_is_retryable_without_exposing_provider_body(){server.expect(requestTo("https://api.openai.com/v1/moderations")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("secret provider details"));assertThatThrownBy(()->analyzer.flagged(image)).isInstanceOf(IllegalStateException.class).hasMessage("Image moderation unavailable; publication remains pending").hasNoCause();}
  @Test void missing_key_fails_closed_without_http_call(){var missing=new OpenAiImageContentAnalyzer(builder.build(),"","omni-moderation-latest");assertThatThrownBy(()->missing.flagged(image)).isInstanceOf(IllegalStateException.class);server.verify();}
}
