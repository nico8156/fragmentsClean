package com.nm.fragmentsclean.coffeeContextTest.support;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionSource;
import java.util.*;
public class FakeCoffeePhotoProjectionSource implements CoffeePhotoProjectionSource {
 public final Map<UUID,Snapshot> snapshots=new HashMap<>();
 public Optional<Snapshot> current(UUID id){return Optional.ofNullable(snapshots.get(id));}
}
