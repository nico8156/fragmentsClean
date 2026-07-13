package com.nm.fragmentsclean.ticketContext.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.ticketContext.read.GetUserEntitlementsQuery;
import com.nm.fragmentsclean.ticketContext.read.GetUserEntitlementsQueryHandler;
import com.nm.fragmentsclean.ticketContext.read.UserEntitlementsReadRepository;
import com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.controllers.ReadUserEntitlementsController;
import com.nm.fragmentsclean.ticketContext.read.projections.UserEntitlementsView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

class ReadUserEntitlementsControllerTest {

    @Test
    void returns_current_user_entitlements_snapshot() {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        QueryBus queryBus = new QueryBus();
        queryBus.registerQueryHandlers(List.of(new GetUserEntitlementsQueryHandler(new FixedRepository(
                new UserEntitlementsView(
                        userId,
                        3,
                        1,
                        0,
                        12L,
                        Instant.parse("2026-07-06T10:00:00Z"))))));
        ReadUserEntitlementsController controller = new ReadUserEntitlementsController(queryBus);

        var response = controller.get(jwt(userId));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(userId);
        assertThat(response.getBody().confirmedTickets()).isEqualTo(3);
        assertThat(response.getBody().publishedComments()).isEqualTo(1);
        assertThat(response.getBody().confirmedLikes()).isEqualTo(0);
        assertThat(response.getBody().counters().validatedTickets()).isEqualTo(3);
        assertThat(response.getBody().currentLevel().name()).isEqualTo("URBAN_EXPLORER");
        assertThat(response.getBody().levels()).hasSize(4);
        assertThat(response.getBody().levels().get(3).requirements().validatedTickets()).isNull();
        assertThat(response.getBody().rights()).isEmpty();
        assertThat(response.getBody().version()).isEqualTo(12L);
    }

    @Test
    void returns_unauthorized_without_jwt() {
        QueryBus queryBus = new QueryBus();
        ReadUserEntitlementsController controller = new ReadUserEntitlementsController(queryBus);

        var response = controller.get(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .issuedAt(Instant.parse("2026-07-06T09:00:00Z"))
                .expiresAt(Instant.parse("2026-07-06T10:00:00Z"))
                .build();
    }

    private record FixedRepository(UserEntitlementsView view) implements UserEntitlementsReadRepository {
        @Override
        public UserEntitlementsView findByUserId(UUID userId) {
            return view;
        }
    }
}
