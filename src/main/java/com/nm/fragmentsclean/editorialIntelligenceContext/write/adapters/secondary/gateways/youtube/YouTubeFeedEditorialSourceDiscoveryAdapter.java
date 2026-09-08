package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.youtube;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryException;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSourceAccessMode;
import org.w3c.dom.Document;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** YouTube Atom feed ACL. It normalizes video entries without exposing Atom XML to the domain. */
public final class YouTubeFeedEditorialSourceDiscoveryAdapter implements EditorialSourceDiscoveryPort {
    private static final String ACCEPT = "application/atom+xml, application/xml;q=0.9, text/xml;q=0.8";
    private final HttpClient httpClient;

    public YouTubeFeedEditorialSourceDiscoveryAdapter(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override public EditorialSourceAccessMode accessMode() { return EditorialSourceAccessMode.YOUTUBE_FEED; }

    @Override
    public DiscoveryResult discover(String endpoint, String etag, String lastModified) {
        try {
            var response = httpClient.send(request(endpoint, etag, lastModified), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 304) {
                close(response.body());
                return DiscoveryResult.notModified(response.headers().firstValue("ETag").orElse(etag), response.headers().firstValue("Last-Modified").orElse(lastModified));
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                close(response.body());
                throw EditorialSourceDiscoveryException.remoteFailure("YouTube feed returned HTTP " + response.statusCode());
            }
            try (var body = response.body()) {
                return DiscoveryResult.discovered(response.headers().firstValue("ETag").orElse(etag),
                        response.headers().firstValue("Last-Modified").orElse(lastModified), parse(body));
            }
        } catch (EditorialSourceDiscoveryException failure) {
            throw failure;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw EditorialSourceDiscoveryException.remoteFailure("YouTube feed request interrupted", interrupted);
        } catch (Exception failure) {
            throw EditorialSourceDiscoveryException.remoteFailure("YouTube feed request failed", failure);
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
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("YouTube endpoint must use HTTP(S)");
            return uri;
        } catch (IllegalArgumentException invalid) {
            throw EditorialSourceDiscoveryException.malformedEndpoint("Invalid YouTube feed endpoint", invalid);
        }
    }

    private static List<DiscoveredItem> parse(InputStream input) {
        try {
            Document document = secureFactory().newDocumentBuilder().parse(input);
            var items = new ArrayList<DiscoveredItem>();
            var entries = document.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "entry");
            if (entries.getLength() == 0) entries = document.getElementsByTagName("entry");
            for (int index = 0; index < entries.getLength(); index++) {
                var entry = (Element) entries.item(index);
                var videoId = firstPresent(descendantText(entry, "videoId"), directText(entry, "id"));
                var title = directText(entry, "title");
                var url = alternateLink(entry, videoId);
                if (blank(videoId) || blank(title) || blank(url)) continue;
                var summary = firstPresent(directText(entry, "content"), descendantText(entry, "description"));
                var author = descendantText(entry, "name");
                var publishedAt = parseInstant(firstPresent(directText(entry, "published"), directText(entry, "updated")));
                items.add(new DiscoveredItem(videoId, title, summary, url, author, publishedAt,
                        fingerprint(videoId, title, summary, url, author, publishedAt)));
            }
            return List.copyOf(items);
        } catch (EditorialSourceDiscoveryException failure) {
            throw failure;
        } catch (Exception failure) {
            throw EditorialSourceDiscoveryException.malformedPayload("Malformed YouTube Atom payload", failure);
        }
    }

    private static DocumentBuilderFactory secureFactory() throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
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

    private static String alternateLink(Element entry, String videoId) {
        NodeList links = entry.getElementsByTagNameNS("http://www.w3.org/2005/Atom", "link");
        if (links.getLength() == 0) links = entry.getElementsByTagName("link");
        for (int index = 0; index < links.getLength(); index++) {
            var link = (Element) links.item(index);
            if ("alternate".equals(link.getAttribute("rel")) || blank(link.getAttribute("rel"))) {
                var href = link.getAttribute("href");
                if (!blank(href)) return href;
            }
        }
        return blank(videoId) ? null : "https://www.youtube.com/watch?v=" + videoId;
    }

    private static String directText(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE && matches(child, name)) return normalized(child.getTextContent());
        }
        return null;
    }

    private static String descendantText(Element parent, String name) {
        NodeList nodes = parent.getElementsByTagNameNS("*", name);
        if (nodes.getLength() > 0) return normalized(nodes.item(0).getTextContent());
        nodes = parent.getElementsByTagName(name);
        return nodes.getLength() == 0 ? null : normalized(nodes.item(0).getTextContent());
    }

    private static boolean matches(Node node, String name) { return name.equals(node.getLocalName()) || name.equals(node.getNodeName()) || node.getNodeName().endsWith(":" + name); }
    private static Instant parseInstant(String value) { try { return blank(value) ? null : Instant.parse(value); } catch (RuntimeException ignored) { return null; } }
    private static String fingerprint(String id, String title, String summary, String url, String author, Instant publishedAt) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(String.join("\n", id, title, nullable(summary), url, nullable(author), nullable(publishedAt)).getBytes(StandardCharsets.UTF_8))); }
        catch (Exception impossible) { throw new IllegalStateException("SHA-256 is not available", impossible); }
    }
    private static void close(InputStream body) { try { body.close(); } catch (Exception ignored) { } }
    private static String firstPresent(String first, String second) { return blank(first) ? second : first; }
    private static String normalized(String value) { return blank(value) ? null : value.trim(); }
    private static String nullable(Object value) { return value == null ? "" : value.toString(); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
