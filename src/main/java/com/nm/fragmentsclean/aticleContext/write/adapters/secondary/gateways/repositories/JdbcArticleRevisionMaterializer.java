package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRevisionMaterializer;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.GeneratedArticleDraft;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
@Repository
public final class JdbcArticleRevisionMaterializer implements ArticleRevisionMaterializer {
    private final JdbcTemplate jdbc;
    public JdbcArticleRevisionMaterializer(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void materialize(UUID articleId, UUID revisionId, GeneratedArticleDraft draft, Instant now) {
        Integer exists=jdbc.queryForObject("SELECT count(*) FROM articles WHERE article_id=?",Integer.class,articleId);
        if (exists==null || exists!=1) throw new IllegalStateException("Cannot materialize a revision for an unknown article");
        Integer already=jdbc.queryForObject("SELECT count(*) FROM article_revisions WHERE revision_id=?",Integer.class,revisionId);
        if (already!=null && already==1) return;
        Integer number=jdbc.queryForObject("SELECT coalesce(max(revision_number),0)+1 FROM article_revisions WHERE article_id=?",Integer.class,articleId);
        String text=draft.content().introduction().value()+" "+draft.content().conclusion().value()+" "+draft.sections().stream().map(s->s.content().paragraphs().get(0).value()).reduce("",(a,b)->a+" "+b);
        int readingTime=Math.max(1,(text.trim().split("\\s+").length+199)/200);
        jdbc.update("INSERT INTO article_revisions (revision_id,article_id,revision_number,title,introduction,conclusion,cover_reference,cover_width,cover_height,cover_alt,reading_time_min,status,created_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",revisionId,articleId,number,draft.content().title().value(),draft.content().introduction().value(),draft.content().conclusion().value(),null,null,null,null,readingTime,"DRAFT",Timestamp.from(now),Timestamp.from(now),0L);
        int position=0;
        for (var generated : draft.sections()) {
            var section=generated.content(); UUID sectionId=UUID.randomUUID();
            jdbc.update("INSERT INTO article_revision_sections (section_id,revision_id,position,heading) VALUES (?,?,?,?)",sectionId,revisionId,position++,section.heading());
            int paragraphPosition=0;
            for (var paragraph : section.paragraphs()) jdbc.update("INSERT INTO article_revision_paragraphs (paragraph_id,section_id,position,body) VALUES (?,?,?,?)",UUID.randomUUID(),sectionId,paragraphPosition++,paragraph.value());
        }
        int tagPosition=0;
        for (var tag : draft.tags()) jdbc.update("INSERT INTO article_revision_tags (revision_id,position,tag) VALUES (?,?,?)",revisionId,tagPosition++,tag.label());
    }
}
