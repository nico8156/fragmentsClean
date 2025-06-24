package com.nm.fragmentsclean.coffeeContext.read;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.QueryHandler;
import org.springframework.stereotype.Component;

@Component
public class GetCoffeesQueryHandler implements QueryHandler<GetCoffeesQuery, CoffeeListView> {
    private final CoffeeRepository coffeeRepository;

    public GetCoffeesQueryHandler(CoffeeRepository coffeeRepository) {
        this.coffeeRepository = coffeeRepository;
    }

    @Override
    public CoffeeListView handle(GetCoffeesQuery query) {
        var coffeeViews = coffeeRepository.findAll().stream().map(c->
                new CoffeeView(
                        c.toSnapshot().id(),
                        c.toSnapshot().googleId(),
                        c.toSnapshot().displayName(),
                        c.toSnapshot().formattedAddress(),
                        c.toSnapshot().nationalPhoneNumber(),
                        c.toSnapshot().websiteUri(),
                        c.toSnapshot().latitude(),
                        c.toSnapshot().longitude()
                )).toList();
        return new CoffeeListView(coffeeViews, coffeeViews.size());
    }
}
