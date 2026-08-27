package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleGenerationRunRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcArticleGenerationRunRepository implements ArticleGenerationRunRepository {
    private final JdbcTemplate jdbc;
    public JdbcArticleGenerationRunRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public Optional<ArticleGenerationRun> byId(UUID id) {
        try { return Optional.of(jdbc.queryForObject("SELECT run_id,saga_id,attempt,worker_id,status,provider_response_id,provider,model,schema_version,failure_category,started_at,completed_at FROM article_generation_runs WHERE run_id=?", this::map, id)); }
        catch (EmptyResultDataAccessException e) { return Optional.empty(); }
    }
    private ArticleGenerationRun map(ResultSet rs, int ignored) throws SQLException {
        return ArticleGenerationRun.reconstitute(new ArticleGenerationRun.Snapshot(rs.getObject("run_id",UUID.class),rs.getObject("saga_id",UUID.class),rs.getInt("attempt"),rs.getString("worker_id"),ArticleGenerationRun.Status.valueOf(rs.getString("status")),rs.getString("provider_response_id"),rs.getString("provider"),rs.getString("model"),rs.getString("schema_version"),rs.getString("failure_category")==null?null:ArticleAuthoringFailureCategory.valueOf(rs.getString("failure_category")),rs.getTimestamp("started_at").toInstant(),rs.getTimestamp("completed_at")==null?null:rs.getTimestamp("completed_at").toInstant()));
    }
    @Override public void save(ArticleGenerationRun run) {
        ArticleGenerationRun.Snapshot s=run.snapshot();
        if (s.status()==ArticleGenerationRun.Status.STARTED) { jdbc.update("INSERT INTO article_generation_runs (run_id,saga_id,attempt,worker_id,status,started_at) VALUES (?,?,?,?,?,?) ON CONFLICT (saga_id,attempt) DO NOTHING",s.runId(),s.sagaId(),s.attempt(),s.workerId(),s.status().name(),Timestamp.from(s.startedAt())); return; }
        jdbc.update("UPDATE article_generation_runs SET status=?,provider_response_id=?,provider=?,model=?,schema_version=?,failure_category=?,completed_at=? WHERE run_id=? AND status='STARTED'",s.status().name(),s.providerResponseId(),s.provider(),s.model(),s.schemaVersion(),s.failureCategory()==null?null:s.failureCategory().name(),Timestamp.from(s.completedAt()),s.runId());
    }
}
