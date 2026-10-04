package com.nm.fragmentsclean.experienceContextTest.endtoend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class AvatarMediaLifecycleIT extends AbstractExperienceE2E {
    static final String ADMIN="99999999-9999-9999-9999-999999999999";
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Autowired com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider clock;
    @Test void purge_is_an_explicit_audited_request_after_retention_not_an_immediate_storage_success() throws Exception {
        UUID owner=UUID.randomUUID(),media=UUID.randomUUID(),command=UUID.randomUUID();seed(owner,media,null);
        var retiredAt=clock.now().minus(java.time.Duration.ofDays(31));
        jdbc.update("UPDATE user_avatar_media SET status='RETIRED',created_at=?,updated_at=? WHERE media_id=?",java.sql.Timestamp.from(retiredAt.minusSeconds(86400)),java.sql.Timestamp.from(retiredAt),media);
        String route="/api/admin/studio/avatar-media/"+media;
        mvc.perform(get(route).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk())
            .andExpect(jsonPath("$.canPurge").value(true)).andExpect(jsonPath("$.retiredAt").value(retiredAt.toString()))
            .andExpect(jsonPath("$.purgeEligibleAt").value(retiredAt.plus(java.time.Duration.ofDays(30)).toString()));
        assertThat(sourceMedia.cleanupCandidates(clock.now(),100).stream().map(m->m.id())).doesNotContain(media);
        change(media,"PURGE_REQUESTED","Rétention terminée",command).andExpect(status().isAccepted());
        change(media,"PURGE_REQUESTED","Rétention terminée",command).andExpect(status().isAccepted());
        assertThat(jdbc.queryForObject("SELECT status FROM user_avatar_media WHERE media_id=?",String.class,media)).isEqualTo("DELETION_PENDING");
        assertThat(jdbc.queryForObject("SELECT object_key FROM user_avatar_media WHERE media_id=?",String.class,media)).isEqualTo("avatars/"+media);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit_log WHERE command_id=?",Integer.class,command)).isOne();
        mvc.perform(get(route+"/operations").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].action").value("AVATAR_MEDIA_PURGE_REQUESTED")).andExpect(jsonPath("$[0].actorUserId").value(ADMIN)).andExpect(jsonPath("$[0].reason").value("Rétention terminée"));
        mvc.perform(get(route).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.canPurge").value(false)).andExpect(jsonPath("$.canRestore").value(false));
        assertThat(sourceMedia.cleanupCandidates(clock.now(),100).stream().map(m->m.id())).contains(media);
        change(media,"AVAILABLE","Restore",UUID.randomUUID()).andExpect(status().isUnprocessableEntity());
    }
    @Test void source_refuses_purge_before_retention_or_while_a_profile_still_uses_the_file() throws Exception {
        UUID owner=UUID.randomUUID(),media=UUID.randomUUID();seed(owner,media,null);
        var retiredAt=clock.now().minus(java.time.Duration.ofDays(30)).plusMillis(1);
        jdbc.update("UPDATE user_avatar_media SET status='RETIRED',created_at=?,updated_at=? WHERE media_id=?",java.sql.Timestamp.from(retiredAt.minusSeconds(86400)),java.sql.Timestamp.from(retiredAt),media);
        mvc.perform(get("/api/admin/studio/avatar-media/"+media).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.canPurge").value(false));
        change(media,"PURGE_REQUESTED","Early",UUID.randomUUID()).andExpect(status().isUnprocessableEntity());
        jdbc.update("UPDATE user_avatar_media SET updated_at=? WHERE media_id=?",java.sql.Timestamp.from(clock.now().minus(java.time.Duration.ofDays(31))),media);
        jdbc.update("UPDATE app_users SET avatar_url=? WHERE id=?","media:avatar:avatars/"+media,owner);
        mvc.perform(get("/api/admin/studio/avatar-media/"+media).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.canPurge").value(false));
        change(media,"PURGE_REQUESTED","Used",UUID.randomUUID()).andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT status FROM user_avatar_media WHERE media_id=?",String.class,media)).isEqualTo("RETIRED");
    }
    @Test void unused_available_avatar_can_be_retired_without_deleting_the_object() throws Exception {
        UUID owner=UUID.randomUUID(), media=UUID.randomUUID(); seed(owner,media,null);
        mvc.perform(post("/api/admin/studio/avatar-media/"+media+"/lifecycle")
            .with(jwt().jwt(j->j.subject(ADMIN))).contentType("application/json")
            .content("{\"commandId\":\""+UUID.randomUUID()+"\",\"status\":\"RETIRED\",\"reason\":\"Ancien fichier\"}"))
            .andExpect(status().isAccepted());
        assertThat(jdbc.queryForObject("SELECT status FROM user_avatar_media WHERE media_id=?",String.class,media)).isEqualTo("RETIRED");
        assertThat(jdbc.queryForObject("SELECT object_key FROM user_avatar_media WHERE media_id=?",String.class,media)).isEqualTo("avatars/"+media);
    }
    @Test void restoration_returns_the_file_without_assigning_it_to_the_profile() throws Exception {
        UUID owner=UUID.randomUUID(), media=UUID.randomUUID(); seed(owner,media,null);
        jdbc.update("UPDATE user_avatar_media SET status='RETIRED' WHERE media_id=?",media);
        mvc.perform(post("/api/admin/studio/avatar-media/"+media+"/lifecycle")
            .with(jwt().jwt(j->j.subject(ADMIN))).contentType("application/json")
            .content("{\"commandId\":\""+UUID.randomUUID()+"\",\"status\":\"AVAILABLE\",\"reason\":\"Restaurer le fichier\"}"))
            .andExpect(status().isAccepted());
        assertThat(jdbc.queryForObject("SELECT status FROM user_avatar_media WHERE media_id=?",String.class,media)).isEqualTo("AVAILABLE");
        assertThat(jdbc.queryForObject("SELECT avatar_url FROM app_users WHERE id=?",String.class,owner)).isNull();
    }
    @Test void source_capabilities_protect_an_avatar_still_used_by_a_profile() throws Exception {
        UUID owner=UUID.randomUUID(), media=UUID.randomUUID(); seed(owner,media,"media:avatar:avatars/"+media);
        mvc.perform(get("/api/admin/studio/avatar-media/"+media).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.usages").value(1))
            .andExpect(jsonPath("$.canRetire").value(false)).andExpect(jsonPath("$.canRestore").value(false));
    }
    @Test void used_avatar_is_rejected_and_same_command_is_audited_once() throws Exception {
        UUID owner=UUID.randomUUID(), used=UUID.randomUUID(); seed(owner,used,"media:avatar:avatars/"+used);
        change(used,"RETIRED","Ancien fichier",UUID.randomUUID()).andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT status FROM user_avatar_media WHERE media_id=?",String.class,used)).isEqualTo("AVAILABLE");
        UUID unusedOwner=UUID.randomUUID(),unused=UUID.randomUUID(),command=UUID.randomUUID();seed(unusedOwner,unused,null);
        change(unused,"RETIRED"," Ancien fichier ",command).andExpect(status().isAccepted());
        change(unused,"RETIRED"," Ancien fichier ",command).andExpect(status().isAccepted());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit_log WHERE command_id=?",Integer.class,command)).isOne();
        mvc.perform(get("/api/admin/commands/"+command).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPLIED"));
        mvc.perform(get("/api/admin/studio/avatar-media/"+unused+"/operations").with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].actorUserId").value(ADMIN))
            .andExpect(jsonPath("$[0].action").value("AVATAR_MEDIA_RETIRED"))
            .andExpect(jsonPath("$[0].reason").value("Ancien fichier"));
        change(unused,"RETIRED","Different intent",command).andExpect(status().isConflict());
    }
    @Test void replacement_conflict_and_deletion_states_cannot_be_restored() throws Exception {
        UUID owner=UUID.randomUUID(),retired=UUID.randomUUID(),replacement=UUID.randomUUID();seed(owner,retired,null);
        change(retired,"RETIRED","Unused",UUID.randomUUID()).andExpect(status().isAccepted());
        jdbc.update("INSERT INTO user_avatar_media(media_id,user_id,declared_content_type,declared_size,pending_object_key,status,object_key,content_type,size_bytes,width,height,sha256,created_at,updated_at,version) VALUES(?,?,'image/jpeg',1024,?,'AVAILABLE',?,'image/jpeg',1024,512,512,'hash',now(),now(),1)",replacement,owner,"pending/"+replacement,"avatars/"+replacement);
        mvc.perform(get("/api/admin/studio/avatar-media/"+retired).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.canRestore").value(false));
        change(retired,"AVAILABLE","Restore",UUID.randomUUID()).andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT status FROM user_avatar_media WHERE media_id=?",String.class,retired)).isEqualTo("RETIRED");
        jdbc.update("UPDATE user_avatar_media SET status='DELETION_PENDING' WHERE media_id=?",replacement);
        change(replacement,"AVAILABLE","Restore",UUID.randomUUID()).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/admin/studio/avatar-media/"+replacement).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.retention").value("EXISTING_SOURCE_LIFECYCLE"));
        jdbc.update("UPDATE app_users SET lifecycle_status='DELETION_REQUESTED' WHERE id=?",owner);
        change(retired,"AVAILABLE","Restore",UUID.randomUUID()).andExpect(status().isUnprocessableEntity());
    }
    @Test void admin_identity_validation_and_missing_resources_are_enforced() throws Exception {
        UUID owner=UUID.randomUUID(),media=UUID.randomUUID();seed(owner,media,null);
        String route="/api/admin/studio/avatar-media/"+media;
        mvc.perform(get(route)).andExpect(status().isUnauthorized());
        mvc.perform(get(route).with(jwt())).andExpect(status().isForbidden());
        mvc.perform(post(route+"/lifecycle").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(post(route+"/lifecycle").with(jwt()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post(route+"/lifecycle").with(jwt().jwt(j->j.subject(ADMIN))).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
        change(media,"RETIRED"," ",UUID.randomUUID()).andExpect(status().isUnprocessableEntity());
        change(media,"PURGE","Unused",UUID.randomUUID()).andExpect(status().isUnprocessableEntity());
        change(UUID.randomUUID(),"RETIRED","Unused",UUID.randomUUID()).andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/admin/studio/avatar-media/"+UUID.randomUUID()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isNotFound());
        mvc.perform(get(route+"/operations").with(jwt())).andExpect(status().isForbidden());
    }
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository outbox;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting router;
    @Autowired com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AvatarMediaRepository sourceMedia;
    @Test void retired_media_is_not_cleaned_and_its_fact_reaches_the_catalogue() throws Exception {
        UUID owner=UUID.randomUUID(),media=UUID.randomUUID();seed(owner,media,null);
        change(media,"RETIRED","Unused",UUID.randomUUID()).andExpect(status().isAccepted());
        var fact=outbox.findAll().stream().filter(e->e.getEventType().endsWith("AvatarMediaChangedEvent")&&e.getPayloadJson().contains(media.toString())).findFirst().orElseThrow();
        router.route(new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory(json).from(fact,"media-catalog-events"));
        mvc.perform(get("/api/admin/media/AVATAR:"+media).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELETION_PENDING"))
            .andExpect(jsonPath("$.previewUrl").isEmpty()).andExpect(jsonPath("$.ownerId").value(owner.toString()));
        assertThat(sourceMedia.cleanupCandidates(java.time.Instant.now().plusSeconds(86400),100).stream().map(m->m.id())).doesNotContain(media);
        mvc.perform(get("/api/admin/studio/avatar-media/"+media).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.canRestore").value(true))
            .andExpect(jsonPath("$.retention").value("INDEFINITE_NO_AUTOMATIC_PURGE"));
        change(media,"AVAILABLE","Restore",UUID.randomUUID()).andExpect(status().isAccepted());
        var restored=outbox.findAll().stream().filter(e->e.getEventType().endsWith("AvatarMediaChangedEvent")&&e.getPayloadJson().contains(media.toString())&&e.getPayloadJson().contains("AVAILABLE")).findFirst().orElseThrow();
        router.route(new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory(json).from(restored,"media-catalog-events"));
        router.route(new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory(json).from(fact,"media-catalog-events"));
        mvc.perform(get("/api/admin/media/AVATAR:"+media).with(jwt().jwt(j->j.subject(ADMIN))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AVAILABLE"))
            .andExpect(jsonPath("$.resourceId").isEmpty());
    }
    @Test void concurrent_profile_binding_commits_before_authoritative_retirement_check() throws Exception {
        UUID owner=UUID.randomUUID(),media=UUID.randomUUID();seed(owner,media,null);
        var pool=java.util.concurrent.Executors.newSingleThreadExecutor();
        try(var connection=jdbc.getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try(var lock=connection.prepareStatement("SELECT id FROM app_users WHERE id=? FOR UPDATE")){lock.setObject(1,owner);lock.executeQuery().close();}
            try(var update=connection.prepareStatement("UPDATE app_users SET avatar_url=? WHERE id=?")){update.setString(1,"media:avatar:avatars/"+media);update.setObject(2,owner);update.executeUpdate();}
            var pending=pool.submit(()->change(media,"RETIRED","Unused",UUID.randomUUID()));
            org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.TimeoutException.class,()->pending.get(150,java.util.concurrent.TimeUnit.MILLISECONDS));
            connection.commit();pending.get(10,java.util.concurrent.TimeUnit.SECONDS).andExpect(status().isUnprocessableEntity());
            assertThat(jdbc.queryForObject("SELECT status FROM user_avatar_media WHERE media_id=?",String.class,media)).isEqualTo("AVAILABLE");
        } finally {pool.shutdownNow();}
    }
    @Test void additive_migration_is_repeatable_and_keeps_retired_data() throws Exception {
        String schema="avatar_lifecycle_"+UUID.randomUUID().toString().replace("-","");
        try(var c=jdbc.getDataSource().getConnection();var statement=c.createStatement()) {
            statement.execute("CREATE SCHEMA "+schema);
            try {
                statement.execute("SET search_path TO "+schema);
                statement.execute("CREATE TABLE user_avatar_media (LIKE public.user_avatar_media INCLUDING ALL)");
                String fragment=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/release/2026-10-04-avatar-media-lifecycle.sql"));
                statement.execute(fragment);statement.execute(fragment);
                statement.execute("INSERT INTO user_avatar_media(media_id,user_id,declared_content_type,declared_size,pending_object_key,status,object_key,content_type,size_bytes,width,height,sha256,created_at,updated_at,version) VALUES('"+UUID.randomUUID()+"','"+UUID.randomUUID()+"','image/jpeg',1024,'pending','RETIRED','avatar','image/jpeg',1024,512,512,'hash',now(),now(),1)");
                statement.execute(fragment);
                try(var rows=statement.executeQuery("SELECT status FROM user_avatar_media")){assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("RETIRED");}
            } finally {statement.execute("SET search_path TO public");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
        }
    }
    org.springframework.test.web.servlet.ResultActions change(UUID media,String state,String reason,UUID command)throws Exception {
        String body=json.writeValueAsString(java.util.Map.of("commandId",command,"status",state,"reason",reason));
        return mvc.perform(post("/api/admin/studio/avatar-media/"+media+"/lifecycle").with(jwt().jwt(j->j.subject(ADMIN))).contentType("application/json").content(body));
    }
    void seed(UUID owner,UUID media,String reference){
        jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email,email_verified,display_name,last_login_at) VALUES(?,'GOOGLE',?,'avatar-lifecycle@test',true,'Admin',now()) ON CONFLICT DO NOTHING",UUID.fromString(ADMIN),ADMIN);
        jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email,email_verified,display_name,last_login_at) VALUES(?,'GOOGLE',?,'avatar-owner@test',true,'Owner',now())",owner,owner.toString());
        jdbc.update("INSERT INTO app_users(id,auth_user_id,display_name,avatar_url,created_at,updated_at,version,lifecycle_status) VALUES(?,?,'Avatar owner',?,now(),now(),1,'ACTIVE')",owner,owner,reference);
        jdbc.update("INSERT INTO user_avatar_media(media_id,user_id,declared_content_type,declared_size,pending_object_key,status,object_key,content_type,size_bytes,width,height,sha256,created_at,updated_at,version) VALUES(?,?,'image/jpeg',1024,?,'AVAILABLE',?,'image/jpeg',1024,512,512,'hash',now(),now(),1)",media,owner,"pending/"+media,"avatars/"+media);
    }
}
