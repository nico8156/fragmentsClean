package com.nm.fragmentsclean.ticketContext.write.adapters.primary.springboot.scheduling;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationJobRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationCompletionHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationLeaseClaimer;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Claims durable work transactionally, then invokes the local process outside any transaction. */
@Component
public class ScheduledTicketVerificationWorker {
    private static final Logger log = LoggerFactory.getLogger(ScheduledTicketVerificationWorker.class);
    private final TicketVerificationJobRepository jobs;
    private final TicketVerificationLeaseClaimer claimer;
    private final TicketVerificationProvider provider;
    private final TicketVerificationCompletionHandler completer;
    private final DateTimeProvider clock;
    private final Duration leaseDuration;
    private final Duration retryDelay;
    private final int maxAttempts;
    private final int batchSize;

    public ScheduledTicketVerificationWorker(TicketVerificationJobRepository jobs, TicketVerificationLeaseClaimer claimer,
            TicketVerificationProvider provider, TicketVerificationCompletionHandler completer, DateTimeProvider clock,
            @Value("${ticketverify.worker.lease-seconds:30}") long leaseSeconds,
            @Value("${ticketverify.worker.retry-seconds:5}") long retrySeconds,
            @Value("${ticketverify.worker.max-attempts:5}") int maxAttempts,
            @Value("${ticketverify.worker.batch-size:10}") int batchSize) {
        this.jobs = jobs; this.claimer = claimer; this.provider = provider; this.completer = completer; this.clock = clock;
        this.leaseDuration = Duration.ofSeconds(leaseSeconds); this.retryDelay = Duration.ofSeconds(retrySeconds);
        this.maxAttempts = maxAttempts; this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ticketverify.worker.poll-ms:1000}")
    public void runDue() {
        var now = clock.now();
        for (UUID jobId : jobs.claimableIds(now, batchSize)) {
            try {
                process(jobId);
            } catch (RuntimeException failure) {
                // The lease makes the job recoverable after process death or an unexpected adapter failure.
                log.warn("[ticketverify-worker] execution failed jobId={} type={}", jobId,
                        failure.getClass().getSimpleName());
            }
        }
    }

    void process(UUID jobId) {
        String workerId = "ticket-verification-" + UUID.randomUUID();
        TicketVerificationLeaseClaimer.Work work;
        try {
            work = claimer.claim(jobId, workerId, clock.now(), leaseDuration);
        } catch (IllegalStateException raced) {
            log.debug("[ticketverify-worker] claim skipped jobId={} reason={}", jobId, raced.getMessage());
            return;
        }

        var result = provider.verify(work.job().ocrText(), work.job().imageRef());
        var completedAt = clock.now();
        if (result instanceof TicketVerificationProvider.FailedRetryable failure) {
            if (work.job().attempts() >= maxAttempts) {
                completer.failFinal(work, failure.message(), failure.providerTraceId(), completedAt);
            } else {
                completer.retry(work, failure.message(), completedAt,
                        completedAt.plus(retryDelay.multipliedBy(Math.max(1, work.job().attempts()))));
            }
            return;
        }
        completer.complete(work, result, completedAt);
    }
}
