package com.nm.fragmentsclean.experienceContextTest.endtoend;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.sql.Timestamp;import java.time.Instant;import java.util.UUID;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;import com.fasterxml.jackson.databind.ObjectMapper;
@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class StudioUserReportsIT extends AbstractExperienceE2E {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired ObjectMapper json;
 private static final String ADMIN="99999999-9999-9999-9999-999999999999",ROUTE="/api/admin/experiences/reports";
 private UUID seed(UUID author,String status){
  UUID experience=UUID.randomUUID(),report=UUID.randomUUID(),coffee=UUID.randomUUID();var at=Timestamp.from(Instant.parse("2026-10-05T00:00:00Z"));
  jdbc.update("INSERT INTO experience_views VALUES(?,?,?,?,?,?,?,?,?,?)",experience,author,coffee,"Report content","PUBLISHED","VISIBLE",at,at,null,1);
  jdbc.update("INSERT INTO experience_reports_projection(report_id,experience_id,coffee_id,author_id,reporter_id,reason,details,status,created_at,version) VALUES(?,?,?,?,?,'SPAM','Duplicated advertising',?,?,1)",report,experience,coffee,author,UUID.randomUUID(),status,at);return report;
 }
 @Test void pages_received_reports_by_author_and_status_without_disclosing_the_reporter()throws Exception{
  UUID author=UUID.randomUUID();seed(author,"OPEN");seed(author,"OPEN");seed(author,"RESOLVED");seed(UUID.randomUUID(),"OPEN");
  var first=mvc.perform(get(ROUTE).param("authorId",author.toString()).param("status","OPEN").param("limit","1").with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].status").value("OPEN"))
   .andExpect(jsonPath("$.items[0].reason").value("SPAM")).andExpect(jsonPath("$.items[0].details").value("Duplicated advertising"))
   .andExpect(jsonPath("$.items[0].reporterId").doesNotExist()).andExpect(jsonPath("$.items[0].experienceId").exists()).andReturn();
  var page=json.readTree(first.getResponse().getContentAsByteArray());
  var second=mvc.perform(get(ROUTE).param("authorId",author.toString()).param("status","OPEN").param("limit","1").param("cursor",page.path("nextCursor").asText()).with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty()).andReturn();
  assertThat(json.readTree(second.getResponse().getContentAsByteArray()).path("items").get(0).path("reportId").asText()).isNotEqualTo(page.path("items").get(0).path("reportId").asText());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(3));
 }
 @Test void requires_admin_and_validates_filters()throws Exception{
  UUID author=UUID.randomUUID();mvc.perform(get(ROUTE).param("authorId",author.toString())).andExpect(status().isUnauthorized());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(author.toString())))).andExpect(status().isForbidden());
  for(String limit:java.util.List.of("0","101"))mvc.perform(get(ROUTE).param("authorId",author.toString()).param("limit",limit).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).param("status","bad").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).param("cursor","bad").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  mvc.perform(get(ROUTE).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
 }
 @Test void account_erasure_removes_received_reports()throws Exception{
  UUID author=UUID.randomUUID();seed(author,"OPEN");new com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jdbc.JdbcExperienceAccountDataEraser(jdbc).erase(author);
  mvc.perform(get(ROUTE).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
 }
 @Test void additive_index_migration_can_be_replayed()throws Exception{
  String schema="user_reports_"+UUID.randomUUID().toString().replace("-","");
  try(var connection=jdbc.getDataSource().getConnection();var statement=connection.createStatement()){
   statement.execute("CREATE SCHEMA "+schema);
   try{
    statement.execute("SET search_path TO "+schema);statement.execute("CREATE TABLE experience_reports_projection (LIKE public.experience_reports_projection INCLUDING ALL)");
    String migration=java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/release/2026-10-05-studio-user-reports.sql"));statement.execute(migration);statement.execute(migration);
    try(var result=statement.executeQuery("SELECT indexdef FROM pg_indexes WHERE schemaname='"+schema+"' AND indexname='ix_experience_reports_author_created'")){
     assertThat(result.next()).isTrue();assertThat(result.getString(1)).contains("author_id, created_at DESC, report_id DESC");
    }
   }finally{statement.execute("SET search_path TO public");statement.execute("DROP SCHEMA "+schema+" CASCADE");}
  }
 }

}
