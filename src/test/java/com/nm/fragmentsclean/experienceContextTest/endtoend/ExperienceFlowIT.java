package com.nm.fragmentsclean.experienceContextTest.endtoend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nm.fragmentsclean.experienceContext.read.ListCoffeeExperiencesQuery;
import com.nm.fragmentsclean.experienceContext.read.ListCoffeeExperiencesQueryHandler;
import com.nm.fragmentsclean.experienceContext.read.ListExperienceModerationReportsQueryHandler;
import com.nm.fragmentsclean.experienceContext.read.ListMyExperiencesQuery;
import com.nm.fragmentsclean.experienceContext.read.ListMyExperiencesQueryHandler;
import com.nm.fragmentsclean.experienceContext.read.adapters.secondary.repositories.JdbcExperienceProjectionRepository;
import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.SpringExperienceReportRepository;
import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.SpringExperienceMediaRepository;
import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.SpringExperienceRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceModerationStatus;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperiencePublicationStatus;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceReportStatus;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations;
import com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory;
import com.nm.fragmentsclean.platform.eventing.contracts.ExperienceIntegrationEvents;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class ExperienceFlowIT extends AbstractExperienceE2E{
    @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired SpringExperienceRepository experiences;@Autowired SpringExperienceReportRepository reports;@Autowired SpringExperienceMediaRepository media;@Autowired SpringOutboxEventRepository outbox;@Autowired SqsIntegrationEventRouting router;@Autowired DateTimeProvider clock;@Autowired ListCoffeeExperiencesQueryHandler coffeeQuery;@Autowired ListMyExperiencesQueryHandler myQuery;@Autowired ListExperienceModerationReportsQueryHandler moderationQuery;@Autowired JdbcExperienceProjectionRepository projections;
    private final UUID user=UUID.randomUUID(),coffee=UUID.randomUUID(),operator=UUID.fromString("99999999-9999-9999-9999-999999999999");
    @BeforeEach void clean(){jdbc.update("DELETE FROM inbox_messages");jdbc.update("DELETE FROM experience_moderation_actions_projection");jdbc.update("DELETE FROM experience_reports_projection");jdbc.update("DELETE FROM experience_user_blocks");jdbc.update("DELETE FROM experience_media_views");jdbc.update("DELETE FROM experience_views");media.deleteAll();reports.deleteAll();experiences.deleteAll();outbox.deleteAll();jdbc.update("DELETE FROM experience_coffee_references");jdbc.update("INSERT INTO experience_coffee_references VALUES(?,?,?,?)",coffee,true,Timestamp.from(now()),1);jdbc.update("DELETE FROM experience_user_profiles");jdbc.update("INSERT INTO experience_user_profiles VALUES(?,?,?,?,?)",user,"Nicolas",null,Timestamp.from(now()),1);((DeterministicDateTimeProvider)clock).instantOfNow=now();}
    @Test void publishes_without_ticket_then_projects_lists_and_pass_contribution()throws Exception{UUID id=UUID.randomUUID();mvc.perform(post("/api/experiences").with(as(user)).contentType("application/json").content("""
        {"commandId":"%s","experienceId":"%s","coffeeId":"%s","message":"  Une très belle visite  ","publicationStatus":"PUBLISHED","at":"2026-09-11T09:59:00Z"}
        """.formatted(UUID.randomUUID(),id,coffee))).andExpect(status().isAccepted());
        assertThat(experiences.findById(id).orElseThrow().getMessage()).isEqualTo("Une très belle visite");assertThat(jdbc.queryForObject("SELECT count(*) FROM tickets WHERE user_id=?",Long.class,user)).isZero();
        var events=outbox.findAll();assertThat(events).hasSize(2);events.stream().filter(e->e.getEventType().endsWith("ExperienceSnapshotChangedEvent")).findFirst().ifPresent(e->router.route(new IntegrationEventEnvelopeFactory().from(e,IntegrationEventDestinations.EXPERIENCES_EVENTS)));events.stream().filter(e->e.getEventType().endsWith("ExperienceLifecycleChangedEvent")).findFirst().ifPresent(e->router.route(new IntegrationEventEnvelopeFactory().from(e,IntegrationEventDestinations.APP_USERS_EVENTS)));
        mvc.perform(get("/api/coffees/{coffeeId}/experiences",coffee).with(as(user))).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].experienceId").value(id.toString()));
        assertThat(coffeeQuery.handle(new ListCoffeeExperiencesQuery(user,coffee,null,20)).items()).singleElement().satisfies(view->{assertThat(view.experienceId()).isEqualTo(id);assertThat(view.authorName()).isEqualTo("Nicolas");});assertThat(myQuery.handle(new ListMyExperiencesQuery(user,null,20)).items()).hasSize(1);assertThat(jdbc.queryForObject("SELECT published_experiences FROM user_pass_projection WHERE user_id=?",Integer.class,user)).isEqualTo(1);
    }
    @Test void draft_update_publish_and_delete_are_owned_durable_commands()throws Exception{
        UUID id=UUID.randomUUID();
        mvc.perform(post("/api/experiences").with(as(user)).contentType("application/json").content("""
                {"commandId":"%s","experienceId":"%s","coffeeId":"%s","message":null,"publicationStatus":"DRAFT","at":"2026-09-11T10:00:00Z"}
                """.formatted(UUID.randomUUID(),id,coffee))).andExpect(status().isAccepted());
        assertThat(experiences.findById(id).orElseThrow().getPublicationStatus()).isEqualTo(ExperiencePublicationStatus.DRAFT);

        mvc.perform(patch("/api/experiences/{id}",id).with(as(user)).contentType("application/json").content("""
                {"commandId":"%s","message":"Texte final","at":"2026-09-11T10:01:00Z"}
                """.formatted(UUID.randomUUID()))).andExpect(status().isAccepted());
        mvc.perform(post("/api/experiences/{id}/publish",id).with(as(user)).contentType("application/json").content("""
                {"commandId":"%s","at":"2026-09-11T10:02:00Z"}
                """.formatted(UUID.randomUUID()))).andExpect(status().isAccepted());
        assertThat(experiences.findById(id).orElseThrow().getPublicationStatus()).isEqualTo(ExperiencePublicationStatus.PUBLISHED);

        UUID rejectedCommand=UUID.randomUUID();
        mvc.perform(patch("/api/experiences/{id}",id).with(as(UUID.randomUUID())).contentType("application/json").content("""
                {"commandId":"%s","message":"Intrusion","at":"2026-09-11T10:03:00Z"}
                """.formatted(rejectedCommand))).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.reason").value("EXPERIENCE_FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT status FROM command_status WHERE command_id=?",String.class,rejectedCommand)).isEqualTo("REJECTED");

        mvc.perform(delete("/api/experiences/{id}",id).with(as(user)).contentType("application/json").content("""
                {"commandId":"%s","at":"2026-09-11T10:04:00Z"}
                """.formatted(UUID.randomUUID()))).andExpect(status().isAccepted());
        assertThat(experiences.findById(id).orElseThrow().getPublicationStatus()).isEqualTo(ExperiencePublicationStatus.DELETED);
    }
    @Test void report_hides_for_reporter_and_admin_decision_hides_globally()throws Exception{UUID author=UUID.randomUUID(),id=UUID.randomUUID(),reporter=UUID.randomUUID(),reportId=UUID.randomUUID();jdbc.update("INSERT INTO experience_user_profiles VALUES(?,?,?,?,?)",author,"Auteur",null,Timestamp.from(now()),1);seedView(id,author,"Texte public",0);experiences.save(new com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities.ExperienceJpaEntity(id,author,coffee,"Texte public",ExperiencePublicationStatus.PUBLISHED,ExperienceModerationStatus.VISIBLE,now(),now(),null,0));
        mvc.perform(post("/api/experiences/{id}/reports", id).with(as(reporter)).contentType("application/json").content("""
                {"commandId":"%s","reportId":"%s","reason":"SPAM","at":"2026-09-11T10:00:00Z"}
                """.formatted(UUID.randomUUID(), reportId))).andExpect(status().isAccepted());
        var reportEvent = outbox.findAll().stream().filter(e -> e.getEventType().endsWith("ExperienceReportedEvent")).findFirst().orElseThrow();
        router.route(new IntegrationEventEnvelopeFactory().from(reportEvent, IntegrationEventDestinations.EXPERIENCES_EVENTS));
        assertThat(coffeeQuery.handle(new ListCoffeeExperiencesQuery(reporter, coffee, null, 20)).items()).isEmpty();
        assertThat(moderationQuery.handle("OPEN", 50)).singleElement().satisfies(v -> assertThat(v.content()).isEqualTo("Texte public"));

        mvc.perform(post("/api/admin/experience-moderation/reports/{reportId}/decision", reportId).param("experienceId", id.toString()).with(as(operator)).contentType("application/json").content("""
                {"commandId":"%s","actionId":"%s","decision":"HIDDEN","reason":"spam confirmé","at":"2026-09-11T10:01:00Z"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()))).andExpect(status().isAccepted());
        var moderated = outbox.findAll().stream().filter(e -> e.getEventType().endsWith("ExperienceModerationDecidedEvent")).findFirst().orElseThrow();
        router.route(new IntegrationEventEnvelopeFactory().from(moderated, IntegrationEventDestinations.EXPERIENCES_EVENTS));
        assertThat(coffeeQuery.handle(new ListCoffeeExperiencesQuery(user, coffee, null, 20)).items()).isEmpty();
        assertThat(reports.findById(reportId).orElseThrow().getStatus()).isEqualTo(ExperienceReportStatus.RESOLVED);
    }
    @Test void projections_are_replay_safe_and_blocked_authors_are_filtered(){UUID author=UUID.randomUUID(),id=UUID.randomUUID();seedView(id,author,"Visite",1);projections.upsertBlock(UUID.randomUUID(),user,author,true,1,now());assertThat(coffeeQuery.handle(new ListCoffeeExperiencesQuery(user,coffee,null,20)).items()).isEmpty();}
    @Test void invalid_read_cursor_is_rejected_at_the_http_boundary()throws Exception{mvc.perform(get("/api/coffees/{coffeeId}/experiences",coffee).param("cursor","invalid").with(as(user))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_QUERY"));}
    @Test void uploads_normalizes_confirms_and_projects_private_experience_media()throws Exception{UUID id=UUID.randomUUID(),mediaId=UUID.randomUUID(),confirm=UUID.randomUUID();mvc.perform(post("/api/experiences").with(as(user)).contentType("application/json").content("""
        {"commandId":"%s","experienceId":"%s","coffeeId":"%s","message":"Avec photo","publicationStatus":"DRAFT","at":"2026-09-11T10:00:00Z"}
        """.formatted(UUID.randomUUID(),id,coffee))).andExpect(status().isAccepted());mvc.perform(post("/api/experiences/{id}/media/upload-intents",id).with(as(user)).contentType("application/json").content("""
        {"mediaId":"%s","contentType":"image/png","size":2048}
        """.formatted(mediaId))).andExpect(status().isCreated()).andExpect(jsonPath("$.uploadRequired").value(true)).andExpect(jsonPath("$.headers['Content-Type']").value("image/png"));mvc.perform(post("/api/experiences/{id}/media/{mediaId}/confirm",id,mediaId).with(as(user)).contentType("application/json").content("""
        {"commandId":"%s","at":"2026-09-11T10:01:00Z"}
        """.formatted(confirm))).andExpect(status().isAccepted());assertThat(jdbc.queryForObject("SELECT status FROM command_status WHERE command_id=?",String.class,confirm)).isEqualTo("APPLIED");var created=outbox.findAll().stream().filter(e->e.getEventType().endsWith("ExperienceSnapshotChangedEvent")).findFirst().orElseThrow();router.route(new IntegrationEventEnvelopeFactory().from(created,IntegrationEventDestinations.EXPERIENCES_EVENTS));var event=outbox.findAll().stream().filter(e->e.getEventType().endsWith("ExperienceMediaChangedEvent")).findFirst().orElseThrow();router.route(new IntegrationEventEnvelopeFactory().from(event,IntegrationEventDestinations.EXPERIENCES_EVENTS));assertThat(myQuery.handle(new ListMyExperiencesQuery(user,null,20)).items()).singleElement().satisfies(view->{assertThat(view.media()).singleElement().satisfies(photo->assertThat(photo.url()).startsWith("https://download.test/"));});}
    @Test void an_older_moderation_event_cannot_overwrite_a_newer_decision(){
        UUID author=UUID.randomUUID(),id=UUID.randomUUID(),reportId=UUID.randomUUID();
        seedView(id,author,"Visite",3);
        projections.apply(new ExperienceIntegrationEvents.Reported(UUID.randomUUID(),UUID.randomUUID(),reportId,id,coffee,author,user,"SPAM",null,"OPEN",0,now(),now()));
        projections.apply(new ExperienceIntegrationEvents.Moderated(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),reportId,id,coffee,author,operator,"VISIBLE","DISMISSED","restauration",5,now().plusSeconds(2),now()));
        projections.apply(new ExperienceIntegrationEvents.Moderated(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),reportId,id,coffee,author,operator,"HIDDEN","RESOLVED","ancien événement",4,now().plusSeconds(1),now()));

        assertThat(jdbc.queryForObject("SELECT moderation_status FROM experience_views WHERE experience_id=?",String.class,id)).isEqualTo("VISIBLE");
        assertThat(jdbc.queryForObject("SELECT status FROM experience_reports_projection WHERE report_id=?",String.class,reportId)).isEqualTo("DISMISSED");
    }
    private void seedView(UUID id,UUID author,String message,long version){jdbc.update("INSERT INTO experience_views VALUES(?,?,?,?,?,?,?,?,?,?)",id,author,coffee,message,"PUBLISHED","VISIBLE",Timestamp.from(now()),Timestamp.from(now()),null,version);}
    private static org.springframework.test.web.servlet.request.RequestPostProcessor as(UUID id){return jwt().jwt(j->j.subject(id.toString()).claim("roles",List.of("USER","ADMIN")));}private Instant now(){return Instant.parse("2026-09-11T10:00:00Z");}
}
