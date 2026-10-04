package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoRetirementRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoRetirement;
import java.util.*;
public class FakeCoffeePhotoRetirementRepository implements CoffeePhotoRetirementRepository {
    public final Map<UUID,CoffeePhotoRetirement> items=new HashMap<>();
    public final Map<UUID,Long> currentCounts=new HashMap<>();
    public Optional<CoffeePhotoRetirement> byId(UUID id){return Optional.ofNullable(items.get(id));}
    public void save(CoffeePhotoRetirement item){items.put(item.photo().id().value(),item);}
    public void remove(UUID id){items.remove(id);}
    public boolean hasForCoffee(UUID id){return items.values().stream().anyMatch(i->i.photo().coffeeId().value().equals(id));}
    public long currentPhotoCount(UUID id){return currentCounts.getOrDefault(id,0L);}
}
