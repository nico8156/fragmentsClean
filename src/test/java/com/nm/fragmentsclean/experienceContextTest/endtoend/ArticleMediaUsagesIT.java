package com.nm.fragmentsclean.experienceContextTest.endtoend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class ArticleMediaUsagesIT extends AbstractExperienceE2E {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    private static final String ADMIN="99999999-9999-9999-9999-999999999999";
    private String path(UUID id){return "/api/admin/studio/articles/"+id+"/media";}
    private UUID article(){
        var id=UUID.randomUUID();
        jdbc.update("INSERT INTO articles(article_id,slug,locale,author_id,author_name,title,intro,blocks_json,conclusion,tags_json,reading_time_min,coffee_ids_json,created_at,updated_at,status,version) VALUES(?,?,'fr-FR',?,'Editorial','Article','Intro','[]','Fin','[]',1,'[]',now(),now(),'DRAFT',1)",id,id.toString(),UUID.randomUUID());
        return id;
    }
    private UUID revision(UUID article,int number,String reference){
        var id=UUID.randomUUID();
        jdbc.update("INSERT INTO article_revisions(revision_id,article_id,revision_number,title,introduction,conclusion,cover_reference,cover_width,cover_height,cover_alt,reading_time_min,status,created_at,updated_at,version) VALUES(?,?,?,'Titre','Intro','Fin',?,1200,800,'Couverture',1,'DRAFT',now(),now(),1)",id,article,number,reference);
        return id;
    }
    @Test void one_file_reused_across_revisions_has_one_identity_and_distinct_usages() throws Exception {
        var article=article();var first=revision(article,1,"https://images.test/cover.jpg");var second=revision(article,2,"https://images.test/cover.jpg");
        jdbc.update("UPDATE articles SET working_revision_id=?,published_revision_id=? WHERE article_id=?",second,first,article);
        var result=mvc.perform(get(path(article)).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2)).andReturn();
        var body=json.readTree(result.getResponse().getContentAsByteArray());
        assertThat(body.path("items").get(0).path("mediaId")).isEqualTo(body.path("items").get(1).path("mediaId"));
        assertThat(body.path("items").get(0).path("revisionId")).isNotEqualTo(body.path("items").get(1).path("revisionId"));
        assertThat(body.toString()).doesNotContain("storageReference","authorId","uploadedAt","uploadedBy");
    }

    @Test void pagination_keeps_all_usages_and_section_row_ids_do_not_define_file_identity() throws Exception {
        var article=article();var revision=revision(article,1,"https://images.test/cover.jpg");var section=UUID.randomUUID();var row=UUID.randomUUID();
        jdbc.update("INSERT INTO article_revision_sections(section_id,revision_id,position,heading) VALUES(?,?,0,'Section')",section,revision);
        jdbc.update("INSERT INTO article_revision_images(image_id,revision_id,section_id,position,storage_reference,width,height,alt) VALUES(?,?,?,0,'https://images.test/cover.jpg',1200,800,'Image')",row,revision,section);
        var first=mvc.perform(get(path(article)).param("limit","1").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andReturn();
        var page=json.readTree(first.getResponse().getContentAsByteArray());
        var second=mvc.perform(get(path(article)).param("limit","1").param("cursor",page.path("nextCursor").asText()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.nextCursor").isEmpty()).andExpect(jsonPath("$.items[0].role").value("SECTION")).andReturn();
        var identity=json.readTree(second.getResponse().getContentAsByteArray()).path("items").get(0).path("mediaId");
        assertThat(identity).isEqualTo(page.path("items").get(0).path("mediaId"));
        jdbc.update("UPDATE article_revision_images SET image_id=? WHERE image_id=?",UUID.randomUUID(),row);
        var after=mvc.perform(get(path(article)).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(after.getResponse().getContentAsByteArray()).path("items").get(1).path("mediaId")).isEqualTo(identity);
    }
    @Test void admin_only_invalid_queries_missing_and_empty_are_distinct() throws Exception {
        var article=article();
        mvc.perform(get(path(article))).andExpect(status().isUnauthorized());
        mvc.perform(get(path(article)).with(jwt())).andExpect(status().isForbidden());
        mvc.perform(get(path(article)).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get(path(UUID.randomUUID())).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isNotFound());
        for(String limit:java.util.List.of("0","101","invalid"))mvc.perform(get(path(article)).param("limit",limit).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
        mvc.perform(get(path(article)).param("cursor","invalid-private-reference").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
    }
    @Test void private_references_outside_article_scope_have_no_preview_and_never_leave_the_backend() throws Exception {
        var article=article();revision(article,1,"s3://private-bucket/other-context/private.jpg");
        var result=mvc.perform(get(path(article)).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].previewUrl").isEmpty()).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("private-bucket","other-context","s3://");
    }
}
