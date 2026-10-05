package com.nm.fragmentsclean.coffeeContext.read;
import java.util.*;
import org.springframework.stereotype.Service;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
@Service
public final class AdminCoffeeMediaPreviewsQueryHandler implements QueryHandler<AdminCoffeeMediaPreviewsQuery,Map<UUID,String>> {
    private final AdminCoffeeMediaPreviewRepository repository;
    public AdminCoffeeMediaPreviewsQueryHandler(AdminCoffeeMediaPreviewRepository repository){this.repository=repository;}
    public Map<UUID,String> handle(AdminCoffeeMediaPreviewsQuery query){return repository.currentPreviews(query.mediaResources());}
}
