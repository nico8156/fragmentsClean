package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.LikeJpaEntity;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;


public class JpaLikeRepository implements LikeRepository {

    private final SpringLikeRepository springLikeRepository;

    public JpaLikeRepository(SpringLikeRepository springLikeRepository) {
        this.springLikeRepository = springLikeRepository;
    }

    @Override
    public Optional<Like> byId(UUID likeId) {
        return springLikeRepository.findById(likeId).map(this::toDomain);
    }

    @Override
    public Optional<Like> byUserIdAndTargetId(UUID userId, UUID targetId) {
        return springLikeRepository.findByUserIdAndTargetId(userId, targetId).map(this::toDomain);
    }

    @Override
    public void save(Like like) {
        springLikeRepository.save(toJpa(like));
    }

    @Override
    public long countByTargetId(UUID targetId) {
        return springLikeRepository.countByTargetIdAndActiveIsTrue(targetId);
    }

    // ----- mapping -----

    private Like toDomain(LikeJpaEntity entity) {
        return Like.fromSnapshot(
                new Like.LikeSnapshot(
                        entity.getLikeId(),
                        entity.getUserId(),
                        entity.getTargetId(),
                        entity.isActive(),
                        entity.getUpdatedAt(),
                        entity.getVersion()
                )
        );
    }

    private LikeJpaEntity toJpa(Like like) {
        var snap = like.toSnapshot();
        return new LikeJpaEntity(
                snap.likeId(),
                snap.userId(),
                snap.targetId(),
                snap.active(),
                snap.updatedAt(),
                snap.version()
        );
    }
}
