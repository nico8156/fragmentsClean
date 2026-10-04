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
class CoffeeMediaCatalogIT extends AbstractExperienceE2E {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting router;
    final Instant at=Instant.parse("2026-10-04T10:00:00Z");
    final String admin="99999999-9999-9999-9999-999999999999";
    @Test void replacement_retires_missing_photos_and_blocks_old_unknown_additions() throws Exception {
        UUID coffee=UUID.randomUUID(),old=UUID.randomUUID(),current=UUID.randomUUID(),unknown=UUID.randomUUID();
        added(coffee,old,1);added(coffee,old,1);
        arranged(coffee,List.of(current),3);
        added(coffee,unknown,2);added(coffee,old,1);arranged(coffee,List.of(unknown),2);
        assertThat(photoStatus(old)).isEqualTo("DELETED");assertThat(photoStatus(current)).isEqualTo("AVAILABLE");
        assertThat(photoStatus(unknown)).isNull();
        mvc.perform(get("/api/admin/media").param("q",coffee.toString()).param("origin","COFFEE").with(jwt().jwt(j->j.subject(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].ownerId").isEmpty()).andExpect(jsonPath("$.items[0].createdAt").isEmpty());
    }
    @Test void newer_individual_changes_survive_an_older_complete_inventory_and_parent_deletion_blocks_replay() throws Exception {
        UUID coffee=UUID.randomUUID(),newer=UUID.randomUUID(),older=UUID.randomUUID(),late=UUID.randomUUID();
        added(coffee,newer,8);arranged(coffee,List.of(older),5);
        assertThat(photoStatus(newer)).isEqualTo("AVAILABLE");assertThat(photoStatus(older)).isEqualTo("AVAILABLE");
        route("coffee.deleted",new CoffeeLifecycleIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),coffee,9,at));
        arranged(coffee,List.of(late),6);added(coffee,late,7);
        assertThat(photoStatus(late)).isNull();assertThat(photoStatus(newer)).isEqualTo("DELETED");assertThat(photoStatus(older)).isEqualTo("DELETED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM media_catalog_entries WHERE origin='COFFEE' AND (resource_id=? OR object_key LIKE ?)",Integer.class,coffee,"%"+coffee+"%")).isZero();
    }
    @Test void a_photo_deleted_before_its_addition_stays_deleted() throws Exception {
        UUID coffee=UUID.randomUUID(),photo=UUID.randomUUID();
        route("coffee.photo_deleted",new CoffeePhotoDeletedIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),coffee,photo,4,at,null));
        added(coffee,photo,2);
        assertThat(photoStatus(photo)).isEqualTo("DELETED");
        mvc.perform(get("/api/admin/media/COFFEE:"+photo).with(jwt().jwt(j->j.subject(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.resourceId").isEmpty()).andExpect(jsonPath("$.previewUrl").isEmpty());
    }
    @Test void a_current_source_photo_has_a_preview_and_removal_revokes_new_previews_even_if_catalogue_is_stale() throws Exception {
        UUID coffee=UUID.randomUUID(),photo=UUID.randomUUID();
        jdbc.update("INSERT INTO coffees(id,name,lat,lon,version,updated_at,publication_status) VALUES(?,'Test catalogue',0,0,1,?,'DRAFT')",coffee,java.sql.Timestamp.from(at));
        jdbc.update("INSERT INTO coffee_photos(coffee_id,photo_id,photo_uri,sort_order) VALUES(?,?,?,0)",coffee,photo,"https://images.test/current.jpg");
        UUID otherCoffee=UUID.randomUUID();
        jdbc.update("INSERT INTO coffees(id,name,lat,lon,version,updated_at) VALUES(?,'Other catalogue',0,0,1,?)",otherCoffee,java.sql.Timestamp.from(at));
        jdbc.update("INSERT INTO coffee_photos(coffee_id,photo_id,photo_uri,sort_order) VALUES(?,?,?,0)",otherCoffee,photo,"https://images.test/other-coffee.jpg");
        added(coffee,photo,1);
        mvc.perform(get("/api/admin/media/COFFEE:"+photo).with(jwt().jwt(j->j.subject(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.previewUrl").value("https://images.test/current.jpg"));
        jdbc.update("DELETE FROM coffee_photos WHERE coffee_id=?",coffee);
        mvc.perform(get("/api/admin/media/COFFEE:"+photo).with(jwt().jwt(j->j.subject(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.previewUrl").isEmpty());
    }
    @Autowired com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ReplayCoffeeMediaCatalog replay;
    @Autowired com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository outbox;
    @Test void replay_is_bounded_restartable_and_goes_only_to_the_admin_index() throws Exception {
        var first=UUID.fromString("ffffffff-ffff-ffff-ffff-000000000000");
        var photo=UUID.randomUUID();var coffee=UUID.fromString("ffffffff-ffff-ffff-ffff-000000000001");
        for(int i=1;i<=101;i++)jdbc.update("INSERT INTO coffees(id,name,lat,lon,version,updated_at) VALUES(?,'Replay catalogue',0,0,7,?)",UUID.fromString("ffffffff-ffff-ffff-ffff-"+String.format("%012x",i)),java.sql.Timestamp.from(at));
        jdbc.update("INSERT INTO coffee_photos(coffee_id,photo_id,photo_uri,sort_order) VALUES(?,?,?,0)",coffee,photo,"https://images.test/replay.jpg");
        jdbc.update("UPDATE coffee_media_catalog_scan SET cursor_id=?,next_scan_at=now(),completed_at=NULL WHERE id=1",first);
        assertThat(replay.nextBatch()).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT cursor_id FROM coffee_media_catalog_scan WHERE id=1",UUID.class)).isEqualTo(UUID.fromString("ffffffff-ffff-ffff-ffff-000000000064"));
        assertThat(replay.nextBatch()).isEqualTo(1);assertThat(replay.nextBatch()).isZero();
        var factory=new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory();
        var snapshots=outbox.findAll().stream().filter(e->e.getEventType().endsWith("CoffeeMediaCatalogSnapshotEvent") && e.getPayloadJson().contains(coffee.toString())).toList();
        assertThat(snapshots).hasSize(1);
        var snapshot=snapshots.getFirst();
        assertThat(new com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinationResolver().destinationsFor(snapshot)).containsExactly("media-catalog-events");
        router.route(factory.from(snapshot,"media-catalog-events"));
        mvc.perform(get("/api/admin/media/COFFEE:"+photo).with(jwt().jwt(j->j.subject(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.resourceId").value(coffee.toString()))
            .andExpect(jsonPath("$.createdAt").isEmpty()).andExpect(jsonPath("$.updatedAt").value(at.toString()));
        route("coffee.deleted",new CoffeeLifecycleIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),coffee,8,at));
        router.route(factory.from(snapshot,"media-catalog-events"));
        // A new event id exercises version safety beyond inbox deduplication.
        arranged(coffee,List.of(photo),7);
        assertThat(photoStatus(photo)).isEqualTo("DELETED");
    }
    @Test void additive_coffee_migration_is_replayable() throws Exception {
        String schema="coffee_catalog_migration_"+UUID.randomUUID().toString().replace("-","");
        try(var connection=jdbc.getDataSource().getConnection();var statement=connection.createStatement()){
            statement.execute("CREATE SCHEMA "+schema);
            try {
                statement.execute("SET search_path TO "+schema);
                statement.execute("CREATE TABLE media_catalog_entries (LIKE public.media_catalog_entries INCLUDING ALL)");
                String sql=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/release/2026-10-04-coffee-media-catalogue.sql"));
                statement.execute(sql);statement.execute(sql);
                try(var rows=statement.executeQuery("SELECT count(*) FROM coffee_media_catalog_scan")){assertThat(rows.next()).isTrue();assertThat(rows.getInt(1)).isEqualTo(1);}
            } finally {statement.execute("SET search_path TO public");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
        }
    }
    private String photoStatus(UUID id){return jdbc.query("SELECT status FROM media_catalog_entries WHERE origin='COFFEE' AND media_id=?",(r,n)->r.getString(1),id).stream().findFirst().orElse(null);}
    private void added(UUID coffee,UUID photo,int version)throws Exception{route("coffee.photo_added",new CoffeePhotoAddedIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),coffee,photo,"https://images.test/"+coffee+"/"+photo,version,at,null));}
    private void arranged(UUID coffee,List<UUID> photos,int version)throws Exception{route("coffee.photos_arranged",new CoffeePhotosArrangedIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),coffee,photos.stream().map(p->new CoffeePhotosArrangedIntegrationEvent.Photo(p,"https://images.test/"+coffee+"/"+p,false,0)).toList(),version,at,null));}
    private void route(String type,Object event)throws Exception{router.route(new IntegrationEventEnvelope(json.valueToTree(event).path("eventId").asText(),type,1,event.getClass().getName(),"Coffee",UUID.randomUUID().toString(),"coffee-media-test","media-catalog-events",json.writeValueAsString(event),at));}
}
