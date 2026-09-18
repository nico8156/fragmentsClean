package com.nm.fragmentsclean.editorialIntelligenceContext.configuration;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.rss.RssEditorialSourceDiscoveryAdapter;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.youtube.YouTubeFeedEditorialSourceDiscoveryAdapter;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.http.BoundedEditorialHttpFetcher;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.http.PublicEditorialEndpointPolicy;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialAnalysisPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.DeterministicEditorialAnalysisAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ScheduledCommandOutcomePort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.CommandStatusScheduledOutcomeAdapter;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.CommandStatusRepository;

/** Wiring only: provider HTTP clients remain outside editorial application logic. */
@Configuration
public class EditorialIntelligenceConfiguration {
    @Bean ScheduledCommandOutcomePort scheduledCommandOutcomePort(CommandStatusRepository statuses) { return new CommandStatusScheduledOutcomeAdapter(statuses); }
    @Bean EditorialAnalysisPort editorialAnalysisPort() { return new DeterministicEditorialAnalysisAdapter(); }
    @Bean
    HttpClient editorialSourceHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Bean
    BoundedEditorialHttpFetcher boundedEditorialHttpFetcher(
            HttpClient editorialSourceHttpClient,
            @Value("${editorial.discovery.request-timeout-ms:10000}") long requestTimeoutMs,
            @Value("${editorial.discovery.max-body-bytes:1048576}") int maxBodyBytes,
            @Value("${editorial.discovery.explicit-host-allowlist:}") String explicitHostAllowlist) {
        Set<String> allowedHosts = Arrays.stream(explicitHostAllowlist.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        return new BoundedEditorialHttpFetcher(
                editorialSourceHttpClient,
                new PublicEditorialEndpointPolicy(allowedHosts),
                Duration.ofMillis(requestTimeoutMs),
                maxBodyBytes);
    }

    @Bean
    EditorialSourceDiscoveryPort rssEditorialSourceDiscoveryPort(
            BoundedEditorialHttpFetcher httpFetcher,
            @Value("${editorial.discovery.max-items:100}") int maxItems) {
        return new RssEditorialSourceDiscoveryAdapter(httpFetcher, maxItems);
    }

    @Bean
    EditorialSourceDiscoveryPort youTubeFeedEditorialSourceDiscoveryPort(
            BoundedEditorialHttpFetcher httpFetcher,
            @Value("${editorial.discovery.max-items:100}") int maxItems) {
        return new YouTubeFeedEditorialSourceDiscoveryAdapter(httpFetcher, maxItems);
    }
}
