package com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.articleContext.read.ArticleMediaPreviewReadRepository;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcArticleMediaPreviewReadRepository implements ArticleMediaPreviewReadRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcArticleMediaPreviewReadRepository(JdbcTemplate jdbc){this.jdbc=new NamedParameterJdbcTemplate(jdbc);}
    public Set<String> currentReferences(Collection<String> refs){
        if(refs.isEmpty())return Set.of();
        return Set.copyOf(jdbc.query("""
            SELECT trim(cover_reference) FROM article_revisions WHERE trim(cover_reference) IN (:refs)
            UNION SELECT trim(storage_reference) FROM article_revision_images WHERE trim(storage_reference) IN (:refs)
            UNION SELECT storage_reference FROM article_media_uploads WHERE storage_reference IN (:refs)
            """,new MapSqlParameterSource("refs",refs),(rs,n)->rs.getString(1)));
    }
}
