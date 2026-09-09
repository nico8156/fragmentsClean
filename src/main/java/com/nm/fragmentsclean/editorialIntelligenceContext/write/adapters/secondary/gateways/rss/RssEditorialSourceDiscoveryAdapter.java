package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.rss;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryException;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSourceAccessMode;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * RSS-specific anti-corruption adapter. It owns HTTP and XML concerns and
 * returns only provider-neutral discovered items to the editorial domain.
 */
public final class RssEditorialSourceDiscoveryAdapter implements EditorialSourceDiscoveryPort {
    private static final String ACCEPT = "application/rss+xml, application/xml;q=0.9, text/xml;q=0.8";

    private final HttpClient httpClient;

    public RssEditorialSourceDiscoveryAdapter(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public EditorialSourceAccessMode accessMode() {
        return EditorialSourceAccessMode.RSS;
    }

    @Override
    public DiscoveryResult discover(String endpoint, String etag, String lastModified) {
        var request = request(endpoint, etag, lastModified);
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 304) {
                close(response.body());
                return DiscoveryResult.notModified(response.headers().firstValue("ETag").orElse(etag),
                        response.headers().firstValue("Last-Modified").orElse(lastModified));
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                close(response.body());
                throw EditorialSourceDiscoveryException.remoteFailure("RSS returned HTTP " + response.statusCode());
            }
            try (var body = response.body()) {
                return DiscoveryResult.discovered(
                        response.headers().firstValue("ETag").orElse(etag),
                        response.headers().firstValue("Last-Modified").orElse(lastModified),
                        parse(body));
            }
        } catch (EditorialSourceDiscoveryException failure) {
            throw failure;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw EditorialSourceDiscoveryException.remoteFailure("RSS request interrupted", interrupted);
        } catch (Exception failure) {
            throw EditorialSourceDiscoveryException.remoteFailure("RSS request failed", failure);
        }
    }

    private static HttpRequest request(String endpoint, String etag, String lastModified) {
        var builder = HttpRequest.newBuilder(validHttpUri(endpoint)).header("Accept", ACCEPT).GET();
        if (etag != null && !etag.isBlank()) builder.header("If-None-Match", etag);
        if (lastModified != null && !lastModified.isBlank()) builder.header("If-Modified-Since", lastModified);
        return builder.build();
    }

    private static URI validHttpUri(String endpoint) {
        try {
            var uri = URI.create(endpoint);
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException("RSS endpoint must use HTTP(S)");
            }
            return uri;
        } catch (IllegalArgumentException invalid) {
            throw EditorialSourceDiscoveryException.malformedEndpoint("Invalid RSS endpoint", invalid);
        }
    }

    private static List<DiscoveredItem> parse(InputStream input) {
        try {
            var document = secureFactory().newDocumentBuilder().parse(input);
            var items = new ArrayList<DiscoveredItem>();
            var nodes = document.getElementsByTagName("item");
            for (int index = 0; index < nodes.getLength(); index++) {
                var item = (Element) nodes.item(index);
                var title = childText(item, "title");
                var url = childText(item, "link");
                var externalId = firstPresent(childText(item, "guid"), url);
                if (blank(externalId) || blank(title) || blank(url)) continue;

                var summary = firstPresent(childText(item, "description"), childText(item, "encoded"));
                var publishedAt = parsePublishedAt(childText(item, "pubDate"));
                var author = firstPresent(childText(item, "author"), childText(item, "creator"));
                items.add(new DiscoveredItem(externalId, title, summary, url, author, publishedAt,
                        fingerprint(externalId, title, summary, url, author, publishedAt)));
            }
            return List.copyOf(items);
        } catch (EditorialSourceDiscoveryException failure) {
            throw failure;
        } catch (Exception failure) {
            throw EditorialSourceDiscoveryException.malformedPayload("Malformed RSS payload", failure);
        }
    }

    private static DocumentBuilderFactory secureFactory() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static String childText(Element parent, String requestedName) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            var localName = node.getLocalName();
            var nodeName = node.getNodeName();
            if (requestedName.equals(localName) || requestedName.equals(nodeName)
                    || nodeName.endsWith(":" + requestedName)) {
                var text = node.getTextContent();
                return blank(text) ? null : text.trim();
            }
        }
        return null;
    }

    private static Instant parsePublishedAt(String raw) {
        if (blank(raw)) return null;
        try {
            return ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String fingerprint(String externalId, String title, String summary, String url, String author, Instant publishedAt) {
        try {
            var raw = String.join("\n", externalId, title, nullable(summary), url, nullable(author), nullable(publishedAt));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException("SHA-256 is not available", impossible);
        }
    }

    private static void close(InputStream body) {
        try {
            body.close();
        } catch (Exception ignored) {
            // Nothing useful can be done after a failed HTTP response.
        }
    }

    private static String firstPresent(String first, String second) { return blank(first) ? second : first; }
    private static String nullable(Object value) { return value == null ? "" : value.toString(); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
