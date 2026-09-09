package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ArticleBriefEvidenceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class JdbcArticleBriefEvidenceRepository implements ArticleBriefEvidenceRepository {
    private final JdbcTemplate jdbc;
    public JdbcArticleBriefEvidenceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<Evidence> bySignalIds(List<UUID> signalIds) {
        if (signalIds.isEmpty()) return List.of();
        String placeholders = String.join(",", java.util.Collections.nCopies(signalIds.size(), "?"));
        return jdbc.query("""
                SELECT source.name, signal.url, signal.published_at, source.authority_level
                FROM editorial_source_signals signal
                JOIN editorial_sources source ON source.source_id=signal.source_id
                WHERE signal.signal_id IN (%s)
                ORDER BY signal.published_at NULLS LAST, signal.signal_id
                """.formatted(placeholders), (result, row) -> new Evidence(
                result.getString("name"), result.getString("url"),
                result.getTimestamp("published_at") == null ? null : result.getTimestamp("published_at").toInstant(),
                result.getString("authority_level")), signalIds.toArray());
    }
}
