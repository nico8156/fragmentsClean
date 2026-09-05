package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleRevisionMaterializer;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.GeneratedArticleDraft;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
@Repository
public class JdbcArticleRevisionMaterializer implements ArticleRevisionMaterializer {
    private final JdbcTemplate jdbc; private final ObjectMapper mapper;
    public JdbcArticleRevisionMaterializer(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc=jdbc; this.mapper=mapper; }
    @Override public void materialize(UUID articleId, UUID revisionId, GeneratedArticleDraft draft, Instant now) {
        Integer exists=jdbc.queryForObject("SELECT count(*) FROM articles WHERE article_id=?",Integer.class,articleId);
        if (exists==null || exists!=1) throw new IllegalStateException("Cannot materialize a revision for an unknown article");
        Integer already=jdbc.queryForObject("SELECT count(*) FROM article_revisions WHERE revision_id=?",Integer.class,revisionId);
        if (already!=null && already==1) return;
        Integer number=jdbc.queryForObject("SELECT coalesce(max(revision_number),0)+1 FROM article_revisions WHERE article_id=?",Integer.class,articleId);
        String text=draft.content().introduction().value()+" "+draft.content().conclusion().value()+" "+draft.sections().stream().map(s->s.content().paragraphs().get(0).value()).reduce("",(a,b)->a+" "+b);
        int readingTime=Math.max(1,(text.trim().split("\\s+").length+199)/200);
        var cover=draft.coverImage();
        jdbc.update("INSERT INTO article_revisions (revision_id,article_id,revision_number,title,introduction,conclusion,cover_reference,cover_width,cover_height,cover_alt,reading_time_min,status,created_at,updated_at,version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",revisionId,articleId,number,draft.content().title().value(),draft.content().introduction().value(),draft.content().conclusion().value(),cover==null?null:cover.storageReference(),cover==null?null:cover.width(),cover==null?null:cover.height(),cover==null?null:cover.alt(),readingTime,"DRAFT",Timestamp.from(now),Timestamp.from(now),0L);
        int position=0;
        for (var generated : draft.sections()) {
            var section=generated.content(); UUID sectionId=UUID.randomUUID();
            jdbc.update("INSERT INTO article_revision_sections (section_id,revision_id,position,heading) VALUES (?,?,?,?)",sectionId,revisionId,position++,section.heading());
            int paragraphPosition=0;
            for (var paragraph : section.paragraphs()) jdbc.update("INSERT INTO article_revision_paragraphs (paragraph_id,section_id,position,body) VALUES (?,?,?,?)",UUID.randomUUID(),sectionId,paragraphPosition++,paragraph.value());
            int imagePosition=0;
            for(var image:section.images()) jdbc.update("INSERT INTO article_revision_images (image_id,revision_id,section_id,position,storage_reference,width,height,alt,source) VALUES (?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),revisionId,sectionId,imagePosition++,image.storageReference(),image.width(),image.height(),image.alt(),"OPENAI");
        }
        int tagPosition=0;
        for (var tag : draft.tags()) jdbc.update("INSERT INTO article_revision_tags (revision_id,position,tag) VALUES (?,?,?)",revisionId,tagPosition++,tag.label());
        jdbc.update("UPDATE articles SET working_revision_id=?,title=?,intro=?,conclusion=?,cover_url=?,cover_width=?,cover_height=?,cover_alt=?,tags_json=?,reading_time_min=?,updated_at=?,version=version+1 WHERE article_id=?",
                revisionId,draft.content().title().value(),draft.content().introduction().value(),draft.content().conclusion().value(),
                cover==null?null:cover.storageReference(),cover==null?null:cover.width(),cover==null?null:cover.height(),cover==null?null:cover.alt(),
                tagsJson(draft),
                readingTime,Timestamp.from(now),articleId);
    }
    private String tagsJson(GeneratedArticleDraft draft) { try { return mapper.writeValueAsString(draft.tags().stream().map(t->t.label()).toList()); } catch (JsonProcessingException e) { throw new IllegalStateException("Cannot serialize article tags",e); } }
}
