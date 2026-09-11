package com.nm.fragmentsclean.ticketContext.unit;

import com.nm.fragmentsclean.ticketContext.read.GetTicketHistoryQuery;
import com.nm.fragmentsclean.ticketContext.read.GetTicketHistoryQueryHandler;
import com.nm.fragmentsclean.ticketContext.read.TicketHistoryReadRepository;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketHistoryEntry;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketHistoryItemView;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketHistorySlice;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetTicketHistoryQueryHandlerTest {
    private static final UUID USER_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

    @Test
    void exposes_an_opaque_cursor_without_leaking_internal_position() {
        var repository = new RecordingHistoryRepository();
        var handler = new GetTicketHistoryQueryHandler(repository);

        var first = handler.handle(new GetTicketHistoryQuery(USER_ID, null, 2));
        var second = handler.handle(new GetTicketHistoryQuery(USER_ID, first.nextCursor(), 2));

        assertThat(first.items()).extracting(TicketHistoryItemView::merchantName)
                .containsExactly("Recent", "Older");
        assertThat(first.nextCursor()).isNotBlank().doesNotContain("41");
        assertThat(repository.beforePositions).containsExactly(null, 41L);
        assertThat(second.nextCursor()).isNull();
    }

    @Test
    void rejects_an_invalid_or_unsupported_cursor() {
        var handler = new GetTicketHistoryQueryHandler(new RecordingHistoryRepository());

        assertThatThrownBy(() -> handler.handle(new GetTicketHistoryQuery(USER_ID, "not-a-cursor", 20)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid ticket history cursor");
    }

    private static final class RecordingHistoryRepository implements TicketHistoryReadRepository {
        private final java.util.ArrayList<Long> beforePositions = new java.util.ArrayList<>();

        @Override
        public TicketHistorySlice pageByUserId(UUID userId, Long beforePosition, int limit) {
            beforePositions.add(beforePosition);
            if (beforePosition != null) return new TicketHistorySlice(List.of(), null);
            return new TicketHistorySlice(List.of(
                    entry(42, "Recent"),
                    entry(41, "Older")), 41L);
        }

        private TicketHistoryEntry entry(long position, String merchant) {
            return new TicketHistoryEntry(position, new TicketHistoryItemView(
                    UUID.randomUUID(), "CONFIRMED", "APPROVED", 450, "EUR",
                    Instant.parse("2026-09-11T10:00:00Z"), merchant, null,
                    null, 1L, Instant.parse("2026-09-11T10:01:00Z")));
        }
    }
}
