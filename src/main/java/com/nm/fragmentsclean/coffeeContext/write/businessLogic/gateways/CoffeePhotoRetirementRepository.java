package com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoRetirement;
import java.util.Optional;
import java.util.UUID;
public interface CoffeePhotoRetirementRepository {
    Optional<UUID> ownerOf(UUID photoId);
    java.util.List<UUID> pendingPurgeIds(int limit);
    long referencesTo(UUID photoId,java.util.List<String> references);
    Optional<CoffeePhotoRetirement> byId(UUID photoId);
    java.util.List<CoffeePhotoRetirement> byCoffee(UUID coffeeId);
    void save(CoffeePhotoRetirement retirement);
    void remove(UUID photoId);
    boolean hasForCoffee(UUID coffeeId);
    long currentPhotoCount(UUID photoId);
}
