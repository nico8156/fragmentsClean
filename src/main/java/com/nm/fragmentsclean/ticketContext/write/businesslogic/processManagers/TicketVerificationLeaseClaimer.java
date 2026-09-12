package com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationJobRepository;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TicketVerificationLeaseClaimer {
    private final TicketVerificationJobRepository jobs;
    public TicketVerificationLeaseClaimer(TicketVerificationJobRepository jobs) { this.jobs = jobs; }

    @Transactional
    public Work claim(UUID jobId, String workerId, Instant now, Duration leaseDuration) {
        var job = jobs.byId(jobId).orElseThrow(() -> new IllegalStateException("Unknown ticket verification job: " + jobId));
        job.claim(workerId, now, leaseDuration);
        jobs.save(job);
        return new Work(job.snapshot());
    }

    public record Work(TicketVerificationJob.Snapshot job) {}
}
