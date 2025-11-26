package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringLikeRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.LikeJpaEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WriteLikeControllerIT extends AbstractBaseE2E {

    private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID LIKE_ID    = UUID.fromString("f47b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
    private static final UUID USER_ID    = UUID.fromString("d57b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
    private static final UUID TARGET_ID  = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpringLikeRepository springLikeRepository;

    @Autowired
    private DateTimeProvider dateTimeProvider;

    @BeforeEach
    void setup() {
        springLikeRepository.deleteAll();
        // pour d'autres parties du code qui utilisent DateTimeProvider
        ((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow =
                Instant.parse("2024-01-01T10:00:00Z");
    }

    @Test
    void can_set_like_active_true() throws Exception {
        var clientAt = "2024-01-01T09:00:00Z";

        mockMvc.perform(
                        post("/api/social/likes") // <-- mapping du controller
                                .contentType("application/json")
                                .content(
                                        """
                                        {
                                          "commandId": "%s",
                                          "likeId": "%s",
                                          "userId": "%s",
                                          "targetId": "%s",
                                          "value": true,
                                          "at": "%s"
                                        }
                                        """.formatted(
                                                COMMAND_ID,
                                                LIKE_ID,
                                                USER_ID,
                                                TARGET_ID,
                                                clientAt
                                        )
                                )
                )
                .andExpect(status().isAccepted()); // 202, comme dans ton controller

        var now = Instant.parse("2024-01-01T10:00:00Z");

        assertThat(springLikeRepository.findAll()).containsExactly(
                new LikeJpaEntity(
                        LIKE_ID,
                        USER_ID,
                        TARGET_ID,
                        true,
                        now,
                        1L  // version après un premier changement (à ajuster si ta logique diffère)
                )
        );
    }
}
