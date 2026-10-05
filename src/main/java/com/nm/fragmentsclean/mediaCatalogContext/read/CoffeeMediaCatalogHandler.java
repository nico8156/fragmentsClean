package com.nm.fragmentsclean.mediaCatalogContext.read;
import com.nm.fragmentsclean.platform.eventing.contracts.*;
import java.time.Instant;
import java.util.*;
public final class CoffeeMediaCatalogHandler {
    private final CoffeeMediaCatalogProjection projection;
    public CoffeeMediaCatalogHandler(CoffeeMediaCatalogProjection projection){this.projection=projection;}
    public void handle(CoffeePhotoAddedIntegrationEvent e){projection.applyCoffee(e.coffeeId(),List.of(entry(e.coffeeId(),e.photoId(),e.photoUri(),e.version(),e.occurredAt(),false)),e.version(),e.occurredAt(),false,false);}
    public void handle(CoffeePhotoDeletedIntegrationEvent e){projection.applyCoffee(e.coffeeId(),List.of(entry(e.coffeeId(),e.photoId(),null,e.version(),e.occurredAt(),true)),e.version(),e.occurredAt(),false,false);}
    public void handle(CoffeePhotosImportedIntegrationEvent e){projection.applyCoffee(e.coffeeId(),e.photos().stream().map(p->entry(e.coffeeId(),p.photoId(),p.photoUri(),e.version(),e.occurredAt(),false)).toList(),e.version(),e.occurredAt(),true,false);}
    public void handle(CoffeePhotosArrangedIntegrationEvent e){projection.applyCoffee(e.coffeeId(),e.photos().stream().map(p->entry(e.coffeeId(),p.photoId(),p.photoUri(),e.version(),e.occurredAt(),false)).toList(),e.version(),e.occurredAt(),true,false);}
    public void handle(CoffeeMediaCatalogSnapshotIntegrationEvent e){
        var entries=new ArrayList<MediaCatalogEntry>();
        e.photos().forEach(p->entries.add(entry(e.coffeeId(),p.photoId(),p.photoUri(),e.version(),e.occurredAt(),false)));
        // resourceId remains the source owner; UNUSED is distinct from gallery association.
        e.retiredPhotos().forEach(p->entries.add(new MediaCatalogEntry("COFFEE",p.photoId(),e.coffeeId(),null,"DELETION_PENDING",null,null,0,null,null,null,e.occurredAt(),e.version())));
        projection.applyCoffee(e.coffeeId(),entries,e.version(),e.occurredAt(),true,false);
    }
    public void handle(CoffeeMediaCatalogSnapshotV3IntegrationEvent e){
        var entries=new ArrayList<MediaCatalogEntry>();e.photos().forEach(p->entries.add(entry(e.coffeeId(),p.photoId(),p.photoUri(),e.version(),e.occurredAt(),false)));
        e.retiredPhotos().forEach(p->entries.add(new MediaCatalogEntry("COFFEE",p.photoId(),e.coffeeId(),null,"DELETED".equals(p.status())?"DELETED":"DELETION_PENDING",null,null,0,null,null,null,e.occurredAt(),e.version(),"DELETED".equals(p.status()))));projection.applyCoffee(e.coffeeId(),entries,e.version(),e.occurredAt(),true,false);
    }
    public void deleted(CoffeeLifecycleIntegrationEvent e){projection.applyCoffee(e.coffeeId(),List.of(),e.version(),e.occurredAt(),true,true);}
    private MediaCatalogEntry entry(UUID coffee,UUID photo,String uri,long version,Instant at,boolean deleted){return new MediaCatalogEntry("COFFEE",photo,deleted?null:coffee,null,deleted?"DELETED":"AVAILABLE",deleted?null:uri,null,0,null,null,null,at,version);}
}
