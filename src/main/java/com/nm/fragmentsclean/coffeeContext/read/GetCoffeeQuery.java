package com.nm.fragmentsclean.coffeeContext.read;

import java.util.Optional;
import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

public record GetCoffeeQuery(UUID coffeeId) implements Query<Optional<CoffeeSummaryView>> {
}
