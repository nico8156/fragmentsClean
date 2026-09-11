package com.nm.fragmentsclean.userApplicationContextTest.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
import com.nm.fragmentsclean.platform.eventing.contracts.TicketIntegrationEvents;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers.AbstractBaseE2E;
import com.nm.fragmentsclean.userApplicationContext.pass.application.PassContributionProjector;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.PassLevel;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketSubmissionFingerprintRegistry;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketSubmissionFingerprint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PassContributionProjectionIT extends AbstractBaseE2E {
    @Autowired PassContributionProjector projector;
    @Autowired JdbcTemplate jdbc;
    @Autowired TicketSubmissionFingerprintRegistry fingerprints;
    @Autowired SqsIntegrationEventRouting routing;
    @Autowired ObjectMapper objectMapper;
    @Autowired MockMvc mvc;

    @BeforeEach void clear() {
        jdbc.update("DELETE FROM user_pass_projection");
        jdbc.update("DELETE FROM pass_experience_contributions");
        jdbc.update("DELETE FROM pass_ticket_contributions");
        jdbc.update("DELETE FROM ticket_submission_fingerprints");
        jdbc.update("DELETE FROM inbox_messages WHERE destination='app-users-events'");
    }

    @Test
    void projections_deduplicate_sources_and_count_distinct_coffees() {
        UUID user = UUID.randomUUID();
        UUID cafe = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-11T10:00:00Z");

        assertThat(projector.experienceChanged(first, user, cafe, true, false, 1, now)).isPresent();
        assertThat(projector.experienceChanged(first, user, cafe, true, false, 1, now)).isEmpty();
        assertThat(projector.experienceChanged(UUID.randomUUID(), user, cafe, true, false, 1, now)).isPresent();
        var pass = projector.ticketChanged(UUID.randomUUID(), user, true, false, 1, now).orElseThrow();

        assertThat(pass.counters().publishedExperiences()).isEqualTo(2);
        assertThat(pass.counters().distinctExperiencedCoffees()).isEqualTo(1);
        assertThat(pass.counters().validatedTickets()).isEqualTo(1);
        assertThat(pass.acquiredLevels()).contains(PassLevel.COFFEE_TASTER);
        assertThat(pass.version()).isEqualTo(3);
    }

    @Test
    void a_newer_moderation_tombstone_cannot_be_reopened_by_an_older_event() {
        UUID user = UUID.randomUUID(), experience = UUID.randomUUID(), cafe = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-11T10:00:00Z");
        projector.experienceChanged(experience, user, cafe, true, false, 1, now);
        projector.experienceChanged(experience, user, cafe, false, true, 3, now.plusSeconds(3));

        assertThat(projector.experienceChanged(experience, user, cafe, true, false, 2, now.plusSeconds(2))).isEmpty();
        assertThat(jdbc.queryForObject(
                "SELECT active FROM pass_experience_contributions WHERE experience_id=?",
                Boolean.class, experience)).isFalse();
    }

    @Test
    void a_source_identity_cannot_be_transferred_by_a_newer_event() {
        UUID owner = UUID.randomUUID(), otherUser = UUID.randomUUID();
        UUID experience = UUID.randomUUID(), cafe = UUID.randomUUID();
        UUID ticket = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-11T10:00:00Z");
        projector.experienceChanged(experience, owner, cafe, true, false, 1, now);
        projector.ticketChanged(ticket, owner, true, false, 1, now);

        assertThatThrownBy(() -> projector.experienceChanged(
                experience, otherUser, cafe, true, false, 2, now.plusSeconds(1)))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Experience contribution identity cannot change");
        assertThatThrownBy(() -> projector.ticketChanged(
                ticket, otherUser, true, false, 2, now.plusSeconds(1)))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Ticket contribution owner cannot change");
        assertThatThrownBy(() -> projector.experienceChanged(
                experience, owner, UUID.randomUUID(), true, false, 2, now.plusSeconds(1)))
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Experience contribution identity cannot change");

        assertThat(jdbc.queryForObject(
                "SELECT user_id FROM pass_experience_contributions WHERE experience_id=?",
                UUID.class, experience)).isEqualTo(owner);
        assertThat(jdbc.queryForObject(
                "SELECT user_id FROM pass_ticket_contributions WHERE ticket_id=?",
                UUID.class, ticket)).isEqualTo(owner);
    }

    @Test
    void exact_receipt_fingerprint_has_one_concurrent_business_owner() {
        var fingerprint = TicketSubmissionFingerprint.fromOcr(" CAFE\nTOTAL 4,00 ").orElseThrow();
        UUID first = UUID.randomUUID();
        assertThat(fingerprints.claim(fingerprint, first, UUID.randomUUID(), Instant.now())).isTrue();
        assertThat(fingerprints.claim(fingerprint, UUID.randomUUID(), UUID.randomUUID(), Instant.now())).isFalse();
    }

    @Test
    void stable_ticket_event_is_inbox_idempotent_and_visible_through_the_authenticated_http_contract() throws Exception {
        UUID user = UUID.randomUUID(), ticket = UUID.randomUUID(), eventId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-11T10:00:00Z");
        var payload = new TicketIntegrationEvents.VerificationCompleted(
                eventId, UUID.randomUUID(), ticket, user, "APPROVED", 1, now, now,
                new TicketIntegrationEvents.Approved(500, "EUR", now, "Cafe", "Paris", "CARD", java.util.List.of()),
                null, "ticketEngine", "trace");
        var envelope = new IntegrationEventEnvelope(eventId.toString(), "ticket.verification.completed", 1,
                "TicketVerificationCompletedEvent", "Ticket", ticket.toString(), "user:" + user,
                "app-users-events", objectMapper.writeValueAsString(payload), now);

        routing.route(envelope);
        routing.route(envelope);

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM pass_ticket_contributions WHERE ticket_id=?",
                Integer.class, ticket)).isEqualTo(1);
        mvc.perform(get("/api/users/me/entitlements")
                        .with(jwt().jwt(token -> token.subject(user.toString()).claim("roles", java.util.List.of("USER")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyVersion").value(2))
                .andExpect(jsonPath("$.counters.validatedTickets").value(1))
                .andExpect(jsonPath("$.counters.publishedExperiences").value(0));
    }

    @Test
    void experience_lifecycle_facts_are_interpreted_by_the_pass_consumer() throws Exception {
        UUID user = UUID.randomUUID(), experience = UUID.randomUUID(), cafe = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-11T10:00:00Z");

        routeExperience(new ExperienceIntegrationEvents.LifecycleChanged(
                UUID.randomUUID(), experience, user, cafe, "PUBLISHED", "VISIBLE",
                "PUBLISHED", 1, now));
        assertThat(jdbc.queryForObject(
                "SELECT published_experiences FROM user_pass_projection WHERE user_id=?",
                Integer.class, user)).isEqualTo(1);

        routeExperience(new ExperienceIntegrationEvents.LifecycleChanged(
                UUID.randomUUID(), experience, user, cafe, "PUBLISHED", "HIDDEN",
                "MODERATION_HIDDEN", 2, now.plusSeconds(1)));
        assertThat(jdbc.queryForObject(
                "SELECT published_experiences FROM user_pass_projection WHERE user_id=?",
                Integer.class, user)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT acquired_levels FROM user_pass_projection WHERE user_id=?",
                String.class, user)).isEmpty();
    }

    private void routeExperience(ExperienceIntegrationEvents.LifecycleChanged payload) throws Exception {
        routing.route(new IntegrationEventEnvelope(payload.eventId().toString(), "experience.lifecycle.changed", 1,
                "ExperienceLifecycleChangedEvent", "Experience", payload.experienceId().toString(),
                "user:" + payload.userId(), "app-users-events", objectMapper.writeValueAsString(payload),
                payload.occurredAt()));
    }
}
