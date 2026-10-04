package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaCatalogScan;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class JdbcArticleMediaCatalogScan implements ArticleMediaCatalogScan {
    private final JdbcTemplate jdbc;
    public JdbcArticleMediaCatalogScan(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public Batch lockNext(int limit){
        var rows=jdbc.query("SELECT cursor_id FROM article_media_catalog_scan WHERE id=1 AND next_scan_at<=now() FOR UPDATE SKIP LOCKED",(rs,n)->Optional.ofNullable(rs.getObject(1,UUID.class)));
        if(rows.isEmpty())return new Batch(false,List.of());
        UUID cursor=rows.getFirst().orElse(null);
        var ids=jdbc.query("SELECT article_id FROM (SELECT article_id FROM articles UNION SELECT article_id FROM article_media_uploads) roots WHERE (?::uuid IS NULL OR article_id>?) ORDER BY article_id LIMIT ?",(rs,n)->rs.getObject(1,UUID.class),cursor,cursor,limit);
        return new Batch(true,ids);
    }
    public void advance(UUID cursor,boolean complete){
        if(complete)jdbc.update("UPDATE article_media_catalog_scan SET cursor_id=NULL,next_scan_at=now()+interval '1 day',completed_at=now() WHERE id=1");
        else jdbc.update("UPDATE article_media_catalog_scan SET cursor_id=? WHERE id=1",cursor);
    }
}
