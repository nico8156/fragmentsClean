package com.nm.fragmentsclean.adminImportContextTest.e2e;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.nm.fragmentsclean.authenticationContextTest.e2e.AbstractBaseE2E;

@TestPropertySource(properties = {
 "admin.security.bootstrap-user-ids=99999999-9999-4999-8999-999999999999",
 "admin.security.exclusive-owner-id=99999999-9999-4999-8999-999999999999",
 "admin.security.bootstrap-emails=legacy@example.test"
})
class StudioExclusiveOwnerIT extends AbstractBaseE2E {
 static final String OWNER = "99999999-9999-4999-8999-999999999999";
 @Autowired MockMvc mvc;
 @Autowired JdbcTemplate jdbc;
 @Test void denies_a_previously_granted_admin_on_reads_commands_and_sse() throws Exception {
  UUID other = UUID.randomUUID();
  jdbc.update("INSERT INTO auth_users(id, provider, provider_user_id, email, email_verified, last_login_at) VALUES (?, 'GOOGLE', ?, 'legacy@example.test', TRUE, CURRENT_TIMESTAMP)", other, other.toString());
  jdbc.update("INSERT INTO admin_user_access(user_id, granted_at, granted_by) VALUES (?, CURRENT_TIMESTAMP, ?)", other, other);
  var identity = jwt().jwt(token -> token.subject(other.toString()).claim("email", "legacy@example.test"));
  mvc.perform(get("/api/admin/access/users").with(identity)).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/access/users").with(identity).contentType("application/json").content("{}" )).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/sync/events").with(identity)).andExpect(status().isForbidden());
 }
 @Test void owner_can_verify_access_but_cannot_change_the_allowlist() throws Exception {
  var identity = jwt().jwt(token -> token.subject(OWNER));
  mvc.perform(get("/api/admin/access/users").with(identity)).andExpect(status().isOk());
  mvc.perform(get("/api/admin/access/me").with(identity)).andExpect(status().isOk())
   .andExpect(jsonPath("$.userId").value(OWNER)).andExpect(jsonPath("$.exclusiveOwner").value(true));
  mvc.perform(post("/api/admin/access/users").with(identity).contentType("application/json").content("{}" )).andExpect(status().isForbidden());
  mvc.perform(delete("/api/admin/access/users/" + OWNER).with(identity)).andExpect(status().isForbidden());
 }
 @Test void anonymous_cannot_verify_admin_access() throws Exception {
  mvc.perform(get("/api/admin/access/me")).andExpect(status().isUnauthorized());
 }
}
