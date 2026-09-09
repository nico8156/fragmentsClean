package com.nm.fragmentsclean.editorialIntelligenceContext.read.adapters.secondary;

import com.nm.fragmentsclean.editorialIntelligenceContext.read.EditorialSourceCatalog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcEditorialSourceCatalog implements EditorialSourceCatalog {
    private final JdbcTemplate jdbc;
    public JdbcEditorialSourceCatalog(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public List<EditorialSourceView> listSources() {
        return jdbc.query("SELECT source_id,name,access_mode,authority_level,endpoint,polling_frequency_seconds,enabled,status,last_checked_at,last_successful_check_at,next_check_at,failure_count,checkpoint_external_id,checkpoint_published_at,version FROM editorial_sources ORDER BY name,source_id", (rs,row) -> new EditorialSourceView(rs.getObject("source_id", UUID.class),rs.getString("name"),rs.getString("access_mode"),rs.getString("authority_level"),rs.getString("endpoint"),rs.getLong("polling_frequency_seconds"),rs.getBoolean("enabled"),rs.getString("status"),instant(rs,"last_checked_at"),instant(rs,"last_successful_check_at"),instant(rs,"next_check_at"),rs.getInt("failure_count"),rs.getString("checkpoint_external_id"),instant(rs,"checkpoint_published_at"),rs.getLong("version")));
    }
    @Override public List<SourceSignalView> listSignals(UUID sourceId, int limit) {
        int safeLimit=Math.max(1,Math.min(100,limit));
        return jdbc.query("SELECT signal_id,source_id,external_id,title,summary,url,author,published_at,discovered_at,status FROM editorial_source_signals WHERE source_id=? ORDER BY discovered_at DESC,signal_id DESC LIMIT ?", (rs,row) -> new SourceSignalView(rs.getObject("signal_id",UUID.class),rs.getObject("source_id",UUID.class),rs.getString("external_id"),rs.getString("title"),rs.getString("summary"),rs.getString("url"),rs.getString("author"),instant(rs,"published_at"),instant(rs,"discovered_at"),rs.getString("status")),sourceId,safeLimit);
    }
    private static Instant instant(java.sql.ResultSet rs,String column)throws java.sql.SQLException { Timestamp value=rs.getTimestamp(column); return value==null?null:value.toInstant(); }
}
