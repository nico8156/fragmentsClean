package com.nm.fragmentsclean.experienceContextTest.endtoend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class MediaCatalogIT extends AbstractExperienceE2E {
    @Autowired MockMvc mvc;
    static final String ADMIN="99999999-9999-9999-9999-999999999999";
    @Test void catalogue_is_admin_only_and_announces_its_coverage() throws Exception {
        mvc.perform(get("/api/admin/media")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/media").with(jwt())).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/media").with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.coverage[0]").value("EXPERIENCE"));
    }

    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting router;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final java.time.Instant at=java.time.Instant.parse("2026-10-04T10:00:00Z");
    @Test void events_are_projected_once_and_old_deliveries_cannot_resurrect_a_deleted_media() throws Exception {
        var id=java.util.UUID.randomUUID(); var owner=java.util.UUID.randomUUID();
        var available=event(id,owner,"AVAILABLE",1);
        jdbc.update("INSERT INTO experience_media(media_id,experience_id,coffee_id,user_id,declared_content_type,declared_size,pending_object_key,status,object_key,content_type,size_bytes,width,height,sha256,created_at,updated_at,version) VALUES(?,?,?,?,'image/jpeg',1024,?,'AVAILABLE',?,'image/jpeg',1024,640,480,'sha',?,?,1)",id,available.experienceId(),available.coffeeId(),owner,"pending/"+id,"experiences/current.jpg",java.sql.Timestamp.from(at),java.sql.Timestamp.from(at));
        route("experience.media.changed",available);route("experience.media.changed",available);
        mvc.perform(get("/api/admin/media/EXPERIENCE:"+id).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(owner.toString()))
            .andExpect(jsonPath("$.previewUrl").value(org.hamcrest.Matchers.startsWith("https://download.test/")))
            .andExpect(jsonPath("$.objectKey").doesNotExist());
        jdbc.update("UPDATE experience_media SET status='DELETION_PENDING' WHERE media_id=?",id);
        mvc.perform(get("/api/admin/media/EXPERIENCE:"+id).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.previewUrl").isEmpty());
        route("experience.media.changed",event(id,null,"DELETED",3));
        route("experience.media.changed",event(id,owner,"AVAILABLE",1));
        mvc.perform(get("/api/admin/media/EXPERIENCE:"+id).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELETED"))
            .andExpect(jsonPath("$.ownerId").isEmpty()).andExpect(jsonPath("$.resourceId").isEmpty())
            .andExpect(jsonPath("$.previewUrl").isEmpty());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_catalog_entries WHERE media_id=?",Integer.class,id)).isEqualTo(1);
    }
    @Test void erasure_blocks_late_events_and_backfill_and_removes_existing_associations() throws Exception {
        var owner=java.util.UUID.randomUUID();var id=java.util.UUID.randomUUID();
        route("experience.media.changed",event(id,owner,"AVAILABLE",1));
        route("app.user.deletion_requested",new com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent(java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),owner,owner,1,at));
        route("experience.media.changed",event(id,owner,"AVAILABLE",2));
        mvc.perform(get("/api/admin/media").param("ownerId",owner.toString()).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/admin/media/EXPERIENCE:"+id).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isNotFound());
    }
    @Test void pagination_filters_and_invalid_identifiers_are_checked_on_server() throws Exception {
        var owner=java.util.UUID.randomUUID();
        for(int i=0;i<3;i++)route("experience.media.changed",event(java.util.UUID.randomUUID(),owner,i==2?"PENDING":"AVAILABLE",1));
        var response=mvc.perform(get("/api/admin/media").param("ownerId",owner.toString()).param("status","AVAILABLE").param("limit","1").with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andReturn();
        var page=json.readTree(response.getResponse().getContentAsByteArray());
        var next=mvc.perform(get("/api/admin/media").param("ownerId",owner.toString()).param("status","AVAILABLE").param("limit","1").param("cursor",page.path("nextCursor").asText()).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty()).andReturn();
        assertThat(json.readTree(next.getResponse().getContentAsByteArray()).path("items").get(0).path("id")).isNotEqualTo(page.path("items").get(0).path("id"));
        for(String field:java.util.List.of("cursor","origin","status","limit"))mvc.perform(get("/api/admin/media").param(field,"invalid").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/media/EXPERIENCE:"+java.util.UUID.randomUUID()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isNotFound());
    }

    @Autowired com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.ReplayExperienceMediaCatalog replay;
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository outbox;
    @Test void source_owned_replay_preserves_dates_and_cannot_overwrite_a_newer_projection() throws Exception {
        var media=java.util.UUID.randomUUID();var owner=java.util.UUID.randomUUID();
        jdbc.update("INSERT INTO experience_media(media_id,experience_id,coffee_id,user_id,declared_content_type,declared_size,pending_object_key,status,created_at,updated_at,version) VALUES(?,?,?,?,'image/jpeg',1024,?,'PENDING',?,?,0)",media,java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),owner,"pending/"+media,java.sql.Timestamp.from(at),java.sql.Timestamp.from(at));
        jdbc.update("UPDATE experience_media_catalog_scan SET cursor_id=NULL,next_scan_at=now() WHERE id=1");
        assertThat(replay.nextBatch()).isPositive();
        var events=outbox.findAll().stream().filter(e->e.getEventType().endsWith("ExperienceMediaChangedEvent") && e.getPayloadJson().contains(media.toString())).toList();
        assertThat(events).hasSize(1);
        var envelope=new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory().from(events.getFirst(),"media-catalog-events");
        router.route(envelope);
        mvc.perform(get("/api/admin/media/EXPERIENCE:"+media).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.createdAt").value(at.toString())).andExpect(jsonPath("$.status").value("PENDING"));
        route("experience.media.changed",event(media,owner,"AVAILABLE",1));
        jdbc.update("UPDATE experience_media_catalog_scan SET cursor_id=NULL,next_scan_at=now() WHERE id=1");
        replay.nextBatch();
        for(var e:outbox.findAll())if(e.getEventType().endsWith("ExperienceMediaChangedEvent") && e.getPayloadJson().contains(media.toString()))router.route(new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory().from(e,"media-catalog-events"));
        mvc.perform(get("/api/admin/media/EXPERIENCE:"+media).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AVAILABLE")).andExpect(jsonPath("$.createdAt").value(at.toString()));
        assertThat(replay.nextBatch()).isZero();
        route("app.user.deletion_requested",new com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent(java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),owner,owner,1,at));
        jdbc.update("UPDATE experience_media_catalog_scan SET cursor_id=NULL,next_scan_at=now() WHERE id=1");
        replay.nextBatch();
        for(var e:outbox.findAll())if(e.getEventType().endsWith("ExperienceMediaChangedEvent") && e.getPayloadJson().contains(media.toString()))router.route(new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory().from(e,"media-catalog-events"));
        mvc.perform(get("/api/admin/media/EXPERIENCE:"+media).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isNotFound());
    }

    @Test void migration_is_replayable_and_carries_forward_old_erasure_barriers() throws Exception {
        String schema="media_migration_"+java.util.UUID.randomUUID().toString().replace("-","");
        try(var connection=jdbc.getDataSource().getConnection();var statement=connection.createStatement()) {
            statement.execute("CREATE SCHEMA "+schema);
            try {
                statement.execute("SET search_path TO "+schema);
                statement.execute("CREATE TABLE account_erasure_barriers (LIKE public.account_erasure_barriers INCLUDING ALL)");
                var user=java.util.UUID.randomUUID();
                statement.execute("INSERT INTO account_erasure_barriers(context_name,user_id,status,request_id,erased_at,created_at,updated_at) VALUES ('EXPERIENCE','"+user+"','ERASED','"+java.util.UUID.randomUUID()+"',now(),now(),now())");
                String migration=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/release/2026-10-04-media-catalogue.sql"));
                statement.execute(migration);statement.execute(migration);
                try(var rows=statement.executeQuery("SELECT status FROM account_erasure_barriers WHERE context_name='MEDIA_CATALOG' AND user_id='"+user+"'")) { assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("ERASED"); }
            } finally {statement.execute("SET search_path TO public");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
        }
    }

    @Test void a_stale_catalogue_never_signs_a_preview_when_the_source_is_not_available() throws Exception {
        var id=java.util.UUID.randomUUID();var owner=java.util.UUID.randomUUID();
        route("experience.media.changed",event(id,owner,"AVAILABLE",1));
        mvc.perform(get("/api/admin/media/EXPERIENCE:"+id).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.previewUrl").isEmpty());
    }
    private com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents.MediaChanged event(java.util.UUID id,java.util.UUID owner,String status,long version) {
        return new com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents.MediaChanged(java.util.UUID.randomUUID(),java.util.UUID.randomUUID(),id,java.util.UUID.randomUUID(),owner,java.util.UUID.randomUUID(),status,"experiences/test.jpg","image/jpeg",1024,640,480,"test",version,at,null);
    }
    private void route(String type,Object event)throws Exception {
        var tree=json.valueToTree(event);
        router.route(new com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope(tree.path("eventId").asText(),type,1,event.getClass().getName(),"ExperienceMedia",java.util.UUID.randomUUID().toString(),"media-test","media-catalog-events",json.writeValueAsString(event),at));
    }
}
