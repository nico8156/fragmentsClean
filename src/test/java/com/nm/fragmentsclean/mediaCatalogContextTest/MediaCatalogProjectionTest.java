package com.nm.fragmentsclean.mediaCatalogContextTest;
import com.nm.fragmentsclean.mediaCatalogContext.read.*;
import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class MediaCatalogProjectionTest {
    private final FakeMediaCatalogProjection projection=new FakeMediaCatalogProjection();
    private final MediaCatalogProjectionHandler handler=new MediaCatalogProjectionHandler(projection);
    private final UUID media=UUID.randomUUID(), resource=UUID.randomUUID(), owner=UUID.randomUUID();
    private final Instant at=Instant.parse("2026-10-04T10:00:00Z");
    @Test void available_media_exposes_its_real_relationships_and_metadata() {
        handler.handle(event("AVAILABLE",owner,1));
        assertThat(projection.entries).hasSize(1);
        var entry=projection.entries.getFirst();
        assertThat(entry.origin()).isEqualTo("EXPERIENCE");
        assertThat(entry.resourceId()).isEqualTo(resource);
        assertThat(entry.ownerId()).isEqualTo(owner);
        assertThat(entry.size()).isEqualTo(1024);
        assertThat(entry.createdAt()).isNull(); // old events do not know upload date
    }
    @Test void deletion_scrubs_storage_and_relationships_even_without_an_owner() {
        handler.handle(event("DELETED",null,3));
        assertThat(projection.entries).hasSize(1);
        var entry=projection.entries.getFirst();
        assertThat(entry.status()).isEqualTo("DELETED");
        assertThat(entry.objectKey()).isNull();
        assertThat(entry.ownerId()).isNull();
        assertThat(entry.resourceId()).isNull();
    }
    private ExperienceIntegrationEvents.MediaChanged event(String status,UUID user,long version) {
        return new ExperienceIntegrationEvents.MediaChanged(UUID.randomUUID(),UUID.randomUUID(),media,resource,user,UUID.randomUUID(),status,"experiences/photo.jpg","image/jpeg",1024,640,480,"test",version,at,null);
    }
    private static final class FakeMediaCatalogProjection implements MediaCatalogProjection {
        final List<MediaCatalogEntry> entries=new ArrayList<>();
        public boolean apply(MediaCatalogEntry e){entries.add(e);return true;}
        public void erase(UUID owner){entries.removeIf(e->owner.equals(e.ownerId()));}
    }
}
