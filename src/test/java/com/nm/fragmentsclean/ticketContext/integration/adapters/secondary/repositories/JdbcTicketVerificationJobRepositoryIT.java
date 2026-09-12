package com.nm.fragmentsclean.ticketContext.integration.adapters.secondary.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.repositories.jdbc.JdbcTicketVerificationJobRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationJob;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class JdbcTicketVerificationJobRepositoryIT {
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13.1");
    private static JdbcTicketVerificationJobRepository repository;
    private static final Instant NOW = Instant.parse("2026-09-12T08:00:00Z");

    @BeforeAll static void database() {
        postgres.start();
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        jdbc.execute("""
                CREATE TABLE ticket_verification_jobs (
                  job_id uuid primary key, command_id uuid not null unique, ticket_id uuid not null, user_id uuid not null,
                  ocr_text text, image_ref text, client_at timestamptz, state varchar(32) not null, attempts integer not null,
                  lease_owner varchar(160), lease_until timestamptz, next_attempt_at timestamptz not null,
                  last_failure text, version bigint not null, created_at timestamptz not null, updated_at timestamptz not null)
                """);
        repository = new JdbcTicketVerificationJobRepository(jdbc);
    }

    @AfterAll static void stop() { postgres.stop(); }

    @Test void round_trip_due_query_and_optimistic_conflict() {
        var job = requested(); repository.save(job);
        assertThat(repository.claimableIds(NOW, 10)).contains(job.snapshot().jobId());

        var first = repository.byId(job.snapshot().jobId()).orElseThrow();
        var stale = repository.byId(job.snapshot().jobId()).orElseThrow();
        first.claim("worker-a", NOW, Duration.ofSeconds(30)); repository.save(first);
        stale.claim("worker-b", NOW, Duration.ofSeconds(30));

        assertThatThrownBy(() -> repository.save(stale)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version conflict");
        assertThat(repository.claimableIds(NOW.plusSeconds(29), 10)).doesNotContain(job.snapshot().jobId());
        assertThat(repository.claimableIds(NOW.plusSeconds(30), 10)).contains(job.snapshot().jobId());
    }

    private TicketVerificationJob requested() {
        return TicketVerificationJob.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "TOTAL", "s3://ticket", NOW.minusSeconds(1), NOW);
    }
}
