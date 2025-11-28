package com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;

import java.util.List;

public interface OutboxEventRepository {
    List<OutboxEventJpaEntity> findTop100ByStatusOrderByIdAsc(OutboxStatus status);
    List<OutboxEventJpaEntity> findTop50ByStatusOrderByIdAsc(OutboxStatus status);
}
