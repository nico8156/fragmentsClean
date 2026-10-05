package com.nm.fragmentsclean.coffeeContext.read;
import java.util.Optional;
import org.springframework.stereotype.Service;
@Service public class ReadCoffeeMediaLifecycleQueryHandler {
 private final CoffeeMediaLifecycleReadRepository repository;
 public ReadCoffeeMediaLifecycleQueryHandler(CoffeeMediaLifecycleReadRepository repository){this.repository=repository;}
 public Optional<CoffeeMediaLifecycleView> handle(ReadCoffeeMediaLifecycleQuery query){return repository.byId(query.mediaId());}
}
