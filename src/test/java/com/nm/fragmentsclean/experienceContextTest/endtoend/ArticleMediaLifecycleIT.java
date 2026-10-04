package com.nm.fragmentsclean.experienceContextTest.endtoend;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.test.context.TestPropertySource;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;import java.util.*;import static org.assertj.core.api.Assertions.*;
@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class ArticleMediaLifecycleIT extends AbstractExperienceE2E {
 @Autowired org.springframework.test.web.servlet.MockMvc mvc;
 @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Autowired com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaUploadRecorder uploads;
 final String admin="99999999-9999-9999-9999-999999999999";
 @org.junit.jupiter.api.BeforeEach void identity(){jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",UUID.fromString(admin),admin);}
 @Test void retires_and_restores_an_unused_upload_with_reason_without_removing_the_source_record()throws Exception{
  UUID article=UUID.randomUUID();String ref="https://images.test/lifecycle-"+article+".png";UUID media=com.nm.fragmentsclean.articleContext.read.ArticleMediaReferenceIdentity.of(ref);
  uploads.record(article,ref,"upload.png","image/png",new byte[]{1},640,480,UUID.fromString(admin),"STUDIO");String path="/api/admin/studio/article-media/"+media;
  mvc.perform(post(path+"/lifecycle").with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content("{\"commandId\":\""+UUID.randomUUID()+"\",\"status\":\"RETIRED\",\"reason\":\"Unused cover\"}" )).andExpect(status().isAccepted());
  mvc.perform(get(path).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RETIRED"));
  assertThat(jdbc.queryForObject("SELECT count(*) FROM article_media_uploads WHERE media_id=?",Integer.class,media)).isOne();
  mvc.perform(post(path+"/lifecycle").with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content("{\"commandId\":\""+UUID.randomUUID()+"\",\"status\":\"ACTIVE\",\"reason\":\"Restore for editing\"}" )).andExpect(status().isAccepted());
  mvc.perform(get(path).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.status").value("ACTIVE"));
 }

 @Autowired com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRouting router;
 @Autowired com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository outbox;
 @Autowired com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaCatalogPublisher producer;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 record Upload(UUID article,UUID media,String ref){}
 Upload upload(){UUID article=UUID.randomUUID();String ref="https://images.test/media-"+article+".png";UUID media=com.nm.fragmentsclean.articleContext.read.ArticleMediaReferenceIdentity.of(ref);uploads.record(article,ref,"file.png","image/png",new byte[]{1},640,480,UUID.fromString(admin),"STUDIO");return new Upload(article,media,ref);}
 String body(UUID command,String state,String reason){return "{\"commandId\":\""+command+"\",\"status\":\""+state+"\",\"reason\":\""+reason+"\"}";}
 org.springframework.test.web.servlet.ResultActions change(Upload upload,UUID command,String state,String reason)throws Exception{return mvc.perform(post("/api/admin/studio/article-media/"+upload.media()+"/lifecycle").with(jwt().jwt(j->j.subject(admin))).contentType("application/json").content(body(command,state,reason)));}
 void route(Upload upload){var factory=new com.nm.fragmentsclean.platform.eventing.IntegrationEventEnvelopeFactory();for(var event:outbox.findAll())if(event.getEventType().endsWith("ArticleMediaCatalogSnapshotEvent")&&event.getAggregateId().equals(upload.article().toString()))router.route(factory.from(event,"media-catalog-events"));}
 void revision(UUID article,String ref){
  jdbc.update("INSERT INTO articles(article_id,slug,locale,author_id,author_name,title,intro,blocks_json,conclusion,tags_json,reading_time_min,coffee_ids_json,created_at,updated_at,status,version) VALUES(?,?,'fr-FR',?,'Editorial','Legacy revision','Intro','[]','Fin','[]',1,'[]',now(),now(),'DRAFT',1)",article,article.toString(),UUID.randomUUID());
  jdbc.update("INSERT INTO article_revisions(revision_id,article_id,revision_number,title,introduction,conclusion,cover_reference,cover_width,cover_height,cover_alt,reading_time_min,status,created_at,updated_at,version) VALUES(?,?,1,'Old revision','Intro','Fin',?,640,480,'Cover',1,'ARCHIVED',now(),now(),1)",UUID.randomUUID(),article,ref);
 }
 @Test void used_upload_in_another_articles_old_revision_is_rejected_and_audit_replay_is_idempotent()throws Exception{
  var upload=upload();UUID linked=UUID.randomUUID();revision(linked,upload.ref());UUID command=UUID.randomUUID();
  change(upload,command,"RETIRED","Unused").andExpect(status().isUnprocessableEntity());
  mvc.perform(get("/api/admin/commands/"+command).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.status").value("REJECTED"));
  mvc.perform(get("/api/admin/studio/article-media/"+upload.media()).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.canRetire").value(false)).andExpect(jsonPath("$.usages").value(1));
  var unused=upload();UUID retire=UUID.randomUUID();change(unused,retire,"RETIRED","Obsolete image").andExpect(status().isAccepted());change(unused,retire,"RETIRED","Obsolete image").andExpect(status().isAccepted());
  assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit_log WHERE command_id=?",Integer.class,retire)).isOne();
  var audit=jdbc.queryForMap("SELECT actor_user_id,action,reason FROM admin_audit_log WHERE command_id=?",retire);assertThat(audit.get("actor_user_id")).isEqualTo(UUID.fromString(admin));assertThat(audit.get("reason")).isEqualTo("Obsolete image");assertThat(audit.get("action")).isEqualTo("ARTICLE_MEDIA_RETIRED");
  mvc.perform(get("/api/admin/commands/"+retire).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.status").value("APPLIED"));
 }
 @Test void source_revokes_preview_before_projection_and_rejects_a_new_reference_transaction()throws Exception{
  var upload=upload();route(upload);String path="/api/admin/media/ARTICLE:"+upload.media();
  mvc.perform(get(path).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.previewUrl").value(upload.ref()));
  change(upload,UUID.randomUUID(),"RETIRED","Unused image").andExpect(status().isAccepted());
  mvc.perform(get(path).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.status").value("AVAILABLE")).andExpect(jsonPath("$.previewUrl").isEmpty());
  route(upload);mvc.perform(get(path).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.status").value("DELETION_PENDING"));
  UUID linked=UUID.randomUUID();var transaction=new org.springframework.transaction.support.TransactionTemplate(transactions);
  assertThatThrownBy(()->transaction.executeWithoutResult(s->{revision(linked,upload.ref());producer.publish(linked);})).isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class);
  assertThat(jdbc.queryForObject("SELECT count(*) FROM article_revisions WHERE article_id=?",Integer.class,linked)).isZero();
  change(upload,UUID.randomUUID(),"ACTIVE","Return to editing").andExpect(status().isAccepted());route(upload);
  mvc.perform(get(path).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.status").value("AVAILABLE")).andExpect(jsonPath("$.previewUrl").value(upload.ref()));
  transaction.executeWithoutResult(s->{revision(linked,upload.ref());producer.publish(linked);});
 }
 @Test void endpoints_require_admin_and_reason_and_unknown_resources_are_never_retired()throws Exception{
  var upload=upload();String path="/api/admin/studio/article-media/"+upload.media();
  mvc.perform(get(path)).andExpect(status().isUnauthorized());mvc.perform(get(path).with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());
  mvc.perform(post(path+"/lifecycle").contentType("application/json").content(body(UUID.randomUUID(),"RETIRED","Unused"))).andExpect(status().isUnauthorized());
  mvc.perform(post(path+"/lifecycle").with(jwt().jwt(j->j.subject(UUID.randomUUID().toString()))).contentType("application/json").content(body(UUID.randomUUID(),"RETIRED","Unused"))).andExpect(status().isForbidden());
  change(upload,UUID.randomUUID(),"RETIRED"," ").andExpect(status().isUnprocessableEntity());change(upload,UUID.randomUUID(),"UNKNOWN","Unused").andExpect(status().isUnprocessableEntity());
  mvc.perform(get("/api/admin/studio/article-media/"+UUID.randomUUID()).with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isNotFound());
  change(new Upload(upload.article(),UUID.randomUUID(),upload.ref()),UUID.randomUUID(),"RETIRED","Unused").andExpect(status().isUnprocessableEntity());
 }
 @Test void an_in_flight_reference_holds_retirement_until_commit_then_blocks_it()throws Exception{
  var upload=upload();var transaction=new org.springframework.transaction.support.TransactionTemplate(transactions);var ready=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
  try{
   var writer=executor.submit(()->transaction.executeWithoutResult(s->{UUID article=UUID.randomUUID();revision(article,upload.ref());producer.publish(article);ready.countDown();try{if(!release.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new IllegalStateException("Writer timed out");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}));
   assertThat(ready.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
   var retiring=executor.submit(()->{try{return change(upload,UUID.randomUUID(),"RETIRED","Unused").andReturn().getResponse().getStatus();}catch(Exception e){throw new RuntimeException(e);}});
   assertThatThrownBy(()->retiring.get(150,java.util.concurrent.TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);
   release.countDown();writer.get(5,java.util.concurrent.TimeUnit.SECONDS);assertThat(retiring.get(5,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(422);
   assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM article_media_uploads WHERE media_id=?",String.class,upload.media())).isEqualTo("ACTIVE");
  }finally{release.countDown();executor.shutdownNow();}
 }
 @Test void journal_lists_the_reason_actor_and_restore_without_cross_target_entries()throws Exception{
  var upload=upload();change(upload,UUID.randomUUID(),"RETIRED","Obsolete upload").andExpect(status().isAccepted());change(upload,UUID.randomUUID(),"ACTIVE","Reuse cover").andExpect(status().isAccepted());
  mvc.perform(get("/api/admin/studio/article-media/"+upload.media()+"/operations").with(jwt().jwt(j->j.subject(admin)))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[*].action").value(org.hamcrest.Matchers.containsInAnyOrder("ARTICLE_MEDIA_RESTORED","ARTICLE_MEDIA_RETIRED"))).andExpect(jsonPath("$[*].actorUserId").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(admin)))).andExpect(jsonPath("$[*].reason").value(org.hamcrest.Matchers.containsInAnyOrder("Reuse cover","Obsolete upload")));
  mvc.perform(get("/api/admin/studio/article-media/"+upload.media()+"/operations")).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/admin/studio/article-media/"+UUID.randomUUID()+"/operations").with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.length()").value(0));
 }
 @Test void upload_registration_replay_preserves_the_retirement_date_and_metadata()throws Exception{
  var upload=upload();change(upload,UUID.randomUUID(),"RETIRED","Keep for retention").andExpect(status().isAccepted());
  var retiredAt=java.time.Instant.parse("2026-01-01T00:00:00Z");
  jdbc.update("UPDATE article_media_uploads SET updated_at=? WHERE media_id=?",java.sql.Timestamp.from(retiredAt),upload.media());
  uploads.record(upload.article(),upload.ref(),"replacement.png","image/webp",new byte[]{1,2,3},800,600,UUID.fromString(admin),"STUDIO");
  var row=jdbc.queryForMap("SELECT lifecycle_status,updated_at,content_type,size_bytes,width,height FROM article_media_uploads WHERE media_id=?",upload.media());
  assertThat(row.get("lifecycle_status")).isEqualTo("RETIRED");
  assertThat(((java.sql.Timestamp)row.get("updated_at")).toInstant()).isEqualTo(retiredAt);
  assertThat(row.get("content_type")).isEqualTo("image/png");assertThat(((Number)row.get("size_bytes")).longValue()).isEqualTo(1);
  assertThat(row.get("width")).isEqualTo(640);assertThat(row.get("height")).isEqualTo(480);
 }
 @Test void active_upload_registration_still_refreshes_measured_metadata_and_preserves_original_actor(){
  var upload=upload();
  uploads.record(upload.article(),upload.ref(),"retry.webp","image/webp",new byte[]{1,2,3},800,600,UUID.randomUUID(),"GENERATION");
  var row=jdbc.queryForMap("SELECT lifecycle_status,content_type,size_bytes,width,height,uploaded_by,purpose FROM article_media_uploads WHERE media_id=?",upload.media());
  assertThat(row.get("lifecycle_status")).isEqualTo("ACTIVE");assertThat(row.get("content_type")).isEqualTo("image/webp");
  assertThat(((Number)row.get("size_bytes")).longValue()).isEqualTo(3);assertThat(row.get("width")).isEqualTo(800);assertThat(row.get("height")).isEqualTo(600);
  assertThat(row.get("uploaded_by")).isEqualTo(UUID.fromString(admin));assertThat(row.get("purpose")).isEqualTo("STUDIO");
 }
 @Test void admin_can_request_purge_only_after_thirty_full_days_without_deleting_inline()throws Exception{
  UUID article=UUID.randomUUID();String ref="/api/articles/image-assets/"+UUID.randomUUID()+".png";UUID media=com.nm.fragmentsclean.articleContext.read.ArticleMediaReferenceIdentity.of(ref);
  uploads.record(article,ref,"purge.png","image/png",new byte[]{1},640,480,UUID.fromString(admin),"STUDIO");var upload=new Upload(article,media,ref);
  change(upload,UUID.randomUUID(),"RETIRED","Obsolete").andExpect(status().isAccepted());
  change(upload,UUID.randomUUID(),"PURGE_REQUESTED","Storage cleanup").andExpect(status().isUnprocessableEntity());
  jdbc.update("UPDATE article_media_uploads SET updated_at=? WHERE media_id=?",java.sql.Timestamp.from(java.time.Instant.parse("2023-08-01T00:00:00Z")),media);
  UUID command=UUID.randomUUID();change(upload,command,"PURGE_REQUESTED","Storage cleanup").andExpect(status().isAccepted());
  assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM article_media_uploads WHERE media_id=?",String.class,media)).isEqualTo("DELETION_PENDING");
  assertThat(jdbc.queryForObject("SELECT action FROM admin_audit_log WHERE command_id=?",String.class,command)).isEqualTo("ARTICLE_MEDIA_PURGE_REQUESTED");
 }
 @Autowired com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.CleanArticleMediaObjects cleanup;
 @Autowired com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.ArticleImageStorageProperties storageProperties;
 @Test void requested_purge_cleans_one_file_then_marks_source_and_catalogue_deleted()throws Exception{
  UUID article=UUID.randomUUID();String file=UUID.randomUUID()+".png";String ref="/api/articles/image-assets/"+file;UUID media=com.nm.fragmentsclean.articleContext.read.ArticleMediaReferenceIdentity.of(ref);var upload=new Upload(article,media,ref);
  var path=storageProperties.getDirectory().resolve(file);java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.write(path,new byte[]{1});
  try{
   uploads.record(article,ref,"clean.png","image/png",new byte[]{1},640,480,UUID.fromString(admin),"STUDIO");change(upload,UUID.randomUUID(),"RETIRED","Unused").andExpect(status().isAccepted());
   jdbc.update("UPDATE article_media_uploads SET updated_at=? WHERE media_id=?",java.sql.Timestamp.from(java.time.Instant.parse("2023-08-01T00:00:00Z")),media);
   mvc.perform(get("/api/admin/studio/article-media/"+media).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.canPurge").value(true)).andExpect(jsonPath("$.purgeEligibleAt").value("2023-08-31T00:00:00Z"));
   UUID command=UUID.randomUUID();change(upload,command,"PURGE_REQUESTED","Cleanup approved").andExpect(status().isAccepted());change(upload,command,"PURGE_REQUESTED","Cleanup approved").andExpect(status().isAccepted());assertThat(java.nio.file.Files.exists(path)).isTrue();
   assertThat(jdbc.queryForObject("SELECT count(*) FROM admin_audit_log WHERE command_id=?",Integer.class,command)).isOne();
   change(upload,UUID.randomUUID(),"ACTIVE","Cannot restore").andExpect(status().isUnprocessableEntity());route(upload);
   mvc.perform(get("/api/admin/media/ARTICLE:"+media).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.previewUrl").isEmpty());
   cleanup.run(100);assertThat(java.nio.file.Files.exists(path)).isFalse();assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM article_media_uploads WHERE media_id=?",String.class,media)).isEqualTo("DELETED");route(upload);
   mvc.perform(get("/api/admin/media/ARTICLE:"+media).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.status").value("DELETED")).andExpect(jsonPath("$.previewUrl").isEmpty());
   var transaction=new org.springframework.transaction.support.TransactionTemplate(transactions);assertThatThrownBy(()->transaction.executeWithoutResult(t->{UUID linked=UUID.randomUUID();revision(linked,ref);producer.publish(linked);})).isInstanceOf(com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException.class);
  }finally{java.nio.file.Files.deleteIfExists(path);}
 }
 @Test void foreign_reference_and_shared_retained_usage_never_request_purge()throws Exception{
  var external=upload();change(external,UUID.randomUUID(),"RETIRED","Unused").andExpect(status().isAccepted());jdbc.update("UPDATE article_media_uploads SET updated_at='2023-08-01' WHERE media_id=?",external.media());change(external,UUID.randomUUID(),"PURGE_REQUESTED","Foreign key").andExpect(status().isUnprocessableEntity());
  mvc.perform(get("/api/admin/studio/article-media/"+external.media()).with(jwt().jwt(j->j.subject(admin)))).andExpect(jsonPath("$.canPurge").value(false));
  UUID article=UUID.randomUUID();String ref="/api/articles/image-assets/"+UUID.randomUUID()+".png";UUID media=com.nm.fragmentsclean.articleContext.read.ArticleMediaReferenceIdentity.of(ref);uploads.record(article,ref,"shared.png","image/png",new byte[]{1},640,480,UUID.fromString(admin),"STUDIO");var used=new Upload(article,media,ref);change(used,UUID.randomUUID(),"RETIRED","Initially unused").andExpect(status().isAccepted());jdbc.update("UPDATE article_media_uploads SET updated_at='2023-08-01' WHERE media_id=?",media);UUID linked=UUID.randomUUID();revision(linked,ref);
  change(used,UUID.randomUUID(),"PURGE_REQUESTED","Must retain").andExpect(status().isUnprocessableEntity());assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM article_media_uploads WHERE media_id=?",String.class,media)).isEqualTo("RETIRED");jdbc.update("DELETE FROM article_revisions WHERE article_id=?",linked);
 }
 @Test void additive_purge_upgrade_is_repeatable_preserves_retirement_and_accepts_terminal_states()throws Exception{
  String schema="article_purge_"+UUID.randomUUID().toString().replace("-","");
  try(var connection=jdbc.getDataSource().getConnection();var statement=connection.createStatement()){
   statement.execute("CREATE SCHEMA "+schema);
   try{
    statement.execute("SET search_path TO "+schema+",pg_catalog");statement.execute("CREATE TABLE article_media_uploads(media_id uuid primary key,updated_at timestamptz,lifecycle_status varchar(16) CHECK(lifecycle_status IN ('ACTIVE','RETIRED')))");
    UUID id=UUID.randomUUID();statement.execute("INSERT INTO article_media_uploads VALUES('"+id+"','2023-08-01T00:00:00Z','RETIRED')");
    var script=new org.springframework.core.io.ClassPathResource("db/release/2026-10-04-article-media-purge.sql");org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,script);org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,script);
    try(var rows=statement.executeQuery("SELECT lifecycle_status,updated_at FROM article_media_uploads")){assertThat(rows.next()).isTrue();assertThat(rows.getString(1)).isEqualTo("RETIRED");assertThat(rows.getTimestamp(2).toInstant()).isEqualTo(java.time.Instant.parse("2023-08-01T00:00:00Z"));}
    statement.execute("UPDATE article_media_uploads SET lifecycle_status='DELETION_PENDING'");statement.execute("UPDATE article_media_uploads SET lifecycle_status='DELETED'");
   }finally{statement.execute("SET search_path TO public,pg_catalog");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
  }
 }
 @Test void completion_persistence_failure_rolls_back_marker_after_file_deletion_and_retries()throws Exception{
  UUID article=UUID.randomUUID();String file=UUID.randomUUID()+".png";String ref="/api/articles/image-assets/"+file;UUID media=com.nm.fragmentsclean.articleContext.read.ArticleMediaReferenceIdentity.of(ref);var upload=new Upload(article,media,ref);var path=storageProperties.getDirectory().resolve(file);java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.write(path,new byte[]{1});Long version=null;
  try{
   uploads.record(article,ref,"retry.png","image/png",new byte[]{1},640,480,UUID.fromString(admin),"STUDIO");change(upload,UUID.randomUUID(),"RETIRED","Unused").andExpect(status().isAccepted());jdbc.update("UPDATE article_media_uploads SET updated_at='2023-08-01' WHERE media_id=?",media);change(upload,UUID.randomUUID(),"PURGE_REQUESTED","Cleanup").andExpect(status().isAccepted());
   version=jdbc.queryForObject("SELECT version FROM article_media_catalog_versions WHERE article_id=?",Long.class,article);jdbc.update("UPDATE article_media_catalog_versions SET version=9223372036854775807 WHERE article_id=?",article);
   assertThatThrownBy(()->cleanup.run(100)).isInstanceOf(org.springframework.dao.DataAccessException.class);assertThat(java.nio.file.Files.exists(path)).isFalse();assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM article_media_uploads WHERE media_id=?",String.class,media)).isEqualTo("DELETION_PENDING");
   jdbc.update("UPDATE article_media_catalog_versions SET version=? WHERE article_id=?",version,article);cleanup.run(100);assertThat(jdbc.queryForObject("SELECT lifecycle_status FROM article_media_uploads WHERE media_id=?",String.class,media)).isEqualTo("DELETED");
  }finally{if(version!=null)jdbc.update("UPDATE article_media_catalog_versions SET version=? WHERE article_id=? AND version=9223372036854775807",version,article);java.nio.file.Files.deleteIfExists(path);}
 }
}
