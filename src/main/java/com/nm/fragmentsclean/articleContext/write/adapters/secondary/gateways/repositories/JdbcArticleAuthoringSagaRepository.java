package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleAuthoringSagaRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

/** JDBC adapter for coordination state; atomic version check protects worker races. */
@Repository
public class JdbcArticleAuthoringSagaRepository implements ArticleAuthoringSagaRepository {
    private final JdbcTemplate jdbc;
    public JdbcArticleAuthoringSagaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public Optional<ArticleAuthoringSaga> byId(UUID sagaId) {
        try {
            return Optional.of(jdbc.queryForObject("SELECT saga_id, article_id, revision_id, theme, trigger, state, version, generation_attempts, lease_owner, lease_until, failure_category, created_at, updated_at FROM article_authoring_sagas WHERE saga_id = ?", (rs, n) -> ArticleAuthoringSaga.reconstitute(new ArticleAuthoringSaga.Snapshot(
                    rs.getObject("saga_id", UUID.class), rs.getObject("article_id", UUID.class), rs.getObject("revision_id", UUID.class),
                    rs.getString("theme"), ArticleAuthoringTrigger.valueOf(rs.getString("trigger")), ArticleAuthoringSagaState.valueOf(rs.getString("state")),
                    rs.getLong("version"), rs.getInt("generation_attempts"), rs.getString("lease_owner"),
                    rs.getTimestamp("lease_until") == null ? null : rs.getTimestamp("lease_until").toInstant(),
                    rs.getString("failure_category") == null ? null : ArticleAuthoringFailureCategory.valueOf(rs.getString("failure_category")),
                    rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant())), sagaId));
        } catch (EmptyResultDataAccessException e) { return Optional.empty(); }
    }

    @Override public void save(ArticleAuthoringSaga saga) {
        var s = saga.snapshot();
        if (s.version() == 0) {
            jdbc.update("INSERT INTO article_authoring_sagas (saga_id, article_id, revision_id, theme, trigger, state, version, generation_attempts, lease_owner, lease_until, failure_category, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    s.sagaId(), s.articleId(), s.revisionId(), s.theme(), s.trigger().name(), s.state().name(), s.version(), s.generationAttempts(), s.leaseOwner(), timestamp(s.leaseUntil()), failure(s.failureCategory()), timestamp(s.createdAt()), timestamp(s.updatedAt()));
            return;
        }
        int updated = jdbc.update("UPDATE article_authoring_sagas SET state=?, version=?, generation_attempts=?, lease_owner=?, lease_until=?, failure_category=?, updated_at=? WHERE saga_id=? AND version=?",
                s.state().name(), s.version(), s.generationAttempts(), s.leaseOwner(), timestamp(s.leaseUntil()), failure(s.failureCategory()), timestamp(s.updatedAt()), s.sagaId(), s.version() - 1);
        if (updated != 1) throw new IllegalStateException("Article authoring saga version conflict: " + s.sagaId());
    }
    private static Timestamp timestamp(java.time.Instant value) { return value == null ? null : Timestamp.from(value); }
    private static String failure(ArticleAuthoringFailureCategory value) { return value == null ? null : value.name(); }
}
