package com.nm.fragmentsclean.experienceContextTest.endtoend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class StudioUserModerationHistoryIT extends AbstractExperienceE2E {
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate jdbc;
 @Autowired ObjectMapper json;
 private static final String ADMIN="99999999-9999-9999-9999-999999999999";
 private static final String ROUTE="/api/admin/experiences/actions";
 private UUID seed(UUID author,String reason){
  UUID experience=UUID.randomUUID(),action=UUID.randomUUID();
  var time=Timestamp.from(Instant.parse("2026-10-05T00:00:00Z"));
  jdbc.update("INSERT INTO experience_views VALUES(?,?,?,?,?,?,?,?,?,?)",experience,author,UUID.randomUUID(),"History", "PUBLISHED","HIDDEN",time,time,null,1);
  jdbc.update("INSERT INTO experience_moderation_actions_projection(action_id,report_id,experience_id,operator_id,decision,reason,occurred_at) VALUES(?,null,?,?,?, ?,?)",action,experience,UUID.fromString(ADMIN),"HIDDEN",reason,time);
  return action;
 }
 @Test void pages_all_decisions_for_only_the_requested_author_even_at_equal_times()throws Exception{
  UUID author=UUID.randomUUID();seed(author,"Spam");seed(author,null);seed(UUID.randomUUID(),"Other author");
  var first=mvc.perform(get(ROUTE).param("authorId",author.toString()).param("limit","1").with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].operatorId").value(ADMIN))
   .andExpect(jsonPath("$.items[0].experienceId").exists()).andExpect(jsonPath("$.items[0].decision").value("HIDDEN"))
   .andExpect(jsonPath("$.items[0].email").doesNotExist()).andReturn();
  var firstPage=json.readTree(first.getResponse().getContentAsByteArray());
  var second=mvc.perform(get(ROUTE).param("authorId",author.toString()).param("limit","1").param("cursor",firstPage.path("nextCursor").asText()).with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.nextCursor").isEmpty()).andReturn();
  var secondPage=json.readTree(second.getResponse().getContentAsByteArray());
  assertThat(secondPage.path("items").get(0).path("actionId").asText()).isNotEqualTo(firstPage.path("items").get(0).path("actionId").asText());
  assertThat(firstPage.path("items").get(0).path("reason").asText()+secondPage.path("items").get(0).path("reason").asText()).doesNotContain("Other author");
 }
 @Test void validates_query_and_enforces_admin_access()throws Exception{
  UUID author=UUID.randomUUID();
  mvc.perform(get(ROUTE).param("authorId",author.toString())).andExpect(status().isUnauthorized());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(author.toString())))).andExpect(status().isForbidden());
  for(String limit:java.util.List.of("0","101"))mvc.perform(get(ROUTE).param("authorId",author.toString()).param("limit",limit).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  mvc.perform(get(ROUTE).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).param("cursor","broken").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  mvc.perform(get(ROUTE).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
 }
 @Test void erased_experience_projection_does_not_expose_orphaned_direct_audit()throws Exception{
  UUID author=UUID.randomUUID();seed(author,"Private reason");
  new com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jdbc.JdbcExperienceAccountDataEraser(jdbc).erase(author);
  mvc.perform(get(ROUTE).param("authorId",author.toString()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
 }
}
