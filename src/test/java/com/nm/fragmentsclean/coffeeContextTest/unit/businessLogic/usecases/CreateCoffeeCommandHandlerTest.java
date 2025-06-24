package com.nm.fragmentsclean.coffeeContextTest.unit.businessLogic.usecases;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.providers.FakeGooglePlacesApi;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.providers.CoffeeFromGoogle;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee.CoffeeSnapshot;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;


public class CreateCoffeeCommandHandlerTest {

    FakeCoffeeRepository coffeeRepository = new FakeCoffeeRepository();
    FakeGooglePlacesApi googlePlacesApi = new FakeGooglePlacesApi();

    @Test
    public void shouldCreateACoffee() {
        var uuid = UUID.randomUUID();
        var handler = new CreateCoffeeCommandHandler(coffeeRepository, googlePlacesApi);
        handler.execute(new CreateCoffeeCommand(141.14, 12.54));
        assertThat(coffeeRepository.coffeeList.stream()).size().isEqualTo(2);
        expectCoffee(
                new CoffeeSnapshot(
                    UUID.fromString("DB87BC49-1357-4512-A4F5-B0422C57222F"),
                    "fhsqmjkfh",
                    "un super café",
                    "24 rue de rennes 35000 Rennes",
                    "0612345678",
                    "http://www.unsupercafe.com",
                    132.96,
                    96.45)
                );
    }

    private void expectCoffee(CoffeeSnapshot... coffeeSnapshot) {
        assertThat(coffeeRepository.coffeeList.stream().map(
                Coffee::toSnapshot
        )).contains(coffeeSnapshot);
    }

    @Test
    public void shouldGetInfosFromGooglePlaces(){
        List<CoffeeFromGoogle> coffeeFromGoogles = googlePlacesApi.searchCoffeesNearby(141.14, 12.54);
        assertThat(coffeeFromGoogles.stream()).isNotEmpty();
        assertThat(coffeeFromGoogles.stream()).size().isEqualTo(2);
    }


}
