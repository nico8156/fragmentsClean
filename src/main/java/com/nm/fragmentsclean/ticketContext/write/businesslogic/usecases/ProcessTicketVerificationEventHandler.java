package com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Transactional
public class ProcessTicketVerificationEventHandler implements EventHandler<TicketVerifyAcceptedEvent> {

	private final TicketRepository ticketRepository;
	private final TicketVerificationProvider provider;
	private final DomainEventPublisher eventPublisher;
	private final DateTimeProvider dateTimeProvider;

	public ProcessTicketVerificationEventHandler(
			TicketRepository ticketRepository,
			TicketVerificationProvider provider,
			DomainEventPublisher eventPublisher,
			DateTimeProvider dateTimeProvider) {
		this.ticketRepository = ticketRepository;
		this.provider = provider;
		this.eventPublisher = eventPublisher;
		this.dateTimeProvider = dateTimeProvider;
	}

	public void handle(TicketVerifyAcceptedEvent evt) {
		var now = dateTimeProvider.now();

		var ticket = ticketRepository.byId(evt.ticketId())
				.orElseThrow(() -> new IllegalStateException("Ticket not found: " + evt.ticketId()));

		if (!Objects.equals(ticket.toSnapshot().userId(), evt.userId())) {
			throw new IllegalStateException("Ticket userId mismatch");
		}

		// Idempotence: si terminal, on ignore (ou tu peux publier un "completed"
		// idempotent)
		var snap = ticket.toSnapshot();
		if (snap.status() == Ticket.TicketStatus.CONFIRMED || snap.status() == Ticket.TicketStatus.REJECTED) {
			return;
		}

		// Appel provider (OpenAI)
		var result = provider.verify(evt.ocrText(), evt.imageRef());

		TicketVerificationCompletedEvent completed = switch (result) {

			case TicketVerificationProvider.Approved ok -> {
				var approved = new Ticket.ConfirmResult(
						ok.amountCents(),
						ok.currency(),
						ok.ticketDate(),
						ok.merchantName(),
						ok.merchantAddress(),
						ok.paymentMethod(),
						toDomainLineItems(ok.lineItems()));

				ticket.confirm(approved, now);
				ticketRepository.save(ticket);

				yield new TicketVerificationCompletedEvent(
						UUID.randomUUID(),
						evt.commandId(),
						evt.ticketId(),
						evt.userId(),
						TicketVerificationCompletedEvent.Outcome.APPROVED,
						ticket.toSnapshot().version(),
						now,
						evt.clientAt(),
						new TicketVerificationCompletedEvent.Approved(
								ok.amountCents(),
								ok.currency(),
								ok.ticketDate(),
								ok.merchantName(),
								ok.merchantAddress(),
								ok.paymentMethod(),
								toDomainLineItems(ok.lineItems())),
						null,
						"ticketEngine",
						ok.providerTraceId());
			}

			case TicketVerificationProvider.Rejected rej -> {
				ticket.reject(rej.reasonCode(), now);
				ticketRepository.save(ticket);

				yield new TicketVerificationCompletedEvent(
						UUID.randomUUID(),
						evt.commandId(),
						evt.ticketId(),
						evt.userId(),
						TicketVerificationCompletedEvent.Outcome.REJECTED,
						ticket.toSnapshot().version(),
						now,
						evt.clientAt(),
						null,
						new TicketVerificationCompletedEvent.Rejected(rej.reasonCode(),
								rej.message()),
						"ticketEngine",
						rej.providerTraceId());
			}

			case TicketVerificationProvider.FailedRetryable fail -> {
				// Ici: tu peux laisser le ticket en ANALYZING, ou poser un statut "FAILED" plus
				// tard
				// Pour rester simple: on n'altère pas le ticket, on push juste un completed
				// “FAILED_RETRYABLE”
				yield new TicketVerificationCompletedEvent(
						UUID.randomUUID(),
						evt.commandId(),
						evt.ticketId(),
						evt.userId(),
						TicketVerificationCompletedEvent.Outcome.FAILED_RETRYABLE,
						ticket.toSnapshot().version(),
						now,
						evt.clientAt(),
						null,
						new TicketVerificationCompletedEvent.Rejected("FAILED_RETRYABLE",
								fail.message()),
						"ticketEngine",
						fail.providerTraceId());
			}

			case TicketVerificationProvider.FailedFinal fail -> {
				yield new TicketVerificationCompletedEvent(
						UUID.randomUUID(),
						evt.commandId(),
						evt.ticketId(),
						evt.userId(),
						TicketVerificationCompletedEvent.Outcome.FAILED_FINAL,
						ticket.toSnapshot().version(),
						now,
						evt.clientAt(),
						null,
						new TicketVerificationCompletedEvent.Rejected("FAILED_FINAL",
								fail.message()),
						"ticketEngine",
						fail.providerTraceId());
			}
		};

		// publish -> outbox
		eventPublisher.publish(completed);
	}

	private List<Ticket.TicketLineItem> toDomainLineItems(List<TicketVerificationProvider.LineItem> items) {
		if (items == null)
			return null;
		return items.stream()
				.map(i -> new Ticket.TicketLineItem(i.label(), i.quantity(), i.amountCents()))
				.toList();
	}
}
