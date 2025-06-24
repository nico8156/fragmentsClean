package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.providers;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.providers.CoffeeFromGoogle;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.providers.GooglePlacesApi;

import java.util.List;
import java.util.UUID;

public class FakeGooglePlacesApi implements GooglePlacesApi {
    private final List<CoffeeFromGoogle> coffeeList = List.of(
            new CoffeeFromGoogle(
                    UUID.fromString("DB87BC49-1357-4512-A4F5-B0422C57222F"),
                    "fhsqmjkfh",
                    "un super café",
                    "24 rue de rennes 35000 Rennes",
                    "0612345678",
                    "http://www.unsupercafe.com",
                    132.96,
                    96.45,
                    List.of("https://fseffiozmjùfpojs.com", "https://fjsimoqfh.com"),
                    List.of("lundi ... ", "mardi ...", "mercredi ...", "jeudi ...", "vendredi ...", "samedi ..."," ....","dimanche ..." ))
            ,
            new CoffeeFromGoogle(
                    UUID.fromString("FBE5EB95-7B10-4D38-8D19-2C7D66547B98"),
                    "fmsqflhjk",
                    "wowcafe",
                    "51 rue leon bougeois 35200 Rennes",
                    "0215457896",
                    "https: un site canon",
                    145.65,
                    12.87,
                    List.of("https://fihpsomfhphoto.com", "https://une autre ohotos .com"),
                    List.of("lundi", "mardi", "mercrediw")
            )
    );

    @Override
    public List<CoffeeFromGoogle> searchCoffeesNearby(double latitude, double longitude) {
        return coffeeList;
    }

    @Override
    public List<CoffeeFromGoogle> getRealPhotosFromGoogleApi(List<CoffeeFromGoogle> coffeeFromGoogles) {
        return List.of();
    }
}
