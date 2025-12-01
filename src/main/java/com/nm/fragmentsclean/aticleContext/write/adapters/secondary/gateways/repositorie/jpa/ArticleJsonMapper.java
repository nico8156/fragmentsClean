package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa;

import java.util.List;
import java.util.UUID;

public interface ArticleJsonMapper {

    String tagsToJson(List<String> tags);

    List<String> tagsFromJson(String json);

    String coffeeIdsToJson(List<UUID> ids);

    List<UUID> coffeeIdsFromJson(String json);
}
