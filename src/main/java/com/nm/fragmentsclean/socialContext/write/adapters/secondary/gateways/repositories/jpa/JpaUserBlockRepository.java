package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.UserBlockJpaEntity;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.UserBlockRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.UserBlock;
import java.util.Optional;
import java.util.UUID;

public final class JpaUserBlockRepository implements UserBlockRepository {
    private final SpringUserBlockRepository repository;
    public JpaUserBlockRepository(SpringUserBlockRepository repository) { this.repository = repository; }
    @Override public Optional<UserBlock> byUsers(UUID blockerId, UUID blockedUserId) {
        return repository.findByBlockerIdAndBlockedUserId(blockerId, blockedUserId).map(this::toDomain);
    }
    @Override public void save(UserBlock block) { repository.save(toJpa(block)); }
    private UserBlock toDomain(UserBlockJpaEntity e) {
        return UserBlock.fromSnapshot(new UserBlock.UserBlockSnapshot(e.getBlockId(), e.getBlockerId(),
                e.getBlockedUserId(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion()));
    }
    private UserBlockJpaEntity toJpa(UserBlock block) {
        var s = block.toSnapshot();
        return new UserBlockJpaEntity(s.blockId(), s.blockerId(), s.blockedUserId(), s.active(),
                s.createdAt(), s.updatedAt(), s.version());
    }
}
