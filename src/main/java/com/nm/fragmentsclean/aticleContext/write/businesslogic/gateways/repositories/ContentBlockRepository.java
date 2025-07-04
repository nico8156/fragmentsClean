package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ContentBlock;

import java.util.UUID;

public interface ContentBlockRepository {
    void save(ContentBlock contentBlock);
    void delete(UUID id);
    void update(ContentBlock contentBlock);
}
