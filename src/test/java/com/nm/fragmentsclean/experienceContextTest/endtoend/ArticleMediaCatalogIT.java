package com.nm.fragmentsclean.experienceContextTest.endtoend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class ArticleMediaCatalogIT extends AbstractExperienceE2E {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting router;
    final String admin="99999999-9999-9999-9999-999999999999";
    final Instant at=Instant.parse("2026-10-04T12:00:00Z");
    UUID media(String ref){return com.nm.fragmentsclean.articleContext.read.ArticleMediaReferenceIdentity.of(ref);}
    Map<String,Object> usage(UUID revision,String ref){
        var data=new HashMap<String,Object>();data.put("mediaId",media(ref));data.put("storageReference",ref);data.put("usageId",revision);data.put("revisionId",revision);data.put("revisionNumber",1);data.put("title","Editorial");data.put("revisionStatus","DRAFT");data.put("role","COVER");data.put("imagePosition",0);data.put("width",1200);data.put("height",800);data.put("alt","Cover");data.put("working",true);data.put("published",false);return data;
    }
    void snapshot(UUID article,long version,int part,int parts,List<Map<String,Object>> references)throws Exception{
        var id=UUID.randomUUID();var body=Map.of("eventId",id,"articleId",article,"version",version,"part",part,"parts",parts,"references",references,"occurredAt",at);
        router.route(new IntegrationEventEnvelope(id.toString(),"article.media_catalog_snapshot",1,"ArticleMediaCatalogSnapshotEvent","ArticleMediaCatalog",article.toString(),"article-media:"+article,"media-catalog-events",json.writeValueAsString(body),at));
    }
    String mediaStatus(UUID id){return jdbc.query("SELECT status FROM media_catalog_entries WHERE origin='ARTICLE' AND media_id=?",(rs,n)->rs.getString(1),id).stream().findFirst().orElse(null);}
    @Test void shared_reference_survives_removal_from_one_article_then_retires_after_the_last_usage()throws Exception{
        var a=UUID.randomUUID();var b=UUID.randomUUID();String ref="https://images.test/shared.jpg";var id=media(ref);
        snapshot(a,1,0,1,List.of(usage(UUID.randomUUID(),ref)));snapshot(b,1,0,1,List.of(usage(UUID.randomUUID(),ref)));
        mvc.perform(get("/api/admin/media/ARTICLE:"+id).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.resourceId").isEmpty());
        snapshot(a,2,0,1,List.of());assertThat(mediaStatus(id)).isEqualTo("AVAILABLE");
        snapshot(a,1,0,1,List.of(usage(UUID.randomUUID(),ref)));
        mvc.perform(get("/api/admin/media").param("q",a.toString()).param("origin","ARTICLE").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        snapshot(b,2,0,1,List.of());assertThat(mediaStatus(id)).isEqualTo("DELETED");
    }

    @Test void incomplete_parts_do_not_replace_an_inventory_and_newer_complete_versions_block_old_parts()throws Exception{
        UUID a=UUID.randomUUID();String old="https://images.test/old.jpg",one="https://images.test/one.jpg",two="https://images.test/two.jpg";
        snapshot(a,1,0,1,List.of(usage(UUID.randomUUID(),old)));
        var refs=List.of(usage(UUID.randomUUID(),one));snapshot(a,2,1,2,List.of(usage(UUID.randomUUID(),two)));
        assertThat(mediaStatus(media(old))).isEqualTo("AVAILABLE");assertThat(mediaStatus(media(two))).isNull();
        snapshot(a,2,0,2,refs);assertThat(mediaStatus(media(old))).isEqualTo("DELETED");assertThat(mediaStatus(media(one))).isEqualTo("AVAILABLE");assertThat(mediaStatus(media(two))).isEqualTo("AVAILABLE");
        snapshot(a,4,0,1,List.of());snapshot(a,3,1,2,List.of(usage(UUID.randomUUID(),old)));snapshot(a,3,0,2,List.of(usage(UUID.randomUUID(),old)));
        assertThat(mediaStatus(media(old))).isEqualTo("DELETED");assertThat(jdbc.queryForObject("SELECT count(*) FROM media_catalog_article_parts WHERE article_id=?",Integer.class,a)).isZero();
    }
    @Test void source_replay_is_bounded_and_uses_real_stable_envelopes()throws Exception{
        var a=sourceArticle("Replay article");sourceRevision(a,"https://images.test/replay.jpg");
        jdbc.update("UPDATE article_media_catalog_scan SET cursor_id=NULL,next_scan_at=now() WHERE id=1");
        assertThat(replay.nextBatch()).isPositive();routeOutbox(a);
        mvc.perform(get("/api/admin/media").param("q",a.toString()).param("origin","ARTICLE").with(jwt().jwt(j->j.subject(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].createdAt").isEmpty()).andExpect(jsonPath("$.items[0].ownerId").isEmpty());
        assertThat(replay.nextBatch()).isZero();
    }
    @Test void additive_fragment_can_be_reapplied_and_article_detail_keeps_admin_authorization()throws Exception{
        try(var connection=jdbc.getDataSource().getConnection()){
            var script=new org.springframework.core.io.ClassPathResource("db/release/2026-10-04-article-media-catalogue.sql");
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,script);
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,script);
        }
        UUID a=sourceArticle("Detail");sourceRevision(a,"https://images.test/detail.jpg");
        mvc.perform(get("/api/admin/studio/articles/"+a).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.articleId").value(a.toString()));
        mvc.perform(get("/api/admin/studio/articles/"+a)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/studio/articles/"+a).with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/studio/articles/"+UUID.randomUUID()).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isNotFound());
    }
    @Test void rejects_unknown_event_contract_versions_before_mutating_projection()throws Exception{
        UUID a=UUID.randomUUID(),event=UUID.randomUUID();
        var body=Map.of("eventId",event,"articleId",a,"version",1,"part",0,"parts",1,"references",List.of(),"occurredAt",at);
        var envelope=new IntegrationEventEnvelope(event.toString(),"article.media_catalog_snapshot",2,"ArticleMediaCatalogSnapshotEvent","ArticleMediaCatalog",a.toString(),"article-media:"+a,"media-catalog-events",json.writeValueAsString(body),at);
        org.assertj.core.api.Assertions.assertThatThrownBy(()->router.route(envelope)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_catalog_article_versions WHERE article_id=?",Integer.class,a)).isZero();
    }
    @Test void paginates_retained_revision_usages_without_exposing_storage_references()throws Exception{
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();String ref="https://images.test/pagination-"+a+".jpg";
        snapshot(a,1,0,1,List.of(usage(UUID.randomUUID(),ref)));snapshot(b,1,0,1,List.of(usage(UUID.randomUUID(),ref)));
        String path="/api/admin/media/ARTICLE:"+media(ref)+"/usages";
        var response=mvc.perform(get(path).param("limit","1").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andReturn().getResponse().getContentAsString();
        var page=json.readTree(response);assertThat(response).doesNotContain(ref,"storageReference");String cursor=page.get("nextCursor").asText();
        var next=mvc.perform(get(path).param("limit","1").param("cursor",cursor).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(next).get("items").get(0).get("articleId").asText()).isNotEqualTo(page.get("items").get(0).get("articleId").asText());
    }
    @Autowired com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaUploadRecorder uploads;
    @Test void registered_upload_survives_removing_its_last_content_reference_and_preserves_first_actor()throws Exception{
        UUID a=UUID.randomUUID(),actor=UUID.randomUUID();String ref="https://images.test/registered-"+a+".png";
        uploads.record(a,ref,"coffee.png","image/png",new byte[]{1,2,3},640,480,actor,"STUDIO");routeOutbox(a);
        mvc.perform(get("/api/admin/media/ARTICLE:"+media(ref)).with(jwt().jwt(j->j.subject(admin))))
          .andExpect(status().isOk()).andExpect(jsonPath("$.usageStatus").value("UNUSED")).andExpect(jsonPath("$.size").value(3)).andExpect(jsonPath("$.uploadedBy").value(actor.toString())).andExpect(jsonPath("$.ownerId").isEmpty()).andExpect(jsonPath("$.previewUrl").value(ref));
        uploads.record(a,ref,"retry.png","image/png",new byte[]{1,2,3,4},640,480,UUID.randomUUID(),"STUDIO");routeOutbox(a);
        assertThat(jdbc.queryForObject("SELECT uploaded_by FROM article_media_uploads WHERE media_id=?",UUID.class,media(ref))).isEqualTo(actor);
        // Register the source article using the same id, then add/remove a retained revision reference.
        jdbc.update("INSERT INTO articles(article_id,slug,locale,author_id,author_name,title,intro,blocks_json,conclusion,tags_json,reading_time_min,coffee_ids_json,created_at,updated_at,status,version) VALUES(?,?,'fr-FR',?,'Editorial','Tracked','Intro','[]','Fin','[]',1,'[]',now(),now(),'DRAFT',1)",a,a.toString(),actor);
        UUID revision=sourceRevision(a,ref);producer.publish(a);routeOutbox(a);
        mvc.perform(get("/api/admin/media/ARTICLE:"+media(ref)).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.usageStatus").value("USED")).andExpect(jsonPath("$.articleUsages.items.length()").value(1));
        jdbc.update("UPDATE article_revisions SET cover_reference=NULL,cover_width=NULL,cover_height=NULL,cover_alt=NULL WHERE revision_id=?",revision);producer.publish(a);routeOutbox(a);
        mvc.perform(get("/api/admin/media/ARTICLE:"+media(ref)).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.usageStatus").value("UNUSED")).andExpect(jsonPath("$.status").value("AVAILABLE"));
        mvc.perform(get("/api/admin/media").param("q","coffee.png").param("usage","UNUSED").with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.items.length()").value(1));
    }
    @Test void preview_is_revoked_by_source_even_when_catalogue_is_stale_and_usages_are_admin_only()throws Exception{
        UUID a=sourceArticle("Revocation");String ref="https://images.test/revoked-"+a+".jpg";UUID revision=sourceRevision(a,ref);producer.publish(a);routeOutbox(a);
        String path="/api/admin/media/ARTICLE:"+media(ref);
        mvc.perform(get(path).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.previewUrl").value(ref));
        jdbc.update("UPDATE article_revisions SET cover_reference=NULL,cover_width=NULL,cover_height=NULL,cover_alt=NULL WHERE revision_id=?",revision);
        mvc.perform(get(path).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.previewUrl").isEmpty());
        mvc.perform(get(path+"/usages")).andExpect(status().isUnauthorized());
        mvc.perform(get(path+"/usages").with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());
        mvc.perform(get(path+"/usages").param("limit","0").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isBadRequest());
        mvc.perform(get(path+"/usages").param("cursor","not-a-cursor").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/media/ARTICLE:"+UUID.randomUUID()+"/usages").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isNotFound());
    }
    @Autowired com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.ReplayArticleMediaCatalog replay;
    @Autowired com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaCatalogPublisher producer;
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository outbox;
    UUID sourceArticle(String title){
        var id=UUID.randomUUID();jdbc.update("INSERT INTO articles(article_id,slug,locale,author_id,author_name,title,intro,blocks_json,conclusion,tags_json,reading_time_min,coffee_ids_json,created_at,updated_at,status,version) VALUES(?,?,'fr-FR',?,'Editorial',?,'Intro','[]','Fin','[]',1,'[]',now(),now(),'DRAFT',1)",id,id.toString(),UUID.randomUUID(),title);return id;
    }
    UUID sourceRevision(UUID a,String reference){
        var revision=UUID.randomUUID();jdbc.update("INSERT INTO article_revisions(revision_id,article_id,revision_number,title,introduction,conclusion,cover_reference,cover_width,cover_height,cover_alt,reading_time_min,status,created_at,updated_at,version) VALUES(?,?,1,'Replay article','Intro','Fin',?,1200,800,'Cover',1,'DRAFT',now(),now(),1)",revision,a,reference);jdbc.update("UPDATE articles SET working_revision_id=? WHERE article_id=?",revision,a);return revision;
    }
    void routeOutbox(UUID a){
        var factory=new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory();var destinations=new com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinationResolver();
        for(var event:outbox.findAll())if(event.getEventType().endsWith("ArticleMediaCatalogSnapshotEvent") && event.getAggregateId().equals(a.toString())){
            assertThat(destinations.destinationsFor(event)).containsExactly("media-catalog-events");router.route(factory.from(event,"media-catalog-events"));
        }
    }
}
