package com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;

import java.util.Optional;

public interface CoffeeRepository {

    void save(Coffee coffee);

    Optional<Coffee> findById(CoffeeId id);

    boolean existsByGooglePlaceId(GooglePlaceId googlePlaceId);
}
