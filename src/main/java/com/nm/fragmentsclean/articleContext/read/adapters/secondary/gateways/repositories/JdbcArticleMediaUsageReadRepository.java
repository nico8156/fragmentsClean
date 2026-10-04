package com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.articleContext.read.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
public class JdbcArticleMediaUsageReadRepository implements ArticleMediaUsageReadRepository {
    private final JdbcTemplate jdbc;
    public JdbcArticleMediaUsageReadRepository(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Optional<Page> list(ListAdminArticleMediaQuery query){
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM articles WHERE article_id=?)",Boolean.class,query.articleId())))return Optional.empty();
        var sql=new StringBuilder("""
            WITH usages AS (
              SELECT r.revision_id,r.revision_number,r.title,r.status,r.cover_reference AS reference,
                r.cover_width AS width,r.cover_height AS height,r.cover_alt AS alt,
                0 AS role,-1 AS section_position,0 AS image_position,r.revision_id AS row_id,
                r.revision_id=a.working_revision_id AS working,r.revision_id=a.published_revision_id AS published
              FROM article_revisions r JOIN articles a ON a.article_id=r.article_id
              WHERE r.article_id=? AND r.cover_reference IS NOT NULL AND length(trim(r.cover_reference))>0
              UNION ALL
              SELECT r.revision_id,r.revision_number,r.title,r.status,i.storage_reference,
                i.width,i.height,i.alt,1,coalesce(s.position,-1),i.position,i.image_id,
                r.revision_id=a.working_revision_id,r.revision_id=a.published_revision_id
              FROM article_revision_images i JOIN article_revisions r ON r.revision_id=i.revision_id
              JOIN articles a ON a.article_id=r.article_id
              LEFT JOIN article_revision_sections s ON s.section_id=i.section_id AND s.revision_id=r.revision_id
              WHERE r.article_id=? AND length(trim(i.storage_reference))>0
            ) SELECT * FROM usages WHERE true
            """);
        var args=new ArrayList<Object>(List.of(query.articleId(),query.articleId()));
        if(query.cursor()!=null){
            var c=ListAdminArticleMediaQuery.parseCursor(query.cursor());
            sql.append(" AND (revision_id,role,section_position,image_position,row_id)>(?,?,?,?,?)");
            args.add(UUID.fromString(c[0]));args.add(Integer.parseInt(c[1]));args.add(Integer.parseInt(c[2]));args.add(Integer.parseInt(c[3]));args.add(UUID.fromString(c[4]));
        }
        sql.append(" ORDER BY revision_id,role,section_position,image_position,row_id LIMIT ?");args.add(query.limit()+1);
        var rows=jdbc.query(sql.toString(),(rs,n)->new Row(new Usage(query.articleId(),rs.getObject("revision_id",UUID.class),rs.getInt("revision_number"),rs.getString("title"),rs.getString("status"),rs.getInt("role")==0?"COVER":"SECTION",rs.getInt("section_position")<0?null:rs.getInt("section_position"),rs.getInt("image_position"),rs.getString("alt"),rs.getInt("width"),rs.getInt("height"),rs.getBoolean("working"),rs.getBoolean("published"),rs.getString("reference")),rs.getString("revision_id")+":"+rs.getInt("role")+":"+rs.getInt("section_position")+":"+rs.getInt("image_position")+":"+rs.getString("row_id")),args.toArray());
        boolean more=rows.size()>query.limit();var page=rows.subList(0,Math.min(rows.size(),query.limit()));
        return Optional.of(new Page(page.stream().map(Row::usage).toList(),more?page.getLast().cursor():null));
    }
    private record Row(Usage usage,String cursor){}
}
