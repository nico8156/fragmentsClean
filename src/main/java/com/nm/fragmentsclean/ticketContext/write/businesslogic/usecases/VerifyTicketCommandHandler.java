package com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import jakarta.transaction.Transactional;

import java.util.Objects;

@Transactional
public class VerifyTicketCommandHandler implements CommandHandler<VerifyTicketCommand> {

    private final TicketRepository ticketRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;

    public VerifyTicketCommandHandler(TicketRepository ticketRepository,
                                      DomainEventPublisher eventPublisher,
                                      DateTimeProvider dateTimeProvider) {
        this.ticketRepository = ticketRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void execute(VerifyTicketCommand cmd) {
        var now = dateTimeProvider.now();

        // garde-fous (contract)
        if ((cmd.ocrText() == null || cmd.ocrText().isBlank())
                && (cmd.imageRef() == null || cmd.imageRef().isBlank())) {
            throw new IllegalArgumentException("VerifyTicket requires ocrText or imageRef");
        }

        // Idempotence: si ticket existe déjà, on ne le recrée pas.
        // On autorise par contre "markAnalyzing/enrich" si ticket pas terminal.
        var existingOpt = ticketRepository.byId(cmd.ticketId());

        final Ticket ticket;
        boolean changed;

        if (existingOpt.isPresent()) {
            ticket = existingOpt.get();

            // Optionnel: vérifier cohérence userId (anti-tamper)
            if (!Objects.equals(ticket.toSnapshot().userId(), cmd.userId())) {
                throw new IllegalStateException("Ticket userId mismatch");
            }

            changed = ticket.markAnalyzingIfPossible(cmd.ocrText(), cmd.imageRef(), now);
            if (changed) {
                ticketRepository.save(ticket);
            }

        } else {
            ticket = Ticket.createNewAnalyzing(
                    cmd.ticketId(),
                    cmd.userId(),
                    cmd.ocrText(),
                    cmd.imageRef(),
                    now
            );
            ticketRepository.save(ticket);
            changed = true;
        }

        // Event(s): au minimum, un event "accepted/analyzing" si changed
        // (si tu veux rester minimaliste, tu peux publier même si idempotent = false, mais je préfère éviter)
        if (changed) {
            ticket.registerVerifiedAcceptedEvent(
                    cmd.commandId(),
                    cmd.clientAt(),
                    now
            );

            ticket.domainEvents().forEach(eventPublisher::publish);
            ticket.clearDomainEvents();
        }

        // IMPORTANT:
        // La suite "OpenAI -> confirm/reject -> publish ack final" se fera dans un autre handler (async)
        // typiquement via un consumer/outbox/job qui prendra le ticket en ANALYZING et sortira CONFIRMED/REJECTED.
    }
}
