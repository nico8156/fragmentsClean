package com.nm.fragmentsclean.coffeeContext.read;

import java.util.List;

public record CoffeeListView(List<CoffeeView> coffees, int total) {}
