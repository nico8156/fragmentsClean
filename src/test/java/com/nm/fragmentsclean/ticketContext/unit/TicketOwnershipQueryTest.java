package com.nm.fragmentsclean.ticketContext.unit;

import com.nm.fragmentsclean.ticketContext.read.*;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketStatusView;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class TicketOwnershipQueryTest {
    @Test void queries_only_the_authenticated_users_ticket() {
        UUID ticket = UUID.randomUUID(), owner = UUID.randomUUID();
        TicketStatusReadRepository repository = new TicketStatusReadRepository() {
            public TicketStatusView findById(UUID id) { throw new AssertionError("Unscoped read"); }
            public List<TicketStatusView> list() { throw new AssertionError("Unscoped list"); }
            public TicketStatusView findByIdAndUserId(UUID id, UUID userId) {
                assertThat(id).isEqualTo(ticket);
                assertThat(userId).isEqualTo(owner);
                return null;
            }
        };
        assertThat(new GetTicketStatusQueryHandler(repository).handle(new GetTicketStatusQuery(ticket, owner))).isNull();
    }
}
