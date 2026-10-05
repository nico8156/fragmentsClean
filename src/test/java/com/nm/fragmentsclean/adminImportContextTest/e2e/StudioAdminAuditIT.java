package com.nm.fragmentsclean.adminImportContextTest.e2e;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.sql.Timestamp;import java.time.Instant;import java.util.UUID;
import org.junit.jupiter.api.Test;import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.params.ParameterizedTest;import org.junit.jupiter.params.provider.ValueSource;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.authenticationContextTest.e2e.AbstractBaseE2E;
@TestPropertySource(properties={"admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999","app.outbox.dispatcher.scheduling-enabled=false"})
class StudioAdminAuditIT extends AbstractBaseE2E {
 @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;@Autowired ObjectMapper json;
 static final String ADMIN="99999999-9999-9999-9999-999999999999",ROUTE="/api/admin/operations/audit";
 @BeforeEach void resetAudit(){jdbc.update("DELETE FROM admin_audit_log");}
 UUID seed(UUID actor,String type,UUID target,UUID command,String outcome,String at){
  jdbc.update("INSERT INTO auth_users(id,provider,provider_user_id,email_verified,last_login_at) VALUES(?,'GOOGLE',?,true,now()) ON CONFLICT DO NOTHING",actor,actor.toString());
  UUID id=UUID.randomUUID();jdbc.update("INSERT INTO admin_audit_log(id,actor_user_id,action,target_type,target_id,command_id,outcome,reason,occurred_at) VALUES(?,?,?,?,?,?,?,?,?)",id,actor,"COFFEE_PUBLISHED",type,target,command,outcome,"Operator reason",Timestamp.from(Instant.parse(at)));return id;
 }
 @Test void searches_existing_audit_by_resource_operator_command_and_outcome()throws Exception{
  UUID actor=UUID.randomUUID(),target=UUID.randomUUID(),command=UUID.randomUUID();UUID id=seed(actor,"COFFEE",target,command,"APPLIED","2026-10-05T00:00:00Z");
  seed(actor,"COFFEE",UUID.randomUUID(),command,"REJECTED","2026-10-05T00:00:00Z");seed(UUID.randomUUID(),"COFFEE",target,UUID.randomUUID(),"APPLIED","2026-10-05T00:00:00Z");
  mvc.perform(get(ROUTE).param("targetType","COFFEE").param("targetId",target.toString()).param("actorId",actor.toString()).param("commandId",command.toString()).param("outcome","APPLIED").with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].id").value(id.toString())).andExpect(jsonPath("$.items[0].reason").value("Operator reason")).andExpect(jsonPath("$.items[0].email").doesNotExist());
 }

