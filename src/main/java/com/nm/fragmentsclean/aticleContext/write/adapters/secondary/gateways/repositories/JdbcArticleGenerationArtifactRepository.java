package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositories;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleGenerationArtifactRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.GeneratedArticleDraft;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
@Repository
public final class JdbcArticleGenerationArtifactRepository implements ArticleGenerationArtifactRepository {
    private final JdbcTemplate jdbc; private final ObjectMapper mapper;
    public JdbcArticleGenerationArtifactRepository(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc=jdbc; this.mapper=mapper; }
    @Override public void save(UUID runId, UUID sagaId, UUID articleId, UUID revisionId, String schemaVersion, GeneratedArticleDraft draft) {
        final String json;
        try { json=mapper.writeValueAsString(draft); } catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize normalized article draft", e); }
        jdbc.update("INSERT INTO article_generation_artifacts (run_id,saga_id,article_id,revision_id,schema_version,draft_json,created_at) VALUES (?,?,?,?,?,?,?) ON CONFLICT (run_id) DO UPDATE SET draft_json=EXCLUDED.draft_json,schema_version=EXCLUDED.schema_version",runId,sagaId,articleId,revisionId,schemaVersion,json,Timestamp.from(Instant.now()));
    }
}
