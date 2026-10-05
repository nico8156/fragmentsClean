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
class StudioUserCommentsIT extends AbstractBaseE2E {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired ObjectMapper json;
 private static final String ADMIN="99999999-9999-9999-9999-999999999999",ROUTE="/api/admin/comments";
 private UUID seed(UUID author,String moderation,String at){
  UUID id=UUID.randomUUID();var time=Timestamp.from(Instant.parse(at));
  jdbc.update("INSERT INTO social_comments_projection VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,UUID.randomUUID(),author,null,"Comment retained for review",time,null,"SOFT_DELETED".equals(moderation)?time:null,moderation,0,0,1);return id;
 }
 @Test void reads_the_author_comments_including_moderated_and_soft_deleted_content()throws Exception{
  UUID author=UUID.randomUUID();for(String state:java.util.List.of("PUBLISHED","HIDDEN","SOFT_DELETED"))seed(author,state,"2026-10-05T00:00:00Z");seed(UUID.randomUUID(),"PUBLISHED","2026-10-05T00:00:00Z");
  mvc.perform(get(ROUTE).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(3)).andExpect(jsonPath("$.items[0].authorId").value(author.toString()))
   .andExpect(jsonPath("$.items[0].email").doesNotExist()).andExpect(jsonPath("$.items[0].targetId").exists());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).param("moderation","HIDDEN").with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].moderation").value("HIDDEN"));
 }
 @Test void pagination_preserves_microsecond_precision_and_breaks_equal_time_ties()throws Exception{
  UUID author=UUID.randomUUID();UUID newer=seed(author,"PUBLISHED","2026-10-05T00:00:00.000900Z"),older=seed(author,"PUBLISHED","2026-10-05T00:00:00.000800Z");seed(author,"HIDDEN","2026-10-05T00:00:00.000800Z");
  var first=mvc.perform(get(ROUTE).param("authorId",author.toString()).param("limit","1").with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(newer.toString())).andReturn();
  var page=json.readTree(first.getResponse().getContentAsByteArray());var seen=new java.util.HashSet<String>();seen.add(newer.toString());
  for(int i=0;i<2;i++){
   var response=mvc.perform(get(ROUTE).param("authorId",author.toString()).param("limit","1").param("cursor",page.path("nextCursor").asText()).with(jwt().jwt(j->j.subject(ADMIN))))
    .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andReturn();page=json.readTree(response.getResponse().getContentAsByteArray());assertThat(seen.add(page.path("items").get(0).path("id").asText())).isTrue();
  }
  assertThat(seen).hasSize(3).contains(older.toString());assertThat(page.path("nextCursor").isNull()).isTrue();
 }
 @Test void validates_queries_and_requires_admin()throws Exception{
  UUID author=UUID.randomUUID();mvc.perform(get(ROUTE).param("authorId",author.toString())).andExpect(status().isUnauthorized());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(author.toString())))).andExpect(status().isForbidden());
  for(String limit:java.util.List.of("0","101"))mvc.perform(get(ROUTE).param("authorId",author.toString()).param("limit",limit).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).param("moderation","VISIBLE").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).param("cursor","bad").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  mvc.perform(get(ROUTE).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
 }
 @Test void erased_account_comments_are_not_returned()throws Exception{
  UUID author=UUID.randomUUID();seed(author,"HIDDEN","2026-10-05T00:00:00Z");new com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jdbc.JdbcSocialAccountDataEraser(jdbc).erase(author);
  mvc.perform(get(ROUTE).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
 }
 @Test void additive_index_migration_can_be_replayed()throws Exception{
  String schema="user_comments_"+UUID.randomUUID().toString().replace("-","");
  try(var connection=jdbc.getDataSource().getConnection();var statement=connection.createStatement()){
   statement.execute("CREATE SCHEMA "+schema);
   try{
    statement.execute("SET search_path TO "+schema);statement.execute("CREATE TABLE social_comments_projection (LIKE public.social_comments_projection INCLUDING ALL)");
    String migration=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/release/2026-10-05-studio-user-comments.sql"));statement.execute(migration);statement.execute(migration);
    try(var result=statement.executeQuery("SELECT indexdef FROM pg_indexes WHERE schemaname='"+schema+"' AND indexname='ix_social_comments_author_created'")){
     assertThat(result.next()).isTrue();assertThat(result.getString(1)).contains("author_id, created_at DESC, id DESC");
    }
   }finally{statement.execute("SET search_path TO public");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
  }
 }

}
