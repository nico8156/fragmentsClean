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
}
