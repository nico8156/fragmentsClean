package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoRetirementRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoRetirement;
import java.util.*;
public class FakeCoffeePhotoRetirementRepository implements CoffeePhotoRetirementRepository {
    public final Map<UUID,CoffeePhotoRetirement> items=new HashMap<>();
    public final Map<UUID,Long> currentCounts=new HashMap<>();
    public final Map<String,Long> referenceCounts=new HashMap<>();
    public Optional<UUID> ownerOf(UUID id){return byId(id).map(i->i.photo().coffeeId().value());}
    public List<UUID> pendingPurgeIds(int limit){return items.values().stream().filter(i->i.status().equals("DELETION_PENDING")).map(i->i.photo().id().value()).limit(limit).toList();}
    public long referencesTo(UUID id,List<String> refs){return currentPhotoCount(id)+refs.stream().mapToLong(r->referenceCounts.getOrDefault(r,0L)).sum()+items.values().stream().filter(i->!i.photo().id().value().equals(id)&&!i.status().equals("DELETED")&&refs.contains(i.photo().uri())).count();}
    public Optional<CoffeePhotoRetirement> byId(UUID id){return Optional.ofNullable(items.get(id));}
    public List<CoffeePhotoRetirement> byCoffee(UUID id){return items.values().stream().filter(i->i.photo().coffeeId().value().equals(id)).toList();}
    public void save(CoffeePhotoRetirement item){items.put(item.photo().id().value(),item);}
    public void remove(UUID id){items.remove(id);}
    public boolean hasForCoffee(UUID id){return items.values().stream().anyMatch(i->i.photo().coffeeId().value().equals(id));}
    public long currentPhotoCount(UUID id){return currentCounts.getOrDefault(id,0L);}
}
