package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.fake;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FakeTicketRepository implements TicketRepository {

    private final List<Ticket.TicketSnapshot> store = new ArrayList<>();

    @Override
    public Optional<Ticket> byId(UUID ticketId) {
        return store.stream()
                .filter(s -> s.ticketId().equals(ticketId))
                .findFirst()
                .map(Ticket::fromSnapshot);
    }

    @Override
    public void save(Ticket ticket) {
        var snap = ticket.toSnapshot();
        store.removeIf(s -> s.ticketId().equals(snap.ticketId()));
        store.add(snap);
    }

    public List<Ticket.TicketSnapshot> allSnapshots() {
        return List.copyOf(store);
    }
}
