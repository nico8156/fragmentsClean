package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.OutboxStatus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxEventRepository;

import java.util.List;

public class JpaOutboxEventRepsitory implements OutboxEventRepository {

    private final SpringOutboxEventRepository repository;

    public JpaOutboxEventRepsitory(SpringOutboxEventRepository repository) {
        this.repository = repository;
    }
    @Override
    public List<OutboxEventJpaEntity> findTop100ByStatusOrderByIdAsc(OutboxStatus status) {
        return repository.findTop100ByStatusOrderByIdAsc(status);
    }

    @Override
    public List<OutboxEventJpaEntity> findTop50ByStatusOrderByIdAsc(OutboxStatus status) {
        return repository.findTop50ByStatusOrderByIdAsc(status);
    }
}
