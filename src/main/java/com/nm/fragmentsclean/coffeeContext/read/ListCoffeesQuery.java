package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

import java.util.List;

public record ListCoffeesQuery(boolean publishedOnly) implements Query<List<CoffeeSummaryView>> {
    public ListCoffeesQuery() { this(true); }
}
