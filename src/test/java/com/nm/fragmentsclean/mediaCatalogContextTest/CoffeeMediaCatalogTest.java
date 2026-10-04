package com.nm.fragmentsclean.mediaCatalogContextTest;
import com.nm.fragmentsclean.mediaCatalogContext.read.*;
import com.nm.fragmentsclean.platform.eventing.contracts.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
class CoffeeMediaCatalogTest {
    private final FakeCoffeeCatalog projection=new FakeCoffeeCatalog();
    private final CoffeeMediaCatalogHandler handler=new CoffeeMediaCatalogHandler(projection);
    private final UUID coffee=UUID.randomUUID(),photo=UUID.randomUUID();
    private final Instant at=Instant.parse("2026-10-04T10:00:00Z");
    @Test void added_photo_knows_its_coffee_but_does_not_invent_upload_metadata() {
        handler.handle(new CoffeePhotoAddedIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),coffee,photo,"s3://bucket/photo.jpg",1,at,null));
        var entry=projection.entries.getFirst();
        assertThat(entry.origin()).isEqualTo("COFFEE");
        assertThat(entry.resourceId()).isEqualTo(coffee);
        assertThat(entry.ownerId()).isNull();assertThat(entry.createdAt()).isNull();
        assertThat(entry.contentType()).isNull();assertThat(entry.size()).isZero();
        assertThat(projection.complete).isFalse();
    }
    @Test void an_import_is_the_complete_replacement_inventory_not_an_append() {
        handler.handle(new CoffeePhotosImportedIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),coffee,List.of(new CoffeePhotosImportedIntegrationEvent.PhotoReference(photo,"https://photo.test/image.jpg")),2,at,null));
        assertThat(projection.complete).isTrue();assertThat(projection.entries).hasSize(1);
        assertThat(projection.resource).isEqualTo(coffee);assertThat(projection.version).isEqualTo(2);
    }
    @Test void photo_and_parent_deletion_are_explicit_tombstones() {
        handler.handle(new CoffeePhotoDeletedIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),coffee,photo,3,at,null));
        assertThat(projection.entries.getFirst().status()).isEqualTo("DELETED");
        assertThat(projection.entries.getFirst().resourceId()).isNull();
        assertThat(projection.entries.getFirst().objectKey()).isNull();
        handler.deleted(new CoffeeLifecycleIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),coffee,4,at));
        assertThat(projection.deleted).isTrue();assertThat(projection.complete).isTrue();assertThat(projection.entries).isEmpty();
    }
    private static final class FakeCoffeeCatalog implements CoffeeMediaCatalogProjection {
        List<MediaCatalogEntry> entries=List.of();UUID resource;long version;boolean complete,deleted;
        public void applyCoffee(UUID id,List<MediaCatalogEntry> media,long version,Instant at,boolean complete,boolean deleted){this.resource=id;this.entries=media;this.version=version;this.complete=complete;this.deleted=deleted;}
    }
}
