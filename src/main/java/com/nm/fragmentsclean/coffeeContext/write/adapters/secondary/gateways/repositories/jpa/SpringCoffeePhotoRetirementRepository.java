package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.CoffeePhotoRetirementJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface SpringCoffeePhotoRetirementRepository extends JpaRepository<CoffeePhotoRetirementJpaEntity,UUID> {
    java.util.List<CoffeePhotoRetirementJpaEntity> findByCoffeeId(UUID coffeeId);
    boolean existsByCoffeeId(UUID coffeeId);
}
