package com.nm.fragmentsclean.experienceContextTest.endtoend;

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

@TestPropertySource(properties="admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class StudioModerationReportDetailIT extends AbstractExperienceE2E {
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate jdbc;
 private static final String ADMIN="99999999-9999-9999-9999-999999999999";
 private static final String SOCIAL="/api/admin/moderation/reports/", EXPERIENCE="/api/admin/experience-moderation/reports/";
 private record Fixture(UUID report,UUID content,UUID action,UUID operator) {}
 private Fixture seed(boolean social,String state,String decision) {
  UUID report=UUID.randomUUID(),content=UUID.randomUUID(),author=UUID.randomUUID(),target=UUID.randomUUID(),action=UUID.randomUUID(),operator=UUID.randomUUID();
  var at=Timestamp.from(Instant.parse("2026-10-05T00:00:00Z"));
  if(social){
   jdbc.update("INSERT INTO social_comments_projection VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",content,target,author,null,"Signalé",at,null,null,"HIDDEN",0,0,1);
   jdbc.update("INSERT INTO social_content_reports_projection VALUES (?,?,?,?,?,?,?,?,?,?,?)",report,content,target,author,UUID.randomUUID(),"SPAM","À examiner",state,at,at,1);
   jdbc.update("INSERT INTO social_moderation_actions_projection VALUES (?,?,?,?,?,?,?)",action,report,content,operator,decision,"Examen manuel",at);
  }else{
   jdbc.update("INSERT INTO experience_views VALUES(?,?,?,?,?,?,?,?,?,?)",content,author,target,"Signalé","PUBLISHED","HIDDEN",at,at,null,1);
   jdbc.update("INSERT INTO experience_reports_projection(report_id,experience_id,coffee_id,author_id,reporter_id,reason,details,status,created_at,version) VALUES(?,?,?,?,?,'SPAM','À examiner',?,?,1)",report,content,target,author,UUID.randomUUID(),state,at);
   jdbc.update("INSERT INTO experience_moderation_actions_projection(action_id,report_id,experience_id,operator_id,decision,reason,occurred_at) VALUES(?,?,?,?,?,?,?)",action,report,content,operator,decision,"Examen manuel",at);
  }
  return new Fixture(report,content,action,operator);
 }
 @Test void reads_a_resolved_comment_report_and_only_its_own_audit() throws Exception { detail(true,"RESOLVED","HIDDEN"); }
 @Test void reads_a_dismissed_experience_report_and_only_its_own_audit() throws Exception { detail(false,"DISMISSED","VISIBLE"); }
 private void detail(boolean social,String state,String decision)throws Exception {
  var expected=seed(social,state,decision);seed(social,state,"HIDDEN");
  mvc.perform(get((social?SOCIAL:EXPERIENCE)+expected.report()).with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.reportId").value(expected.report().toString()))
   .andExpect(jsonPath(social?"$.commentId":"$.experienceId").value(expected.content().toString()))
   .andExpect(jsonPath("$.status").value(state)).andExpect(jsonPath("$.content").value("Signalé"))
   .andExpect(jsonPath("$.actions.length()").value(1)).andExpect(jsonPath("$.actions[0].actionId").value(expected.action().toString()))
   .andExpect(jsonPath("$.actions[0].operatorId").value(expected.operator().toString()))
   .andExpect(jsonPath("$.actions[0].decision").value(decision)).andExpect(jsonPath("$.actions[0].reason").value("Examen manuel"));
 }
 @Test void enforces_admin_authentication_for_both_details()throws Exception {
  for(var route:java.util.List.of(SOCIAL,EXPERIENCE)){
   mvc.perform(get(route+UUID.randomUUID())).andExpect(status().isUnauthorized());
   mvc.perform(get(route+UUID.randomUUID()).with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());
  }
 }
 @Test void returns_not_found_for_missing_or_wrong_owner_reports_and_validates_ids()throws Exception {
  var social=seed(true,"OPEN","HIDDEN");var experience=seed(false,"OPEN","HIDDEN");
  for(var route:java.util.List.of(SOCIAL,EXPERIENCE)){
   mvc.perform(get(route+UUID.randomUUID()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isNotFound());
   mvc.perform(get(route+"bad").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  }
  mvc.perform(get(SOCIAL+experience.report()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isNotFound());
  mvc.perform(get(EXPERIENCE+social.report()).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isNotFound());
 }
}
