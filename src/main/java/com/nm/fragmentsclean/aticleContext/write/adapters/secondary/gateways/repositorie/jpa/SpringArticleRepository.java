package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa;


import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.entities.ArticleJpaEntity;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringArticleRepository extends JpaRepository<ArticleJpaEntity, UUID> {
    List<ArticleJpaEntity> findByStatus(ArticleStatus status);

}
