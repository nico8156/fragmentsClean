package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Query;

public record GetCoffeesQuery() implements Query<CoffeeListView> {
}
