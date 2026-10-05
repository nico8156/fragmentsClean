package com.nm.fragmentsclean.coffeeContext.read;
import java.util.Optional;
import java.util.UUID;
public interface CoffeeMediaLifecycleReadRepository { Optional<CoffeeMediaLifecycleView> byId(UUID mediaId); }
