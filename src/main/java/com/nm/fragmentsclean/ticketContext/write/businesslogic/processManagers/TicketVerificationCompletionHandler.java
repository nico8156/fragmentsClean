package com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationJobRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TicketVerificationCompletionHandler {
    private final TicketVerificationJobRepository jobs;
    private final TicketRepository tickets;
    private final DomainEventPublisher events;

    public TicketVerificationCompletionHandler(TicketVerificationJobRepository jobs, TicketRepository tickets,
            DomainEventPublisher events) {
        this.jobs = jobs;
        this.tickets = tickets;
        this.events = events;
    }

    @Transactional
    public boolean complete(TicketVerificationLeaseClaimer.Work work, TicketVerificationProvider.Result result, Instant now) {
        var job = currentOwnedJob(work);
        if (job == null) return false;
        var snapshot = job.snapshot();
        var ticket = tickets.byId(snapshot.ticketId())
                .orElseThrow(() -> new IllegalStateException("Ticket not found: " + snapshot.ticketId()));
        if (!Objects.equals(ticket.toSnapshot().userId(), snapshot.userId())) {
            throw new IllegalStateException("Ticket userId mismatch");
        }

        var currentStatus = ticket.toSnapshot().status();
        if (currentStatus == Ticket.TicketStatus.CONFIRMED || currentStatus == Ticket.TicketStatus.REJECTED
                || currentStatus == Ticket.TicketStatus.DELETED) {
            job.complete(snapshot.leaseOwner(), now);
            jobs.save(job);
            return false;
        }

        var completed = eventFor(ticket, snapshot, result, now);
        if (result instanceof TicketVerificationProvider.FailedFinal failure) {
            job.failFinal(snapshot.leaseOwner(), failure.message(), now);
        } else {
            job.complete(snapshot.leaseOwner(), now);
        }
        jobs.save(job);
        events.publish(completed);
        return true;
    }

    @Transactional
    public boolean retry(TicketVerificationLeaseClaimer.Work work, String reason, Instant now, Instant nextAttemptAt) {
        var job = currentOwnedJob(work);
        if (job == null) return false;
        job.retry(job.snapshot().leaseOwner(), reason, now, nextAttemptAt);
        jobs.save(job);
        return true;
    }

    @Transactional
    public boolean failFinal(TicketVerificationLeaseClaimer.Work work, String reason, String providerTraceId, Instant now) {
        var job = currentOwnedJob(work);
        if (job == null) return false;
        var snapshot = job.snapshot();
        var ticket = tickets.byId(snapshot.ticketId())
                .orElseThrow(() -> new IllegalStateException("Ticket not found: " + snapshot.ticketId()));
        if (!Objects.equals(ticket.toSnapshot().userId(), snapshot.userId())) throw new IllegalStateException("Ticket userId mismatch");

        var currentStatus = ticket.toSnapshot().status();
        if (currentStatus == Ticket.TicketStatus.CONFIRMED || currentStatus == Ticket.TicketStatus.REJECTED
                || currentStatus == Ticket.TicketStatus.DELETED) {
            job.complete(snapshot.leaseOwner(), now);
            jobs.save(job);
            return false;
        }

        boolean changed = ticket.markVerificationFailed("TECHNICAL_VERIFICATION_FAILURE", now);
        if (changed) tickets.save(ticket);
        job.failFinal(snapshot.leaseOwner(), reason, now);
        jobs.save(job);
        events.publish(new TicketVerificationCompletedEvent(UUID.randomUUID(), snapshot.commandId(), snapshot.ticketId(),
                snapshot.userId(), TicketVerificationCompletedEvent.Outcome.FAILED_FINAL, ticket.toSnapshot().version(), now,
                snapshot.clientAt(), null, new TicketVerificationCompletedEvent.Rejected("FAILED_FINAL", reason),
                "ticketEngine", providerTraceId));
        return true;
    }

    private TicketVerificationJob currentOwnedJob(TicketVerificationLeaseClaimer.Work work) {
        var current = jobs.byId(work.job().jobId()).orElseThrow();
        var snapshot = current.snapshot();
        if (snapshot.state() != TicketVerificationJob.State.RUNNING
                || !Objects.equals(snapshot.leaseOwner(), work.job().leaseOwner())
                || snapshot.version() != work.job().version()) return null;
        return current;
    }

    private TicketVerificationCompletedEvent eventFor(Ticket ticket, TicketVerificationJob.Snapshot job,
            TicketVerificationProvider.Result result, Instant now) {
        if (result instanceof TicketVerificationProvider.Approved approved) {
            ticket.confirm(new Ticket.ConfirmResult(approved.amountCents(), approved.currency(), approved.ticketDate(),
                    approved.merchantName(), approved.merchantAddress(), approved.paymentMethod(), toDomainLineItems(approved.lineItems())), now);
            tickets.save(ticket);
            return new TicketVerificationCompletedEvent(UUID.randomUUID(), job.commandId(), job.ticketId(), job.userId(),
                    TicketVerificationCompletedEvent.Outcome.APPROVED, ticket.toSnapshot().version(), now, job.clientAt(),
                    new TicketVerificationCompletedEvent.Approved(approved.amountCents(), approved.currency(), approved.ticketDate(),
                            approved.merchantName(), approved.merchantAddress(), approved.paymentMethod(), toDomainLineItems(approved.lineItems())),
                    null, "ticketEngine", approved.providerTraceId());
        }
        if (result instanceof TicketVerificationProvider.Rejected rejected) {
            ticket.reject(rejected.reasonCode(), now);
            tickets.save(ticket);
            return new TicketVerificationCompletedEvent(UUID.randomUUID(), job.commandId(), job.ticketId(), job.userId(),
                    TicketVerificationCompletedEvent.Outcome.REJECTED, ticket.toSnapshot().version(), now, job.clientAt(), null,
                    new TicketVerificationCompletedEvent.Rejected(rejected.reasonCode(), rejected.message()),
                    "ticketEngine", rejected.providerTraceId());
        }
        if (result instanceof TicketVerificationProvider.FailedFinal failed) {
            ticket.markVerificationFailed("TECHNICAL_VERIFICATION_FAILURE", now);
            tickets.save(ticket);
            return new TicketVerificationCompletedEvent(UUID.randomUUID(), job.commandId(), job.ticketId(), job.userId(),
                    TicketVerificationCompletedEvent.Outcome.FAILED_FINAL, ticket.toSnapshot().version(), now, job.clientAt(), null,
                    new TicketVerificationCompletedEvent.Rejected("FAILED_FINAL", failed.message()),
                    "ticketEngine", failed.providerTraceId());
        }
        throw new IllegalArgumentException("Retryable result must use retry()");
    }

    private List<Ticket.TicketLineItem> toDomainLineItems(List<TicketVerificationProvider.LineItem> items) {
        return items == null ? null : items.stream()
                .map(item -> new Ticket.TicketLineItem(item.label(), item.quantity(), item.amountCents())).toList();
    }
}
