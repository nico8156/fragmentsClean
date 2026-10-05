package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import java.time.Instant;
import java.util.*;
/** Local read-side snapshot; never a cross-context lookup. */
public interface CoffeePhotoProjectionSource {
 record Snapshot(UUID coffeeId,String publicationStatus,long version,Instant changedAt,List<CoffeePhotoView> photos){public Snapshot{photos=List.copyOf(photos);}}
 Optional<Snapshot> current(UUID coffeeId);
}
