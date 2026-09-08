package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

import java.time.Instant;
import java.util.List;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialSourceAccessMode;

/** Provider-neutral external discovery boundary. */
public interface EditorialSourceDiscoveryPort {
    EditorialSourceAccessMode accessMode();
    DiscoveryResult discover(String endpoint, String etag, String lastModified);
    record DiscoveryResult(String etag, String lastModified, boolean notModified, List<DiscoveredItem> items) {
        public DiscoveryResult {
            items = items == null ? List.of() : List.copyOf(items);
        }
        public static DiscoveryResult discovered(String etag, String lastModified, List<DiscoveredItem> items) {
            return new DiscoveryResult(etag, lastModified, false, items);
        }
        public static DiscoveryResult notModified(String etag, String lastModified) {
            return new DiscoveryResult(etag, lastModified, true, List.of());
        }
    }
    record DiscoveredItem(String externalId, String title, String summary, String url, String author, Instant publishedAt, String fingerprint) { }
}
