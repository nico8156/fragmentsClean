package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringOutboxEventRepository extends JpaRepository<OutboxEventJpaEntity,Long> {
    List<OutboxEventJpaEntity> findTop100ByStatusOrderByIdAsc(OutboxStatus status);
    List<OutboxEventJpaEntity> findTop50ByStatusOrderByIdAsc(OutboxStatus status);

}
