package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.SourceSignalRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.SourceSignal;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.List;

@Repository
public final class JdbcSourceSignalRepository implements SourceSignalRepository {
 private final JdbcTemplate jdbc; public JdbcSourceSignalRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @Override public int saveIgnoringDuplicate(List<SourceSignal> signals) {
  int saved=0; for(var s:signals) saved+=jdbc.update("INSERT INTO editorial_source_signals (signal_id,source_id,external_id,title,summary,url,author,published_at,discovered_at,fingerprint) VALUES (?,?,?,?,?,?,?,?,?,?) ON CONFLICT (source_id,external_id) DO NOTHING",s.id(),s.sourceId(),s.externalId(),s.title(),s.summary(),s.url(),s.author(),timestamp(s.publishedAt()),timestamp(s.discoveredAt()),s.fingerprint()); return saved;
 }
 private static Timestamp timestamp(java.time.Instant value){return value==null?null:Timestamp.from(value);}
}
