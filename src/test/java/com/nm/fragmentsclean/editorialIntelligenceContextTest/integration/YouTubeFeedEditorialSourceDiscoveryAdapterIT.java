package com.nm.fragmentsclean.editorialIntelligenceContextTest.integration;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.youtube.YouTubeFeedEditorialSourceDiscoveryAdapter;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YouTubeFeedEditorialSourceDiscoveryAdapterIT {
    private HttpServer server;
    private final AtomicReference<String> ifNoneMatch = new AtomicReference<>();
    private final AtomicReference<Response> response = new AtomicReference<>();

    @BeforeEach void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/videos.xml", this::respond);
        server.start();
    }

    @AfterEach void stopServer() { server.stop(0); }

    @Test
    void maps_an_atom_video_entry_to_a_normalized_signal() {
        response.set(Response.ok("\"youtube-revision-2\"", feed()));

        var result = adapter().discover(endpoint(), "\"youtube-revision-1\"", null);

        assertThat(ifNoneMatch.get()).isEqualTo("\"youtube-revision-1\"");
        assertThat(result.etag()).isEqualTo("\"youtube-revision-2\"");
        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.externalId()).isEqualTo("video-42");
            assertThat(item.title()).isEqualTo("Comprendre l'extraction");
            assertThat(item.summary()).isEqualTo("Une explication détaillée.");
            assertThat(item.url()).isEqualTo("https://www.youtube.com/watch?v=video-42");
            assertThat(item.author()).isEqualTo("Coffee Channel");
            assertThat(item.publishedAt()).isEqualTo(Instant.parse("2026-09-08T09:30:00Z"));
            assertThat(item.fingerprint()).hasSize(64);
        });
    }

    @Test
    void recognizes_a_not_modified_feed_as_a_successful_empty_discovery() {
        response.set(Response.notModified("\"youtube-revision-2\""));

        var result = adapter().discover(endpoint(), "\"youtube-revision-1\"", null);

        assertThat(result.notModified()).isTrue();
        assertThat(result.items()).isEmpty();
    }

    @Test
    void rejects_a_doctype_payload_before_it_reaches_the_editorial_domain() {
        response.set(Response.ok(null, "<!DOCTYPE feed [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><feed><entry><title>&xxe;</title></entry></feed>"));

        assertThatThrownBy(() -> adapter().discover(endpoint(), null, null))
                .isInstanceOf(EditorialSourceDiscoveryException.class)
                .extracting(failure -> ((EditorialSourceDiscoveryException) failure).category())
                .isEqualTo(EditorialSourceDiscoveryException.Category.MALFORMED_PAYLOAD);
    }

    private YouTubeFeedEditorialSourceDiscoveryAdapter adapter() { return new YouTubeFeedEditorialSourceDiscoveryAdapter(HttpClient.newHttpClient()); }
    private String endpoint() { return "http://localhost:" + server.getAddress().getPort() + "/videos.xml"; }
    private void respond(HttpExchange exchange) throws IOException {
        ifNoneMatch.set(exchange.getRequestHeaders().getFirst("If-None-Match"));
        var current = response.get();
        if (current.etag() != null) exchange.getResponseHeaders().set("ETag", current.etag());
        byte[] body = current.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(current.status(), current.status() == 304 ? -1 : body.length);
        if (current.status() != 304) exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static String feed() {
        return """
                <feed xmlns="http://www.w3.org/2005/Atom" xmlns:yt="http://www.youtube.com/xml/schemas/2015">
                  <entry><yt:videoId>video-42</yt:videoId><title>Comprendre l'extraction</title>
                    <link rel="alternate" href="https://www.youtube.com/watch?v=video-42"/>
                    <content>Une explication détaillée.</content><published>2026-09-08T09:30:00Z</published>
                    <author><name>Coffee Channel</name></author>
                  </entry>
                </feed>
                """;
    }

    private record Response(int status, String etag, String body) {
        static Response ok(String etag, String body) { return new Response(200, etag, body); }
        static Response notModified(String etag) { return new Response(304, etag, ""); }
    }
}
