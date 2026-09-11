package com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketSubmissionFingerprintRegistry;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketSubmissionFingerprint;
import jakarta.transaction.Transactional;

import java.util.Objects;

@Transactional
public class VerifyTicketCommandHandler implements CommandHandler<VerifyTicketCommand> {

    private final TicketRepository ticketRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;
    private final TicketSubmissionFingerprintRegistry fingerprintRegistry;

    public VerifyTicketCommandHandler(TicketRepository ticketRepository,
                                      DomainEventPublisher eventPublisher,
                                      DateTimeProvider dateTimeProvider,
                                      TicketSubmissionFingerprintRegistry fingerprintRegistry) {
        this.ticketRepository = ticketRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
        this.fingerprintRegistry = fingerprintRegistry;
    }

    @Override
    public void execute(VerifyTicketCommand cmd) {
        var now = dateTimeProvider.now();

        if ((cmd.ocrText() == null || cmd.ocrText().isBlank())
                && (cmd.imageRef() == null || cmd.imageRef().isBlank())) {
            throw new BusinessCommandRejectedException(
                    "TICKET_CONTENT_REQUIRED", "Ticket requires OCR text or an image reference");
        }

        var existingOpt = ticketRepository.byId(cmd.ticketId());
        if (existingOpt.isPresent() && !Objects.equals(existingOpt.get().toSnapshot().userId(), cmd.userId())) {
            throw new BusinessCommandRejectedException(
                    "TICKET_ID_CONFLICT", "Ticket id belongs to another user");
        }
        TicketSubmissionFingerprint.fromOcr(cmd.ocrText()).ifPresent(fingerprint -> {
            if (!fingerprintRegistry.claim(fingerprint, cmd.ticketId(), cmd.userId(), now)) {
                throw new BusinessCommandRejectedException(
                        "TICKET_ALREADY_SUBMITTED", "This ticket has already been submitted");
            }
        });

        final Ticket ticket;
        boolean changed;

        if (existingOpt.isPresent()) {
            ticket = existingOpt.get();

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
