package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.moderation;

import com.nm.fragmentsclean.sharedKernel.businesslogic.media.ImageContentAnalyzer;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods=false)
public class ImageModerationConfiguration {
  @Bean @Profile("!fake")
  ImageContentAnalyzer imageContentAnalyzer(
      @Value("${fragments.media.moderation.api-key:${OPENAI_API_KEY:}}") String key,
      @Value("${fragments.media.moderation.model:omni-moderation-latest}") String model) {
    var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build());
    factory.setReadTimeout(Duration.ofSeconds(15));
    return new OpenAiImageContentAnalyzer(RestClient.builder().baseUrl("https://api.openai.com")
        .requestFactory(factory).build(), key, model);
  }
  @Bean @Profile("fake") ImageContentAnalyzer fakeImageContentAnalyzer() { return image -> false; }
}
