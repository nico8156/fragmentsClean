package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa;


import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.entities.ArticleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringArticleRepository extends JpaRepository<ArticleJpaEntity, UUID> {
}
