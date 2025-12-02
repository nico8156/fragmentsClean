package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

public record GetCoffeesQuery() implements Query<CoffeeListView> {
}
