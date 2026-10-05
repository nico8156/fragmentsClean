package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.sql.Timestamp;import java.time.Instant;import java.util.UUID;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;import com.fasterxml.jackson.databind.ObjectMapper;
@TestPropertySource(properties={"admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999","app.outbox.dispatcher.scheduling-enabled=false"})
class StudioUserSocialModerationIT extends AbstractBaseE2E {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired ObjectMapper json;
 private static final String ADMIN="99999999-9999-9999-9999-999999999999",REPORTS="/api/admin/comments/reports",ACTIONS="/api/admin/comments/actions";
 private UUID seed(UUID author,String status,String at){
  UUID report=UUID.randomUUID(),comment=UUID.randomUUID(),target=UUID.randomUUID();var time=Timestamp.from(Instant.parse(at));
  jdbc.update("INSERT INTO social_content_reports_projection VALUES(?,?,?,?,?,?,?,?,?,?,?)",report,comment,target,author,UUID.randomUUID(),"SPAM","Report details",status,time,null,1);
  jdbc.update("INSERT INTO social_moderation_actions_projection VALUES(?,?,?,?,?,?,?)",UUID.randomUUID(),report,comment,UUID.randomUUID(),"HIDDEN","Reviewed spam",time);
  return report;
 }
 @Test void reads_only_reports_received_by_the_author_without_reporter_identity()throws Exception{
  UUID author=UUID.randomUUID();seed(author,"OPEN","2026-10-05T00:00:00Z");seed(author,"RESOLVED","2026-10-05T00:00:00Z");seed(UUID.randomUUID(),"OPEN","2026-10-05T00:00:00Z");
  mvc.perform(get(REPORTS).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.items[0].reporterId").doesNotExist()).andExpect(jsonPath("$.items[0].commentId").exists());
  mvc.perform(get(REPORTS).param("authorId",author.toString()).param("status","OPEN").with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].status").value("OPEN"));
 }
 @Test void reads_the_author_decisions_with_report_comment_operator_and_reason()throws Exception{
  UUID author=UUID.randomUUID();UUID report=seed(author,"RESOLVED","2026-10-05T00:00:00Z");seed(UUID.randomUUID(),"RESOLVED","2026-10-05T00:00:00Z");
  mvc.perform(get(ACTIONS).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].reportId").value(report.toString())).andExpect(jsonPath("$.items[0].commentId").exists()).andExpect(jsonPath("$.items[0].operatorId").exists()).andExpect(jsonPath("$.items[0].reason").value("Reviewed spam"));
 }

 @Test void both_pages_preserve_microseconds_and_equal_time_ties()throws Exception{
  UUID author=UUID.randomUUID();seed(author,"OPEN","2026-10-05T00:00:00.000900Z");seed(author,"OPEN","2026-10-05T00:00:00.000800Z");seed(author,"RESOLVED","2026-10-05T00:00:00.000800Z");
  for(String route:java.util.List.of(REPORTS,ACTIONS)){
   String cursor=null;var seen=new java.util.HashSet<String>();
   for(int i=0;i<3;i++){
    var request=get(route).param("authorId",author.toString()).param("limit","1").with(jwt().jwt(j->j.subject(ADMIN)));if(cursor!=null)request.param("cursor",cursor);
    var response=mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andReturn();var page=json.readTree(response.getResponse().getContentAsByteArray());
    assertThat(seen.add(page.path("items").get(0).path(route.equals(REPORTS)?"reportId":"actionId").asText())).isTrue();
    if(i==0)assertThat(page.path("items").get(0).path(route.equals(REPORTS)?"createdAt":"occurredAt").asText()).isEqualTo("2026-10-05T00:00:00.000900Z");
    cursor=page.path("nextCursor").isNull()?null:page.path("nextCursor").asText();
   }
   assertThat(seen).hasSize(3);assertThat(cursor).isNull();
  }
 }
 @Test void both_routes_validate_and_enforce_admin_access()throws Exception{
  UUID author=UUID.randomUUID();
  for(String route:java.util.List.of(REPORTS,ACTIONS)){
   mvc.perform(get(route).param("authorId",author.toString())).andExpect(status().isUnauthorized());
   mvc.perform(get(route).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(author.toString())))).andExpect(status().isForbidden());
   for(String limit:java.util.List.of("0","101"))mvc.perform(get(route).param("authorId",author.toString()).param("limit",limit).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
   mvc.perform(get(route).param("authorId",author.toString()).param("cursor","bad").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
   mvc.perform(get(route).param("authorId","bad").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
   mvc.perform(get(route).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
   mvc.perform(get(route).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
  }
  mvc.perform(get(REPORTS).param("authorId",author.toString()).param("status","HIDDEN").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
 }
 @Test void erased_account_reports_and_decisions_are_not_reintroduced()throws Exception{
  UUID author=UUID.randomUUID();seed(author,"RESOLVED","2026-10-05T00:00:00Z");
  new com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jdbc.JdbcSocialAccountDataEraser(jdbc).erase(author);
  for(String route:java.util.List.of(REPORTS,ACTIONS))mvc.perform(get(route).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
 }

 @Test void author_index_upgrade_is_additive_and_replayable()throws Exception{
  String schema="social_activity_"+UUID.randomUUID().toString().replace("-","");
  try(var connection=jdbc.getDataSource().getConnection();var statement=connection.createStatement()){
   statement.execute("CREATE SCHEMA "+schema);
   try{
    statement.execute("SET search_path TO "+schema);statement.execute("CREATE TABLE social_content_reports_projection (LIKE public.social_content_reports_projection INCLUDING ALL)");
    String migration=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/release/2026-10-05-studio-user-social-moderation.sql"));statement.execute(migration);statement.execute(migration);
    try(var result=statement.executeQuery("SELECT indexdef FROM pg_indexes WHERE schemaname='"+schema+"' AND indexname='ix_social_reports_author_created'")){assertThat(result.next()).isTrue();assertThat(result.getString(1)).contains("author_id, created_at DESC, report_id DESC");}
   }finally{statement.execute("SET search_path TO public");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
  }
 }

}
