package com.nm.fragmentsclean.experienceContextTest.endtoend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class StudioCommunityIT extends AbstractExperienceE2E {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    private final String admin="99999999-9999-9999-9999-999999999999";
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository outbox;
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting router;
    @Test void direct_moderation_projects_visibility_and_audit_and_is_replay_safe() throws Exception {
        jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);
        UUID user=UUID.randomUUID(),coffee=UUID.randomUUID(),id=UUID.randomUUID();
        jdbc.update("INSERT INTO experience_coffee_references VALUES(?,?,?,?)",coffee,true,Timestamp.from(Instant.now()),1);
        mvc.perform(post("/api/experiences").with(jwt().jwt(j->j.subject(user.toString()))).contentType("application/json").content("""
            {"commandId":"%s","experienceId":"%s","coffeeId":"%s","message":"Modération directe","publicationStatus":"PUBLISHED","at":"2026-10-04T10:00:00Z"}
            """.formatted(UUID.randomUUID(),id,coffee))).andExpect(status().isAccepted());
        project(id);
        UUID command=UUID.randomUUID(),action=UUID.randomUUID();
        String body="""
            {"commandId":"%s","actionId":"%s","decision":"HIDDEN","reason":"Spam confirmé","at":"2026-10-04T10:00:00Z"}
            """.formatted(command,action);
        String path="/api/admin/experiences/"+id+"/moderation";
        mvc.perform(post(path).with(jwt().jwt(j->j.subject(user.toString()))).contentType("application/json").content(body)).andExpect(status().isForbidden());
        for(int attempt=0;attempt<2;attempt++)mvc.perform(post(path).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body)).andExpect(status().isAccepted());
        mvc.perform(get("/api/admin/commands/"+command).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPLIED"));
        assertThat(com.nm.fragmentsclean.platform.eventing.IntegrationEventTypeCatalog.currentVersion("experience.moderated")).isEqualTo(2);
        project(id);project(id);
        mvc.perform(get("/api/coffees/"+coffee+"/experiences").with(jwt().jwt(j->j.subject(user.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get("/api/admin/experiences/"+id).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.actions.length()").value(1)).andExpect(jsonPath("$.actions[0].operatorId").value(admin)).andExpect(jsonPath("$.actions[0].reason").value("Spam confirmé"));
        assertThat(jdbc.queryForObject("SELECT report_id FROM experience_moderation_actions_projection WHERE action_id=?",UUID.class,action)).isNull();
        mvc.perform(post(path).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body.replace(command.toString(),UUID.randomUUID().toString()).replace(action.toString(),UUID.randomUUID().toString()).replace("HIDDEN","VISIBLE").replace("Spam confirmé","Erreur de classement"))).andExpect(status().isAccepted());
        project(id);
        mvc.perform(get("/api/coffees/"+coffee+"/experiences").with(jwt().jwt(j->j.subject(user.toString())))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(post(path).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body.replace(command.toString(),UUID.randomUUID().toString()).replace(action.toString(),UUID.randomUUID().toString()).replace("Spam confirmé"," "))).andExpect(status().isUnprocessableEntity());
        UUID mediaId=UUID.randomUUID();
        jdbc.update("INSERT INTO experience_media_views(media_id,experience_id,user_id,status,object_key,content_type,size_bytes,width,height,position,updated_at,version) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",mediaId,id,user,"AVAILABLE","experiences/test/photo.jpg","image/jpeg",1024,640,480,0,Timestamp.from(Instant.now()),1);
        mvc.perform(get("/api/admin/experience-media/"+mediaId).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(user.toString())).andExpect(jsonPath("$.experienceId").value(id.toString())).andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.startsWith("https://download.test/"))).andExpect(jsonPath("$.objectKey").doesNotExist());
        jdbc.update("UPDATE experience_media_views SET status='DELETION_PENDING' WHERE media_id=?",mediaId);
        mvc.perform(get("/api/admin/experience-media/"+mediaId).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.url").isEmpty());
    }
    @Test void media_lifecycle_reads_source_and_never_exposes_storage_keys() throws Exception {
        UUID experience=UUID.randomUUID(), media=UUID.randomUUID(), owner=UUID.randomUUID(), coffee=UUID.randomUUID();
        jdbc.update("INSERT INTO experiences VALUES(?,?,?,?,?,?,now(),now(),null,1)",experience,owner,coffee,"Source lifecycle","PUBLISHED","VISIBLE");
        jdbc.update("INSERT INTO experience_media(media_id,experience_id,coffee_id,user_id,declared_content_type,declared_size,pending_object_key,status,object_key,content_type,size_bytes,width,height,sha256,created_at,updated_at,version) VALUES(?,?,?,?,?,1024,?,'AVAILABLE',?,'image/jpeg',1024,640,480,?,now(),now(),1)",media,experience,coffee,owner,"image/jpeg","pending/test","experiences/test/source.jpg","a".repeat(64));
        String route="/api/admin/studio/experience-media/"+media;
        mvc.perform(get(route)).andExpect(status().isUnauthorized());
        mvc.perform(get(route).with(jwt().jwt(j->j.subject(owner.toString())))).andExpect(status().isForbidden());
        mvc.perform(get(route).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk())
            .andExpect(jsonPath("$.experienceId").value(experience.toString())).andExpect(jsonPath("$.canHidePublication").value(true))
            .andExpect(jsonPath("$.canRestorePublication").value(false)).andExpect(jsonPath("$.objectKey").doesNotExist());
        jdbc.update("UPDATE experiences SET moderation_status='HIDDEN' WHERE experience_id=?",experience);
        mvc.perform(get(route).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk())
            .andExpect(jsonPath("$.canHidePublication").value(false)).andExpect(jsonPath("$.canRestorePublication").value(true));
        jdbc.update("UPDATE experiences SET publication_status='DELETED' WHERE experience_id=?",experience);
        mvc.perform(get(route).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk())
            .andExpect(jsonPath("$.canHidePublication").value(false)).andExpect(jsonPath("$.canRestorePublication").value(false));
        mvc.perform(get("/api/admin/studio/experience-media/"+UUID.randomUUID()).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isNotFound());
    }
    @Test void reviewing_an_already_visible_experience_closes_the_report_projection() throws Exception {
        jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);
        UUID author=UUID.randomUUID(),coffee=UUID.randomUUID(),id=UUID.randomUUID(),report=UUID.randomUUID();
        jdbc.update("INSERT INTO experience_coffee_references VALUES(?,?,?,?)",coffee,true,Timestamp.from(Instant.now()),1);
        mvc.perform(post("/api/experiences").with(jwt().jwt(j->j.subject(author.toString()))).contentType("application/json").content("""
            {"commandId":"%s","experienceId":"%s","coffeeId":"%s","message":"À examiner","publicationStatus":"PUBLISHED","at":"2026-10-04T10:00:00Z"}
            """.formatted(UUID.randomUUID(),id,coffee))).andExpect(status().isAccepted());
        mvc.perform(post("/api/experiences/"+id+"/reports").with(jwt().jwt(j->j.subject(UUID.randomUUID().toString()))).contentType("application/json").content("""
            {"commandId":"%s","reportId":"%s","reason":"OTHER","at":"2026-10-04T10:00:00Z"}
            """.formatted(UUID.randomUUID(),report))).andExpect(status().isAccepted());
        project(id);
        mvc.perform(post("/api/admin/experiences/"+id+"/moderation").with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content("""
            {"commandId":"%s","actionId":"%s","decision":"VISIBLE","reason":"Contenu conforme","at":"2026-10-04T10:00:00Z"}
            """.formatted(UUID.randomUUID(),UUID.randomUUID()))).andExpect(status().isAccepted());
        project(id);
        assertThat(jdbc.queryForObject("SELECT status FROM experience_reports_projection WHERE report_id=?",String.class,report)).isEqualTo("DISMISSED");
    }
    private void project(UUID id){outbox.findAll().stream().filter(e->e.getEventType().endsWith("ExperienceSnapshotChangedEvent")||e.getEventType().endsWith("ExperienceModerationDecidedEvent")||e.getEventType().endsWith("ExperienceReportedEvent")).filter(e->e.getPayloadJson().contains(id.toString())).forEach(e->router.route(new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory().from(e,com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.EXPERIENCES_EVENTS)));}
    @Test void user_search_returns_application_profile_without_email() throws Exception {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email,email_verified,last_login_at) VALUES(?,'GOOGLE',?,'private@example.test',true,now())",id,id.toString());
        jdbc.update("INSERT INTO app_users(id,auth_user_id,display_name,created_at) VALUES(?,?,?,now())",id,id,"Studio unique "+id);
        mvc.perform(get("/api/admin/users").param("q",id.toString()).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].userId").value(id.toString())).andExpect(jsonPath("$.items[0].email").doesNotExist());
        mvc.perform(get("/api/admin/users/"+id).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
    }
    @Test void migration_upgrades_the_old_audit_schema_and_can_be_replayed() throws Exception {
        String schema="community_migration_"+UUID.randomUUID().toString().replace("-","");
        try(var connection=jdbc.getDataSource().getConnection();var statement=connection.createStatement()) {
            statement.execute("CREATE SCHEMA "+schema);
            try {
                statement.execute("SET search_path TO "+schema);
                statement.execute("CREATE TABLE experience_views (LIKE public.experience_views INCLUDING ALL)");
                statement.execute("CREATE TABLE experience_moderation_actions_projection (LIKE public.experience_moderation_actions_projection INCLUDING ALL)");
                statement.execute("ALTER TABLE experience_moderation_actions_projection ALTER COLUMN report_id SET NOT NULL");
                String migration=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/release/2026-10-04-studio-community.sql"));
                statement.execute(migration);statement.execute(migration);
                try(var result=statement.executeQuery("SELECT is_nullable FROM information_schema.columns WHERE table_schema='"+schema+"' AND table_name='experience_moderation_actions_projection' AND column_name='report_id'")) {
                    assertThat(result.next()).isTrue();assertThat(result.getString(1)).isEqualTo("YES");
                }
            }finally{statement.execute("SET search_path TO public");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
        }
    }
    @Test void admin_search_is_paginated_and_protects_private_routes() throws Exception {
        for (String route:java.util.List.of("/api/admin/users","/api/admin/experiences","/api/admin/experience-media/"+UUID.randomUUID())) {
            mvc.perform(get(route)).andExpect(status().isUnauthorized());
            mvc.perform(get(route).with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());
        }
        UUID author=UUID.randomUUID(), coffee=UUID.randomUUID(), first=UUID.randomUUID(), second=UUID.randomUUID();
        var now=Timestamp.from(Instant.parse("2026-10-04T10:00:00Z"));
        for(UUID id:java.util.List.of(first,second)) jdbc.update("INSERT INTO experience_views VALUES(?,?,?,?,?,?,?,?,?,?)",id,author,coffee,"Studio pagination", "PUBLISHED","HIDDEN",now,now,null,1);
        var response=mvc.perform(get("/api/admin/experiences").param("authorId",author.toString()).param("q","pagination").param("limit","1").with(jwt().jwt(j->j.subject(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].moderationStatus").value("HIDDEN")).andReturn();
        var page=json.readTree(response.getResponse().getContentAsByteArray());
        var next=mvc.perform(get("/api/admin/experiences").param("authorId",author.toString()).param("q","pagination").param("limit","1").param("cursor",page.path("nextCursor").asText()).with(jwt().jwt(j->j.subject(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andReturn();
        assertThat(json.readTree(next.getResponse().getContentAsByteArray()).path("items").get(0).path("experienceId").asText()).isNotEqualTo(page.path("items").get(0).path("experienceId").asText());
        mvc.perform(get("/api/admin/experiences").param("cursor","invalid").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/users/"+UUID.randomUUID()).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/experiences/"+UUID.randomUUID()).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/experience-media/"+UUID.randomUUID()).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isNotFound());
    }
}
