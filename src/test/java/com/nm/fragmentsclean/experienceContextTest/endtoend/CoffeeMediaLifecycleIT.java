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
class CoffeeMediaLifecycleIT extends AbstractExperienceE2E {
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 final String admin="99999999-9999-9999-9999-999999999999";
 UUID[] seed(){UUID coffee=UUID.randomUUID(),photo=UUID.randomUUID();
  jdbc.update("INSERT INTO coffees(id,name,lat,lon,version,updated_at,publication_status) VALUES(?,'Lifecycle Coffee',0,0,1,now(),'PUBLISHED')",coffee);
  jdbc.update("INSERT INTO coffee_photos(coffee_id,photo_id,photo_uri,is_cover,sort_order) VALUES(?,?,?,true,0)",coffee,photo,"https://images.test/"+photo+".jpg");return new UUID[]{coffee,photo};}
 @Test void legacy_delete_cannot_bypass_source_retirement_or_admin_security() throws Exception {
  var ids=seed();String route="/api/admin/coffees/"+ids[0]+"/photos/"+ids[1];
  mvc.perform(delete(route)).andExpect(status().isUnauthorized());
  mvc.perform(delete(route).with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());
  mvc.perform(delete(route).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isGone());
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos WHERE coffee_id=? AND photo_id=?",Integer.class,ids[0],ids[1])).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photo_retirements WHERE photo_id=?",Integer.class,ids[1])).isZero();
 }
 @Test void reads_current_source_association_and_caps_without_exposing_private_uri() throws Exception {
  var ids=seed();String route="/api/admin/studio/coffee-media/"+ids[1];
  mvc.perform(get(route).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk())
   .andExpect(jsonPath("$.coffeeId").value(ids[0].toString())).andExpect(jsonPath("$.canRetire").value(true))
   .andExpect(jsonPath("$.photoUri").doesNotExist());
 }
 @Test void retirement_is_reversible_and_audited_without_a_storage_delete() throws Exception {
  jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);
  var ids=seed();UUID command=UUID.randomUUID();String route="/api/admin/studio/coffee-media/"+ids[1]+"/lifecycle";
  String body="""
   {"commandId":"%s","coffeeId":"%s","status":"RETIRED","reason":"Image obsolète"}
   """.formatted(command,ids[0]);
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body)).andExpect(status().isAccepted());
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body)).andExpect(status().isAccepted());
  mvc.perform(get("/api/admin/studio/coffee-media/"+ids[1]+"/operations").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk())
   .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].actorUserId").value(admin)).andExpect(jsonPath("$[0].reason").value("Image obsolète"));
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos WHERE coffee_id=? AND photo_id=?",Integer.class,ids[0],ids[1])).isZero();
  mvc.perform(get("/api/admin/studio/coffee-media/"+ids[1]).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk())
   .andExpect(jsonPath("$.status").value("RETIRED")).andExpect(jsonPath("$.canRestore").value(true));
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body.replace(command.toString(),UUID.randomUUID().toString()).replace("RETIRED","AVAILABLE"))).andExpect(status().isAccepted());
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos WHERE coffee_id=? AND photo_id=?",Integer.class,ids[0],ids[1])).isEqualTo(1);
 }
 @Test void restoration_rejects_a_photo_identity_now_used_by_another_coffee() throws Exception {
  jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);
  var ids=seed();UUID command=UUID.randomUUID();String route="/api/admin/studio/coffee-media/"+ids[1]+"/lifecycle";
  String body="""
   {"commandId":"%s","coffeeId":"%s","status":"RETIRED","reason":"Image obsolète"}
   """.formatted(command,ids[0]);
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body)).andExpect(status().isAccepted());
  UUID other=seed()[0];
  jdbc.update("INSERT INTO coffee_photos(coffee_id,photo_id,photo_uri,is_cover,sort_order) VALUES(?,?,?,false,1)",other,ids[1],"https://images.test/legacy.jpg");
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body.replace(command.toString(),UUID.randomUUID().toString()).replace("RETIRED","AVAILABLE"))).andExpect(status().isUnprocessableEntity());
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos WHERE coffee_id=? AND photo_id=?",Integer.class,ids[0],ids[1])).isZero();
 }

 @Test void source_lock_precedes_loading_and_observes_an_archive_committed_while_waiting() throws Exception {
  var ids=seed();String body="""
   {"commandId":"%s","coffeeId":"%s","status":"RETIRED","reason":"Review"}
   """.formatted(UUID.randomUUID(),ids[0]);
  try(var executor=java.util.concurrent.Executors.newSingleThreadExecutor();var connection=jdbc.getDataSource().getConnection()){
   connection.setAutoCommit(false);
   try(var statement=connection.prepareStatement("SELECT id FROM coffees WHERE id=? FOR UPDATE")){statement.setObject(1,ids[0]);statement.executeQuery().close();}
   var started=new java.util.concurrent.CountDownLatch(1);
   var future=executor.submit(()->{started.countDown();return mvc.perform(post("/api/admin/studio/coffee-media/"+ids[1]+"/lifecycle").with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body)).andReturn();});
   assertThat(started.await(2,java.util.concurrent.TimeUnit.SECONDS)).isTrue();Thread.sleep(200);assertThat(future.isDone()).isFalse();
   try(var statement=connection.prepareStatement("UPDATE coffees SET publication_status='ARCHIVED',archived_at=now() WHERE id=?")){statement.setObject(1,ids[0]);statement.executeUpdate();}
   connection.commit();assertThat(future.get(5,java.util.concurrent.TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(422);
  }
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos WHERE coffee_id=?",Integer.class,ids[0])).isEqualTo(1);
 }

 @Autowired com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository outbox;
 @Autowired com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting router;
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;
 private void project(UUID id,String suffix){var factory=new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory();outbox.findAll().stream().filter(e->e.getEventType().endsWith(suffix) && e.getPayloadJson().contains(id.toString())).forEach(e->{router.route(factory.from(e,"coffees-events"));router.route(factory.from(e,"media-catalog-events"));});}
 private void routeOldAdded(UUID coffee,UUID photo)throws Exception {
  var event=new com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotoAddedIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),coffee,photo,"https://images.test/old.jpg",1,java.time.Instant.now(),null);
  router.route(new com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope(event.eventId().toString(),"coffee.photo_added",1,event.getClass().getName(),"Coffee",coffee.toString(),"coffee-lifecycle-test","coffees-events",json.writeValueAsString(event),java.time.Instant.now()));
 }
 @Test void a_late_public_photo_addition_cannot_reintroduce_retired_content() throws Exception {
  jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);
  var ids=seed();jdbc.update("INSERT INTO coffee_photos_projection(id,coffee_id,photo_uri,is_cover,sort_order) VALUES(?,?,?,true,0)",ids[1],ids[0],"https://images.test/current.jpg");
  String body="""
   {"commandId":"%s","coffeeId":"%s","status":"RETIRED","reason":"Review"}
   """.formatted(UUID.randomUUID(),ids[0]);
  mvc.perform(post("/api/admin/studio/coffee-media/"+ids[1]+"/lifecycle").with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body)).andExpect(status().isAccepted());
  project(ids[0],"CoffeePhotoDeletedEvent");
  routeOldAdded(ids[0],ids[1]);
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos_projection WHERE id=?",Integer.class,ids[1])).isZero();
 }
 @Test void a_late_public_photo_deletion_cannot_hide_a_restored_source_photo() throws Exception {
  jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);
  var ids=seed();UUID commandId=UUID.randomUUID();String route="/api/admin/studio/coffee-media/"+ids[1]+"/lifecycle";
  String body="""
   {"commandId":"%s","coffeeId":"%s","status":"RETIRED","reason":"Review"}
   """.formatted(commandId,ids[0]);
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body)).andExpect(status().isAccepted());
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body.replace("RETIRED","AVAILABLE").replace(commandId.toString(),UUID.randomUUID().toString()))).andExpect(status().isAccepted());
  project(ids[0],"CoffeePhotoAddedEvent");
  var old=new com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotoDeletedIntegrationEvent(UUID.randomUUID(),UUID.randomUUID(),ids[0],ids[1],1,java.time.Instant.now(),null);
  router.route(new com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope(old.eventId().toString(),"coffee.photo_deleted",1,old.getClass().getName(),"Coffee",ids[0].toString(),"coffee-lifecycle-test","coffees-events",json.writeValueAsString(old),java.time.Instant.now()));
  assertThat(jdbc.queryForObject("SELECT count(*) FROM coffee_photos_projection WHERE id=?",Integer.class,ids[1])).isEqualTo(1);
 }

 @Test void admin_rights_validation_and_missing_resources_are_enforced() throws Exception {
  var ids=seed();String route="/api/admin/studio/coffee-media/"+ids[1];
  mvc.perform(get(route)).andExpect(status().isUnauthorized());
  mvc.perform(get(route).with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/studio/coffee-media/"+UUID.randomUUID()).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isNotFound());
  String body="""
   {"commandId":"%s","coffeeId":"%s","status":"RETIRED","reason":" "}
   """.formatted(UUID.randomUUID(),ids[0]);
  mvc.perform(post(route+"/lifecycle").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
  mvc.perform(post(route+"/lifecycle").with(jwt().jwt(j->j.subject(UUID.randomUUID().toString()))).contentType("application/json").content(body)).andExpect(status().isForbidden());
  mvc.perform(post(route+"/lifecycle").with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body)).andExpect(status().isUnprocessableEntity());
  mvc.perform(post(route+"/lifecycle").with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content("{}" )).andExpect(status().isBadRequest());
  jdbc.update("UPDATE coffees SET publication_status='ARCHIVED',archived_at=now() WHERE id=?",ids[0]);
  mvc.perform(get(route).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.canRetire").value(false)).andExpect(jsonPath("$.canRestore").value(false));
 }
 @Test void additive_migration_is_replayable_and_preserves_retirement_metadata() throws Exception {
  String schema="coffee_lifecycle_"+UUID.randomUUID().toString().replace("-","");
  try(var connection=jdbc.getDataSource().getConnection();var statement=connection.createStatement()){
   statement.execute("CREATE SCHEMA "+schema);
   try{
    statement.execute("SET search_path TO "+schema);
    statement.execute("CREATE TABLE coffees (LIKE public.coffees INCLUDING ALL)");
    String sql=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/release/2026-10-04-coffee-media-lifecycle.sql"));
    statement.execute(sql);
    UUID coffee=UUID.randomUUID(),photo=UUID.randomUUID();
    statement.execute("INSERT INTO coffees(id,name,lat,lon,version,updated_at) VALUES('"+coffee+"','Retained',0,0,1,now())");
    statement.execute("INSERT INTO coffee_photo_retirements(photo_id,coffee_id,photo_uri,was_cover,sort_order,retired_at) VALUES('"+photo+"','"+coffee+"','s3://bucket/evidence.jpg',true,0,'2026-10-04T10:00:00Z')");
    statement.execute(sql);
    try(var rows=statement.executeQuery("SELECT photo_uri,retired_at FROM coffee_photo_retirements")){assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("s3://bucket/evidence.jpg");assertThat(rows.getTimestamp(2).toInstant()).isEqualTo(java.time.Instant.parse("2026-10-04T10:00:00Z"));}
   }finally{statement.execute("SET search_path TO public");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
  }
 }

 @Test void purge_rejects_early_shared_foreign_and_unmanaged_resources()throws Exception{
  jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);
  var ids=seed();String ref="/api/coffees/photo-assets/"+ids[1]+".png";jdbc.update("UPDATE coffee_photos SET photo_uri=? WHERE photo_id=?",ref,ids[1]);String route="/api/admin/studio/coffee-media/"+ids[1]+"/lifecycle";
  var retire="{\"commandId\":\"%s\",\"coffeeId\":\"%s\",\"status\":\"RETIRED\",\"reason\":\"Review\"}";
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(retire.formatted(UUID.randomUUID(),ids[0]))).andExpect(status().isAccepted());
  var purge=retire.replace("RETIRED","PURGE_REQUESTED");
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(purge.formatted(UUID.randomUUID(),ids[0]))).andExpect(status().isUnprocessableEntity());
  jdbc.update("UPDATE coffee_photo_retirements SET retired_at='2023-08-01' WHERE photo_id=?",ids[1]);var other=seed();jdbc.update("UPDATE coffee_photos SET photo_uri=? WHERE photo_id=?",ref,other[1]);
  mvc.perform(get("/api/admin/studio/coffee-media/"+ids[1]).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.canPurge").value(false));
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(purge.formatted(UUID.randomUUID(),ids[0]))).andExpect(status().isUnprocessableEntity());
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(purge.formatted(UUID.randomUUID(),other[0]))).andExpect(status().isUnprocessableEntity());
  jdbc.update("DELETE FROM coffee_photos WHERE coffee_id=?",other[0]);jdbc.update("UPDATE coffee_photo_retirements SET photo_uri='https://external.test/photo.png' WHERE photo_id=?",ids[1]);
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(purge.formatted(UUID.randomUUID(),ids[0]))).andExpect(status().isUnprocessableEntity());
  assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM coffee_photo_retirements WHERE photo_id=?",String.class,ids[1])).isEqualTo("RETIRED");
 }
 @Test void purge_upgrade_preserves_existing_retirement_and_is_replayable()throws Exception{
  String schema="coffee_purge_"+UUID.randomUUID().toString().replace("-","");
  try(var connection=jdbc.getDataSource().getConnection();var statement=connection.createStatement()){
   statement.execute("CREATE SCHEMA "+schema);try{statement.execute("SET search_path TO "+schema);statement.execute("CREATE TABLE coffee_photo_retirements(photo_id UUID PRIMARY KEY,retired_at TIMESTAMPTZ)");statement.execute("CREATE TABLE media_catalog_entries(id UUID PRIMARY KEY)");UUID id=UUID.randomUUID();statement.execute("INSERT INTO coffee_photo_retirements VALUES('"+id+"','2026-09-01T00:00:00Z')");String sql=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/release/2026-10-04-coffee-media-purge.sql"));statement.execute(sql);statement.execute(sql);try(var rows=statement.executeQuery("SELECT lifecycle_status,retired_at,purged_at FROM coffee_photo_retirements")){assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("RETIRED");assertThat(rows.getTimestamp(2).toInstant()).isEqualTo(java.time.Instant.parse("2026-09-01T00:00:00Z"));assertThat(rows.getTimestamp(3)).isNull();}}finally{statement.execute("SET search_path TO public");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
  }
 }
 @Test void persistence_failure_after_physical_delete_keeps_pending_and_retry_completes()throws Exception{
  jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);var ids=seed();var file=storageProperties.getDirectory().resolve(ids[1]+".png");java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.writeString(file,"evidence");String constraint="test_coffee_purge_"+ids[1].toString().replace("-","");boolean installed=false;
  try{
   jdbc.update("UPDATE coffee_photos SET photo_uri=? WHERE photo_id=?","/api/coffees/photo-assets/"+ids[1]+".png",ids[1]);String route="/api/admin/studio/coffee-media/"+ids[1]+"/lifecycle";String body="{\"commandId\":\"%s\",\"coffeeId\":\"%s\",\"status\":\"%s\",\"reason\":\"Review\"}";
   mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body.formatted(UUID.randomUUID(),ids[0],"RETIRED"))).andExpect(status().isAccepted());jdbc.update("UPDATE coffee_photo_retirements SET retired_at='2023-08-01' WHERE photo_id=?",ids[1]);mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body.formatted(UUID.randomUUID(),ids[0],"PURGE_REQUESTED"))).andExpect(status().isAccepted());
   jdbc.execute("ALTER TABLE coffee_photo_retirements ADD CONSTRAINT "+constraint+" CHECK (photo_id<>'"+ids[1]+"'::uuid OR lifecycle_status<>'DELETED') NOT VALID");installed=true;
   org.assertj.core.api.Assertions.assertThatThrownBy(()->cleaner.run(100)).isInstanceOf(org.springframework.dao.DataAccessException.class);assertThat(java.nio.file.Files.exists(file)).isFalse();assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM coffee_photo_retirements WHERE photo_id=?",String.class,ids[1])).isEqualTo("DELETION_PENDING");jdbc.execute("ALTER TABLE coffee_photo_retirements DROP CONSTRAINT "+constraint);installed=false;cleaner.run(100);assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM coffee_photo_retirements WHERE photo_id=?",String.class,ids[1])).isEqualTo("DELETED");
  }finally{if(installed)jdbc.execute("ALTER TABLE coffee_photo_retirements DROP CONSTRAINT "+constraint);java.nio.file.Files.deleteIfExists(file);}
 }
 @Autowired com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CleanCoffeeMediaObjects cleaner;
 @Autowired com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.storage.CoffeePhotoStorageProperties storageProperties;
 @Test void explicit_purge_request_after_thirty_days_is_accepted_and_audited_before_targeted_cleanup()throws Exception{
  jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);
  var ids=seed();String reference="/api/coffees/photo-assets/"+ids[1]+".png";jdbc.update("UPDATE coffee_photos SET photo_uri=? WHERE coffee_id=? AND photo_id=?",reference,ids[0],ids[1]);String route="/api/admin/studio/coffee-media/"+ids[1]+"/lifecycle";
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content("{\"commandId\":\""+UUID.randomUUID()+"\",\"coffeeId\":\""+ids[0]+"\",\"status\":\"RETIRED\",\"reason\":\"Obsolete\"}")).andExpect(status().isAccepted());
  var file=storageProperties.getDirectory().resolve(ids[1]+".png");java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.writeString(file,"evidence");
  jdbc.update("UPDATE coffee_photo_retirements SET retired_at='2023-08-01' WHERE photo_id=?",ids[1]);UUID command=UUID.randomUUID();
  mvc.perform(post(route).with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content("{\"commandId\":\""+command+"\",\"coffeeId\":\""+ids[0]+"\",\"status\":\"PURGE_REQUESTED\",\"reason\":\"Storage cleanup\"}")).andExpect(status().isAccepted());
  assertThat(jdbc.queryForObject("SELECT action FROM admin_audit_log WHERE command_id=?",String.class,command)).isEqualTo("COFFEE_MEDIA_PURGE_REQUESTED");
  assertThat(java.nio.file.Files.exists(file)).isTrue();
  mvc.perform(get("/api/admin/studio/coffee-media/"+ids[1]).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELETION_PENDING")).andExpect(jsonPath("$.canRestore").value(false)).andExpect(jsonPath("$.canPurge").value(false));
  cleaner.run(100);assertThat(java.nio.file.Files.exists(file)).isFalse();assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM coffee_photo_retirements WHERE photo_id=?",String.class,ids[1])).isEqualTo("DELETED");
  mvc.perform(get("/api/admin/studio/coffee-media/"+ids[1]).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELETED")).andExpect(jsonPath("$.purgedAt").isNotEmpty());
  project(ids[0],"CoffeeMediaCatalogSnapshotEvent");mvc.perform(get("/api/admin/media/COFFEE:"+ids[1]).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELETED"));
  cleaner.run(100);

 }
}
