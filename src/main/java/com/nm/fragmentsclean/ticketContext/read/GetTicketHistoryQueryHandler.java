package com.nm.fragmentsclean.ticketContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketHistoryPageView;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public final class GetTicketHistoryQueryHandler
        implements QueryHandler<GetTicketHistoryQuery, TicketHistoryPageView> {
    private static final String CURSOR_VERSION = "v1:";
    private final TicketHistoryReadRepository repository;

    public GetTicketHistoryQueryHandler(TicketHistoryReadRepository repository) {
        this.repository = repository;
    }

    @Override
    public TicketHistoryPageView handle(GetTicketHistoryQuery query) {
        if (query.limit() < 1 || query.limit() > 50) {
            throw new IllegalArgumentException("Ticket history limit must be between 1 and 50");
        }
        var slice = repository.pageByUserId(
                query.requesterId(), decode(query.cursor()), query.limit());
        String nextCursor = slice.nextBeforePosition() == null
                ? null
                : encode(slice.nextBeforePosition());
        return new TicketHistoryPageView(
                slice.entries().stream().map(entry -> entry.view()).toList(),
                nextCursor);
    }

    private Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (!decoded.startsWith(CURSOR_VERSION)) throw new IllegalArgumentException();
            long position = Long.parseLong(decoded.substring(CURSOR_VERSION.length()));
            if (position < 1) throw new IllegalArgumentException();
            return position;
        } catch (RuntimeException invalid) {
            throw new IllegalArgumentException("Invalid ticket history cursor");
        }
    }

    private String encode(long position) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (CURSOR_VERSION + position).getBytes(StandardCharsets.UTF_8));
    }
}
