package com.nm.fragmentsclean.editorialIntelligenceContextTest.integration;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.rss.RssEditorialSourceDiscoveryAdapter;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.http.BoundedEditorialHttpFetcher;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.http.PublicEditorialEndpointPolicy;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RssEditorialSourceDiscoveryAdapterIT {
    private HttpServer server;
    private final AtomicReference<String> ifNoneMatch = new AtomicReference<>();
    private final AtomicReference<String> ifModifiedSince = new AtomicReference<>();
    private final AtomicReference<Response> response = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/feed", this::respond);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void maps_rss_to_normalized_items_and_reuses_http_checkpoints() {
        response.set(Response.ok("\"revision-2\"", "Tue, 08 Sep 2026 10:00:00 GMT", validFeed()));

        var result = adapter().discover(endpoint(), "\"revision-1\"", "Sun, 07 Sep 2026 10:00:00 GMT");

        assertThat(ifNoneMatch.get()).isEqualTo("\"revision-1\"");
        assertThat(ifModifiedSince.get()).isEqualTo("Sun, 07 Sep 2026 10:00:00 GMT");
        assertThat(result.notModified()).isFalse();
        assertThat(result.etag()).isEqualTo("\"revision-2\"");
        assertThat(result.lastModified()).isEqualTo("Tue, 08 Sep 2026 10:00:00 GMT");
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.externalId()).isEqualTo("article-42");
            assertThat(item.title()).isEqualTo("Une récolte plus résiliente");
            assertThat(item.summary()).isEqualTo("Un résumé éditorial.");
            assertThat(item.url()).isEqualTo("https://example.test/articles/42");
            assertThat(item.author()).isEqualTo("Fragments Research");
            assertThat(item.publishedAt()).isEqualTo(Instant.parse("2026-09-08T09:30:00Z"));
            assertThat(item.fingerprint()).hasSize(64);
        });
    }

    @Test
    void treats_not_modified_as_a_successful_empty_discovery() {
        response.set(Response.notModified("\"revision-2\"", "Tue, 08 Sep 2026 10:00:00 GMT"));

        var result = adapter().discover(endpoint(), "\"revision-1\"", "Sun, 07 Sep 2026 10:00:00 GMT");

        assertThat(result.notModified()).isTrue();
        assertThat(result.items()).isEmpty();
        assertThat(result.etag()).isEqualTo("\"revision-2\"");
    }

    @Test
    void rejects_a_payload_with_a_doctype_before_it_can_become_a_signal() {
        response.set(Response.ok(null, null, "<!DOCTYPE rss [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><rss><channel><item><title>&xxe;</title><link>https://example.test</link></item></channel></rss>"));

        assertThatThrownBy(() -> adapter().discover(endpoint(), null, null))
                .isInstanceOf(EditorialSourceDiscoveryException.class)
                .extracting(failure -> ((EditorialSourceDiscoveryException) failure).category())
                .isEqualTo(EditorialSourceDiscoveryException.Category.MALFORMED_PAYLOAD);
    }

    @Test
    void rejects_a_feed_larger_than_the_configured_http_body_limit() {
        response.set(Response.ok(null, null, validFeed()));

        assertThatThrownBy(() -> adapter(64, 10).discover(endpoint(), null, null))
                .isInstanceOf(EditorialSourceDiscoveryException.class)
                .extracting(failure -> ((EditorialSourceDiscoveryException) failure).category())
                .isEqualTo(EditorialSourceDiscoveryException.Category.REMOTE_FAILURE);
    }

    @Test
    void rejects_more_items_than_the_configured_feed_limit() {
        response.set(Response.ok(null, null, validFeed().replace("</channel>", validFeed().substring(validFeed().indexOf("<item>"), validFeed().indexOf("</item>") + 7) + "</channel>")));

        assertThatThrownBy(() -> adapter(16_384, 1).discover(endpoint(), null, null))
                .isInstanceOf(EditorialSourceDiscoveryException.class)
                .extracting(failure -> ((EditorialSourceDiscoveryException) failure).category())
                .isEqualTo(EditorialSourceDiscoveryException.Category.MALFORMED_PAYLOAD);
    }

    @Test
    void refuses_loopback_destinations_without_an_explicit_test_allowlist() {
        var fetcher = new BoundedEditorialHttpFetcher(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                new PublicEditorialEndpointPolicy(Set.of()), Duration.ofSeconds(1), 16_384);
        var adapter = new RssEditorialSourceDiscoveryAdapter(fetcher, 10);

        assertThatThrownBy(() -> adapter.discover(endpoint(), null, null))
                .isInstanceOf(EditorialSourceDiscoveryException.class)
                .extracting(failure -> ((EditorialSourceDiscoveryException) failure).category())
                .isEqualTo(EditorialSourceDiscoveryException.Category.MALFORMED_ENDPOINT);
    }

    @Test
    void refuses_redirects_instead_of_following_them_to_an_unchecked_destination() {
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", endpoint());
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> adapter().discover(
                        "http://localhost:" + server.getAddress().getPort() + "/redirect", null, null))
                .isInstanceOf(EditorialSourceDiscoveryException.class)
                .extracting(failure -> ((EditorialSourceDiscoveryException) failure).category())
                .isEqualTo(EditorialSourceDiscoveryException.Category.REMOTE_FAILURE);
    }

    @Test
    void enforces_a_deadline_while_waiting_for_the_remote_response() {
        server.createContext("/slow", exchange -> {
            try {
                byte[] body = validFeed().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().flush();
                Thread.sleep(250);
                exchange.getResponseBody().write(body);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });

        assertThatThrownBy(() -> adapter(16_384, 10, Duration.ofMillis(50)).discover(
                        "http://localhost:" + server.getAddress().getPort() + "/slow", null, null))
                .isInstanceOf(EditorialSourceDiscoveryException.class)
                .extracting(failure -> ((EditorialSourceDiscoveryException) failure).category())
                .isEqualTo(EditorialSourceDiscoveryException.Category.REMOTE_FAILURE);
    }

    private RssEditorialSourceDiscoveryAdapter adapter() {
        return adapter(16_384, 10);
    }

    private RssEditorialSourceDiscoveryAdapter adapter(int maxBodyBytes, int maxItems) {
        return adapter(maxBodyBytes, maxItems, Duration.ofSeconds(1));
    }

    private RssEditorialSourceDiscoveryAdapter adapter(int maxBodyBytes, int maxItems, Duration timeout) {
        var fetcher = new BoundedEditorialHttpFetcher(
                HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
                new PublicEditorialEndpointPolicy(Set.of("localhost")), timeout, maxBodyBytes);
        return new RssEditorialSourceDiscoveryAdapter(fetcher, maxItems);
    }

    private String endpoint() {
        return "http://localhost:" + server.getAddress().getPort() + "/feed";
    }

    private void respond(HttpExchange exchange) throws IOException {
        ifNoneMatch.set(exchange.getRequestHeaders().getFirst("If-None-Match"));
        ifModifiedSince.set(exchange.getRequestHeaders().getFirst("If-Modified-Since"));
        var current = response.get();
        if (current.etag() != null) exchange.getResponseHeaders().set("ETag", current.etag());
        if (current.lastModified() != null) exchange.getResponseHeaders().set("Last-Modified", current.lastModified());
        var body = current.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(current.status(), current.status() == 304 ? -1 : body.length);
        if (current.status() != 304) exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String validFeed() {
        return """
                <rss xmlns:dc="http://purl.org/dc/elements/1.1/"><channel><item>
                  <guid>article-42</guid><title>Une récolte plus résiliente</title>
                  <link>https://example.test/articles/42</link><description>Un résumé éditorial.</description>
                  <dc:creator>Fragments Research</dc:creator><pubDate>Tue, 08 Sep 2026 09:30:00 GMT</pubDate>
                </item></channel></rss>
                """;
    }

    private record Response(int status, String etag, String lastModified, String body) {
        static Response ok(String etag, String lastModified, String body) { return new Response(200, etag, lastModified, body); }
        static Response notModified(String etag, String lastModified) { return new Response(304, etag, lastModified, ""); }
    }
}
