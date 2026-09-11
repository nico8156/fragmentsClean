package com.nm.fragmentsclean.userApplicationContext.pass.adapters.secondary;

import com.nm.fragmentsclean.userApplicationContext.pass.application.PassContributionStore;
import com.nm.fragmentsclean.userApplicationContext.pass.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class JdbcPassContributionStore implements PassContributionStore {
    private final JdbcTemplate jdbc;

    public JdbcPassContributionStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void lockUser(UUID userId) {
        jdbc.execute((PreparedStatementCreator) connection -> {
            var statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))");
            statement.setString(1, userId.toString());
            return statement;
        }, statement -> {
            statement.execute();
            return null;
        });
    }

    @Override
    public boolean applyTicket(UUID ticketId, UUID userId, boolean active, long sourceVersion, Instant occurredAt) {
        int applied = jdbc.update("""
                INSERT INTO pass_ticket_contributions(ticket_id,user_id,active,source_version,updated_at)
                VALUES (?,?,?,?,?)
                ON CONFLICT(ticket_id) DO UPDATE SET
                  active=EXCLUDED.active,
                  source_version=EXCLUDED.source_version, updated_at=EXCLUDED.updated_at
                WHERE pass_ticket_contributions.user_id=EXCLUDED.user_id
                  AND pass_ticket_contributions.source_version < EXCLUDED.source_version
                """, ticketId, userId, active, sourceVersion, Timestamp.from(occurredAt));
        if (applied == 1) return true;
        assertTicketOwner(ticketId, userId);
        return false;
    }

    @Override
    public boolean applyExperience(UUID experienceId, UUID userId, UUID coffeeId, boolean active,
                                   long sourceVersion, Instant occurredAt) {
        int applied = jdbc.update("""
                INSERT INTO pass_experience_contributions(
                  experience_id,user_id,coffee_id,active,source_version,updated_at)
                VALUES (?,?,?,?,?,?)
                ON CONFLICT(experience_id) DO UPDATE SET
                  active=EXCLUDED.active,
                  source_version=EXCLUDED.source_version, updated_at=EXCLUDED.updated_at
                WHERE pass_experience_contributions.user_id=EXCLUDED.user_id
                  AND pass_experience_contributions.coffee_id=EXCLUDED.coffee_id
                  AND pass_experience_contributions.source_version < EXCLUDED.source_version
                """, experienceId, userId, coffeeId, active, sourceVersion, Timestamp.from(occurredAt));
        if (applied == 1) return true;
        assertExperienceIdentity(experienceId, userId, coffeeId);
        return false;
    }

    @Override
    public PassCounters counters(UUID userId) {
        return jdbc.queryForObject("""
                SELECT
                  (SELECT COUNT(*) FROM pass_experience_contributions WHERE user_id=? AND active=true) experiences,
                  (SELECT COUNT(DISTINCT coffee_id) FROM pass_experience_contributions WHERE user_id=? AND active=true) cafes,
                  (SELECT COUNT(*) FROM pass_ticket_contributions WHERE user_id=? AND active=true) tickets
                """, (rs, row) -> new PassCounters(
                rs.getInt("experiences"), rs.getInt("cafes"), rs.getInt("tickets")),
                userId, userId, userId);
    }

    @Override
    public Optional<PassSnapshot> find(UUID userId) {
        return jdbc.query("""
                SELECT user_id,policy_version,published_experiences,distinct_experienced_coffees,
                       validated_tickets,acquired_levels,version,updated_at
                FROM user_pass_projection WHERE user_id=?
                """, (rs, row) -> {
            var counters = new PassCounters(rs.getInt("published_experiences"),
                    rs.getInt("distinct_experienced_coffees"), rs.getInt("validated_tickets"));
            return PassProgressPolicy.evaluate(rs.getObject("user_id", UUID.class), counters,
                    parseLevels(rs.getString("acquired_levels")), rs.getLong("version"),
                    rs.getTimestamp("updated_at").toInstant());
        }, userId).stream().findFirst();
    }

    @Override
    public void save(PassSnapshot snapshot) {
        jdbc.update("""
                INSERT INTO user_pass_projection(
                  user_id,policy_version,published_experiences,distinct_experienced_coffees,
                  validated_tickets,acquired_levels,version,updated_at)
                VALUES (?,?,?,?,?,?,?,?)
                ON CONFLICT(user_id) DO UPDATE SET
                  policy_version=EXCLUDED.policy_version,
                  published_experiences=EXCLUDED.published_experiences,
                  distinct_experienced_coffees=EXCLUDED.distinct_experienced_coffees,
                  validated_tickets=EXCLUDED.validated_tickets,
                  acquired_levels=EXCLUDED.acquired_levels,
                  version=EXCLUDED.version,updated_at=EXCLUDED.updated_at
                """, snapshot.userId(), snapshot.policyVersion(), snapshot.counters().publishedExperiences(),
                snapshot.counters().distinctExperiencedCoffees(), snapshot.counters().validatedTickets(),
                serialize(snapshot.acquiredLevels()), snapshot.version(), Timestamp.from(snapshot.updatedAt()));
    }

    private String serialize(Set<PassLevel> levels) {
        return Arrays.stream(PassLevel.values()).filter(levels::contains)
                .map(Enum::name).collect(Collectors.joining(","));
    }

    private void assertTicketOwner(UUID ticketId, UUID expectedUserId) {
        jdbc.query("SELECT user_id FROM pass_ticket_contributions WHERE ticket_id=?",
                rs -> {
                    UUID actual = rs.getObject("user_id", UUID.class);
                    if (!actual.equals(expectedUserId)) {
                        throw new IllegalStateException("Ticket contribution owner cannot change");
                    }
                }, ticketId);
    }

    private void assertExperienceIdentity(UUID experienceId, UUID expectedUserId, UUID expectedCoffeeId) {
        jdbc.query("SELECT user_id,coffee_id FROM pass_experience_contributions WHERE experience_id=?",
                rs -> {
                    UUID actualUser = rs.getObject("user_id", UUID.class);
                    UUID actualCoffee = rs.getObject("coffee_id", UUID.class);
                    if (!actualUser.equals(expectedUserId) || !actualCoffee.equals(expectedCoffeeId)) {
                        throw new IllegalStateException("Experience contribution identity cannot change");
                    }
                }, experienceId);
    }

    private Set<PassLevel> parseLevels(String value) {
        var levels = EnumSet.noneOf(PassLevel.class);
        if (value == null || value.isBlank()) return levels;
        Arrays.stream(value.split(",")).map(PassLevel::valueOf).forEach(levels::add);
        return levels;
    }
}
