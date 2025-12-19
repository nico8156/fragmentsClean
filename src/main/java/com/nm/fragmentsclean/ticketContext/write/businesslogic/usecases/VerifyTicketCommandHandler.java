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

        if ((cmd.ocrText() == null || cmd.ocrText().isBlank())
                && (cmd.imageRef() == null || cmd.imageRef().isBlank())) {
            throw new IllegalArgumentException("VerifyTicket requires ocrText or imageRef");
        }

        var existingOpt = ticketRepository.byId(cmd.ticketId());

        final Ticket ticket;
        boolean changed;

        if (existingOpt.isPresent()) {
            ticket = existingOpt.get();

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

        if (changed) {
            ticket.registerVerifiedAcceptedEvent(
                    cmd.commandId(),
                    cmd.clientAt(),
                    now
            );

            ticket.domainEvents().forEach(eventPublisher::publish);
            ticket.clearDomainEvents();
        }
    }
}
