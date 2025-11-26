package com.nm.fragmentsclean.socialContextTest.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringLikeRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.LikeJpaEntity;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;
import com.nm.fragmentsclean.socialContextTest.integration.AbstractJpaIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like.LikeSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

public class JpaLikeRepositoryIT extends AbstractJpaIntegrationTest {

    private static final UUID LIKE_ID   = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");
    private static final UUID USER_ID   = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");
    private static final UUID TARGET_ID = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-3b3b3b3b3b3");

    @Autowired
    private LikeRepository likeRepository; // port DDD

    @Autowired
    private SpringLikeRepository springLikeRepository; // repo Spring Data brut

    @Test
    void can_save_a_like() {
        var now = Instant.parse("2024-01-01T10:00:00Z");

        var snapshot = new LikeSnapshot(
                LIKE_ID,
                USER_ID,
                TARGET_ID,
                true,
                now,
                1L
        );

        likeRepository.save(Like.fromSnapshot(snapshot));

        assertThat(springLikeRepository.findAll()).containsExactly(
                new LikeJpaEntity(
                        LIKE_ID,
                        USER_ID,
                        TARGET_ID,
                        true,
                        now,
                        1L
                )
        );
    }

    @Test
    void repositories_are_injected() {
        assertThat(likeRepository).isNotNull();
        assertThat(springLikeRepository).isNotNull();
    }
}
