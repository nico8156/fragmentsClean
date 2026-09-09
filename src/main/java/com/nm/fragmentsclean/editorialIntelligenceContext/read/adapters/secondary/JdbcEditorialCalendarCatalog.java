package com.nm.fragmentsclean.editorialIntelligenceContext.read.adapters.secondary;

import com.nm.fragmentsclean.editorialIntelligenceContext.read.EditorialCalendarCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class JdbcEditorialCalendarCatalog implements EditorialCalendarCatalog {
    private final JdbcTemplate jdbc;
    public JdbcEditorialCalendarCatalog(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Item> month(YearMonth month) {
        Instant from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant until = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return jdbc.query("""
                SELECT schedule_id,article_id,revision_id,operation,due_at,status,rejection_reason
                FROM editorial_publication_schedule
                WHERE due_at>=? AND due_at<?
                ORDER BY due_at,schedule_id
                """, (result, row) -> new Item(
                result.getObject("schedule_id", java.util.UUID.class),
                result.getObject("article_id", java.util.UUID.class),
                result.getObject("revision_id", java.util.UUID.class),
                result.getString("operation"), result.getTimestamp("due_at").toInstant(),
                result.getString("status"), result.getString("rejection_reason")),
                Timestamp.from(from), Timestamp.from(until));
    }
}