 @ParameterizedTest @ValueSource(strings={"targetType","targetId","actorId","commandId","action","outcome"})
 void each_filter_excludes_a_different_record_independently(String filter)throws Exception {
  UUID actor=UUID.randomUUID(),target=UUID.randomUUID(),command=UUID.randomUUID();
  UUID expected=seed(actor,"COFFEE",target,command,"APPLIED","2026-10-05T00:00:00Z");
  UUID excluded=seed(filter.equals("actorId")?UUID.randomUUID():actor,filter.equals("targetType")?"ARTICLE":"COFFEE",filter.equals("targetId")?UUID.randomUUID():target,filter.equals("commandId")?UUID.randomUUID():command,filter.equals("outcome")?"REJECTED":"APPLIED","2026-10-05T00:00:00Z");
  if(filter.equals("action"))jdbc.update("UPDATE admin_audit_log SET action='COFFEE_IMPORTED' WHERE id=?",excluded);
  String value=switch(filter){case "targetType"->"COFFEE";case "targetId"->target.toString();case "actorId"->actor.toString();case "commandId"->command.toString();case "action"->"COFFEE_PUBLISHED";default->"APPLIED";};
  mvc.perform(get(ROUTE).param(filter,value).with(jwt().jwt(j->j.subject(ADMIN))))
    .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1))
    .andExpect(jsonPath("$.items[0].id").value(expected.toString()));
 }
 @Test void cursor_preserves_microseconds_and_equal_timestamp_order()throws Exception {
  UUID actor=UUID.randomUUID();
  UUID first=seed(actor,"COFFEE",null,null,"APPLIED","2026-10-05T00:00:00.000002Z");
  UUID a=seed(actor,"COFFEE",null,null,"APPLIED","2026-10-05T00:00:00.000001Z");
  UUID b=seed(actor,"COFFEE",null,null,"APPLIED","2026-10-05T00:00:00.000001Z");
  var tail=java.util.stream.Stream.of(a.toString(),b.toString()).sorted(java.util.Comparator.reverseOrder()).toList();
  var found=new java.util.ArrayList<String>();String cursor=null;
  for(int page=0;page<3;page++){
   var request=get(ROUTE).param("limit","1").with(jwt().jwt(j->j.subject(ADMIN)));
   if(cursor!=null)request.param("cursor",cursor);
   var response=mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andReturn();
   var body=json.readTree(response.getResponse().getContentAsByteArray());found.add(body.path("items").get(0).path("id").asText());
   cursor=body.path("nextCursor").isNull()?null:body.path("nextCursor").asText();
   assertThat(cursor==null).isEqualTo(page==2);
  }
  assertThat(found).containsExactly(first.toString(),tail.get(0),tail.get(1));
 }
 @Test void preserves_legacy_nullable_references_and_returns_empty_page()throws Exception {
  UUID actor=UUID.randomUUID();seed(actor,"REQUEST",null,null,"REJECTED","2026-10-05T00:00:00Z");
  mvc.perform(get(ROUTE).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk())
   .andExpect(jsonPath("$.items[0].targetId").doesNotExist()).andExpect(jsonPath("$.items[0].commandId").doesNotExist());
  mvc.perform(get(ROUTE).param("targetId",UUID.randomUUID().toString()).with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty()).andExpect(jsonPath("$.nextCursor").doesNotExist());
 }
 @Test void rejects_anonymous_and_non_admin_identities()throws Exception {
  mvc.perform(get(ROUTE)).andExpect(status().isUnauthorized());
  mvc.perform(get(ROUTE).with(jwt().jwt(j->j.subject(UUID.randomUUID().toString())))).andExpect(status().isForbidden());
 }
 @Test void rejects_invalid_filters_and_page_parameters_without_writing_audit()throws Exception {
  for(var pair:java.util.List.of(new String[]{"limit","0"},new String[]{"limit","101"},new String[]{"cursor","invalid"},new String[]{"cursor","x".repeat(513)},new String[]{"limit","abc"},new String[]{"actorId","not-a-uuid"},new String[]{"action","x".repeat(65)},new String[]{"targetType","x".repeat(65)},new String[]{"outcome","x".repeat(33)})){
   mvc.perform(get(ROUTE).param(pair[0],pair[1]).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isBadRequest());
  }
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_log",Long.class)).isZero();
 }
 @Test void default_page_is_bounded_and_reading_does_not_create_more_audit_entries()throws Exception {
  UUID actor=UUID.randomUUID();for(int n=0;n<31;n++)seed(actor,"COFFEE",null,null,"APPLIED","2026-10-05T00:00:00Z");
  mvc.perform(get(ROUTE).with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk())
   .andExpect(jsonPath("$.items.length()").value(30)).andExpect(jsonPath("$.nextCursor").isString());
  mvc.perform(get(ROUTE).param("limit","100").with(jwt().jwt(j->j.subject(ADMIN)))).andExpect(status().isOk())
   .andExpect(jsonPath("$.items.length()").value(31)).andExpect(jsonPath("$.nextCursor").doesNotExist());
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM admin_audit_log",Long.class)).isEqualTo(31L);
 }
 @Test void normalizes_blank_filters_and_keeps_sql_characters_literal()throws Exception {
  UUID actor=UUID.randomUUID();seed(actor,"COFFEE",null,null,"APPLIED","2026-10-05T00:00:00Z");
  mvc.perform(get(ROUTE).param("targetType"," COFFEE ").param("action"," COFFEE_PUBLISHED ").param("outcome"," ").with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
  mvc.perform(get(ROUTE).param("action","' OR TRUE --").with(jwt().jwt(j->j.subject(ADMIN))))
   .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
 }

}
