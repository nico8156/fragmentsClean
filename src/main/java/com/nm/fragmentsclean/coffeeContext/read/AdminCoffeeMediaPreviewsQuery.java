package com.nm.fragmentsclean.coffeeContext.read;
import java.util.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
public record AdminCoffeeMediaPreviewsQuery(Map<UUID,UUID> mediaResources) implements Query<Map<UUID,String>> {
    public AdminCoffeeMediaPreviewsQuery {mediaResources=Map.copyOf(mediaResources);if(mediaResources.size()>100)throw new IllegalArgumentException("At most 100 media per preview batch");}
}
