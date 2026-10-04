package com.nm.fragmentsclean.experienceContextTest.endtoend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nm.fragmentsclean.platform.eventing.contracts.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class AvatarMediaCatalogIT extends AbstractExperienceE2E {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting router;
    final Instant at=Instant.parse("2026-10-04T10:00:00Z");
    final String admin="99999999-9999-9999-9999-999999999999";
    @Test void avatar_metadata_and_owner_are_admin_only_and_source_preview_is_current() throws Exception {
        UUID owner=UUID.randomUUID(),id=UUID.randomUUID();source(owner,id);
        routeAvatar(event(id,owner,owner,"AVAILABLE",1));
        mvc.perform(get("/api/admin/media/AVATAR:"+id)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/media/AVATAR:"+id).with(jwt())).andExpect(status().isForbidden());
        detail(id).andExpect(status().isOk()).andExpect(jsonPath("$.origin").value("AVATAR"))
            .andExpect(jsonPath("$.ownerId").value(owner.toString())).andExpect(jsonPath("$.resourceId").value(owner.toString()))
            .andExpect(jsonPath("$.createdAt").value(at.toString())).andExpect(jsonPath("$.previewUrl").value("https://download.test/avatars/"+id))
            .andExpect(jsonPath("$.objectKey").doesNotExist());
        jdbc.update("UPDATE app_users SET avatar_url='https://provider.test/replacement.jpg' WHERE id=?",owner);
        detail(id).andExpect(status().isOk()).andExpect(jsonPath("$.previewUrl").isEmpty());
        jdbc.update("UPDATE app_users SET avatar_url=?,lifecycle_status='DELETION_REQUESTED' WHERE id=?","media:avatar:avatars/"+id,owner);
        detail(id).andExpect(status().isOk()).andExpect(jsonPath("$.previewUrl").isEmpty());
    }
    @Test void replacement_retirement_and_deletion_cannot_be_reversed_by_old_events() throws Exception {
        UUID owner=UUID.randomUUID(),id=UUID.randomUUID();
        var active=event(id,owner,owner,"AVAILABLE",1);routeAvatar(active);routeAvatar(active);
        routeAvatar(event(id,owner,null,"DELETION_PENDING",2));routeAvatar(event(id,owner,owner,"AVAILABLE",1));
        detail(id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELETION_PENDING"))
            .andExpect(jsonPath("$.resourceId").isEmpty()).andExpect(jsonPath("$.previewUrl").isEmpty());
        routeAvatar(event(id,null,null,"DELETED",3));routeAvatar(active);
        detail(id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELETED"))
            .andExpect(jsonPath("$.ownerId").isEmpty()).andExpect(jsonPath("$.resourceId").isEmpty()).andExpect(jsonPath("$.size").isEmpty());
        assertThat(jdbc.queryForObject("SELECT object_key FROM media_catalog_entries WHERE origin='AVATAR' AND media_id=?",String.class,id)).isNull();
    }
    @Test void account_erasure_blocks_late_avatar_facts_and_removes_associations() throws Exception {
        UUID owner=UUID.randomUUID(),id=UUID.randomUUID();routeAvatar(event(id,owner,owner,"AVAILABLE",1));
        detail(id).andExpect(status().isOk());
        var request=new AppUserDeletionRequestedIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),owner,owner,1,at);
        route("app.user.deletion_requested",request);
        routeAvatar(event(id,owner,owner,"AVAILABLE",4));
        detail(id).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/media").param("ownerId",owner.toString()).with(jwt().jwt(j->j.subject(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
    }
    @Test void pending_avatars_do_not_claim_profile_usage_and_are_filterable() throws Exception {
        UUID owner=UUID.randomUUID(),id=UUID.randomUUID();routeAvatar(event(id,owner,null,"PENDING",0));
        mvc.perform(get("/api/admin/media").param("ownerId",owner.toString()).param("origin","AVATAR").param("status","PENDING").with(jwt().jwt(j->j.subject(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].resourceId").isEmpty()).andExpect(jsonPath("$.items[0].previewUrl").isEmpty());
    }
    @Autowired com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.ReplayAvatarMediaCatalog replay;
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository outbox;
    @Test void source_replay_resumes_batches_and_uses_stable_avatar_envelopes() throws Exception {
        UUID owner=UUID.randomUUID(),id=UUID.randomUUID();source(owner,id);
        jdbc.update("UPDATE avatar_media_catalog_scan SET cursor_id=NULL,next_scan_at=now(),completed_at=NULL");
        for(int i=0;i<100;i++){
            UUID pending=UUID.randomUUID();
            jdbc.update("INSERT INTO user_avatar_media(media_id,user_id,declared_content_type,declared_size,pending_object_key,status,created_at,updated_at,version) VALUES(?,?,'image/png',256,?,'PENDING',?,?,0)",pending,owner,"pending/"+pending,java.sql.Timestamp.from(at),java.sql.Timestamp.from(at));
        }
        assertThat(replay.nextBatch()).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT cursor_id FROM avatar_media_catalog_scan WHERE id=1",UUID.class)).isNotNull();
        assertThat(replay.nextBatch()).isBetween(1,10);
        var snapshots=outbox.findAll().stream().filter(e->e.getEventType().endsWith("AvatarMediaChangedEvent") && e.getPayloadJson().contains(id.toString())).toList();
        assertThat(snapshots).hasSize(1);
        var snapshot=snapshots.getFirst();
        assertThat(new com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinationResolver().destinationsFor(snapshot)).containsExactly("media-catalog-events");
        var envelope=new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory(json).from(snapshot,"media-catalog-events");
        assertThat(envelope.eventType()).isEqualTo("avatar.media.changed");router.route(envelope);
        detail(id).andExpect(status().isOk()).andExpect(jsonPath("$.resourceId").value(owner.toString())).andExpect(jsonPath("$.createdAt").value(at.toString()));
        assertThat(replay.nextBatch()).isZero();
    }
    @Test void avatar_migration_preserves_historical_user_erasure_and_is_replayable() throws Exception {
        String schema="avatar_catalog_migration_"+UUID.randomUUID().toString().replace("-","");UUID owner=UUID.randomUUID();
        try(var connection=jdbc.getDataSource().getConnection();var statement=connection.createStatement()){
            statement.execute("CREATE SCHEMA "+schema);
            try {
                statement.execute("SET search_path TO "+schema);
                statement.execute("CREATE TABLE account_erasure_barriers (LIKE public.account_erasure_barriers INCLUDING ALL)");
                statement.execute("INSERT INTO account_erasure_barriers(context_name,user_id,status,request_id,erased_at,created_at,updated_at) VALUES('USER_APPLICATION','"+owner+"','ERASED','"+UUID.randomUUID()+"',now(),now(),now())");
                String sql=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/release/2026-10-04-avatar-media-catalogue.sql"));
                statement.execute(sql);statement.execute(sql);
                try(var rows=statement.executeQuery("SELECT count(*) FROM avatar_media_catalog_scan")){assertThat(rows.next()).isTrue();assertThat(rows.getInt(1)).isEqualTo(1);}
                try(var rows=statement.executeQuery("SELECT status FROM account_erasure_barriers WHERE context_name='MEDIA_CATALOG' AND user_id='"+owner+"'")){assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("ERASED");}
            } finally {statement.execute("SET search_path TO public");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
        }
    }
    private void source(UUID owner,UUID id){
        jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email,email_verified,display_name,avatar_url,last_login_at) VALUES(?,'GOOGLE',?,'avatar@example.test',true,'Avatar',null,?)",owner,owner.toString(),java.sql.Timestamp.from(at));
        jdbc.update("INSERT INTO app_users(id,auth_user_id,display_name,avatar_url,created_at,updated_at,version,lifecycle_status) VALUES(?,?,'Avatar catalogue',?,?,?,1,'ACTIVE')",owner,owner,"media:avatar:avatars/"+id,java.sql.Timestamp.from(at),java.sql.Timestamp.from(at));
        jdbc.update("INSERT INTO user_avatar_media(media_id,user_id,declared_content_type,declared_size,pending_object_key,status,object_key,content_type,size_bytes,width,height,sha256,created_at,updated_at,version) VALUES(?,?,'image/jpeg',1024,?,'AVAILABLE',?,'image/jpeg',1024,512,512,'hash',?,?,1)",id,owner,"pending/"+id,"avatars/"+id,java.sql.Timestamp.from(at),java.sql.Timestamp.from(at));
    }
    private AvatarMediaChangedIntegrationEvent event(UUID id,UUID owner,UUID profile,String status,long version){return new AvatarMediaChangedIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),id,owner,profile,status,"avatars/"+id,"image/jpeg",1024,512,512,at,version,at);}
    private org.springframework.test.web.servlet.ResultActions detail(UUID id)throws Exception{return mvc.perform(get("/api/admin/media/AVATAR:"+id).with(jwt().jwt(j->j.subject(admin))));}
    private void routeAvatar(Object event)throws Exception{route("avatar.media.changed",event);}
    private void route(String type,Object event)throws Exception{router.route(new IntegrationEventEnvelope(json.valueToTree(event).path("eventId").asText(),type,1,event.getClass().getName(),"AvatarMedia",UUID.randomUUID().toString(),"avatar-test","media-catalog-events",json.writeValueAsString(event),at));}
}
