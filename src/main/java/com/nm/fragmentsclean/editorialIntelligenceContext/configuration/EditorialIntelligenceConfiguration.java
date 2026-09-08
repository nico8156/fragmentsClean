package com.nm.fragmentsclean.editorialIntelligenceContext.configuration;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.rss.RssEditorialSourceDiscoveryAdapter;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.youtube.YouTubeFeedEditorialSourceDiscoveryAdapter;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.time.Duration;

/** Wiring only: provider HTTP clients remain outside editorial application logic. */
@Configuration
public class EditorialIntelligenceConfiguration {
    @Bean
    HttpClient editorialSourceHttpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Bean
    EditorialSourceDiscoveryPort rssEditorialSourceDiscoveryPort(HttpClient editorialSourceHttpClient) {
        return new RssEditorialSourceDiscoveryAdapter(editorialSourceHttpClient);
    }

    @Bean
    EditorialSourceDiscoveryPort youTubeFeedEditorialSourceDiscoveryPort(HttpClient editorialSourceHttpClient) {
        return new YouTubeFeedEditorialSourceDiscoveryAdapter(editorialSourceHttpClient);
    }
}
