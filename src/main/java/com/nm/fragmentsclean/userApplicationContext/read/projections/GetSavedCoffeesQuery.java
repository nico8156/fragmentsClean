package com.nm.fragmentsclean.userApplicationContext.read.projections;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

import java.util.UUID;

public record GetSavedCoffeesQuery(UUID userId) implements Query<SavedCoffeeListView> {
}
