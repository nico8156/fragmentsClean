package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListCoffeesQueryHandler implements QueryHandler<ListCoffeesQuery, List<CoffeeSummaryView>> {

    private final CoffeeProjectionRepository readRepository;

    public ListCoffeesQueryHandler(CoffeeProjectionRepository readRepository) {
        this.readRepository = readRepository;
    }

    @Override
    public List<CoffeeSummaryView> handle(ListCoffeesQuery query) {
        return readRepository.findAll();
    }
}
