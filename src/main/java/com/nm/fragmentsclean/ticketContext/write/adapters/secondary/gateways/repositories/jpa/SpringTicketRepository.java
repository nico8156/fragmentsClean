package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jpa.entities.TicketJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringTicketRepository extends JpaRepository<TicketJpaEntity, UUID> {
}
