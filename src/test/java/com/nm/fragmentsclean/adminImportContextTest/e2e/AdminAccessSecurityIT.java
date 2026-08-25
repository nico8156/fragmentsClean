package com.nm.fragmentsclean.adminImportContextTest.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.authenticationContextTest.e2e.AbstractBaseE2E;

class AdminAccessSecurityIT extends AbstractBaseE2E {
	@Autowired MockMvc mockMvc;
	@Autowired ObjectMapper objectMapper;
	@Autowired JdbcTemplate jdbcTemplate;

	@BeforeEach
	void clean() {
		jdbcTemplate.update("DELETE FROM admin_user_access");
		jdbcTemplate.update("DELETE FROM refresh_tokens");
		jdbcTemplate.update("DELETE FROM app_users");
		jdbcTemplate.update("DELETE FROM auth_users");
	}

	@Test
	void jwt_allowlisted_user_can_access_admin_api() throws Exception {
		var login = mockMvc.perform(post("/auth/google/mobile")
				.contentType("application/json")
				.content("""
						{"authorizationCode":"admin-it","codeVerifier":"verifier-admin-it","redirectUri":"com.googleusercontent.apps.255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe:/oauthredirect"}
						"""))
			.andExpect(status().isOk()).andReturn();
		var body = objectMapper.readTree(login.getResponse().getContentAsByteArray());
		var token = body.path("accessToken").asText();
		var userId = UUID.fromString(body.path("user").path("id").asText());
		jdbcTemplate.update("INSERT INTO admin_user_access (user_id, granted_at, granted_by) VALUES (?, CURRENT_TIMESTAMP, ?)", userId, userId);

		mockMvc.perform(get("/api/admin/access/users").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].userId").value(userId.toString()))
				.andExpect(jsonPath("$[0].bootstrap").value(false));
	}

	@Test
	void non_allowlisted_jwt_is_forbidden_on_admin_api() throws Exception {
		var login = mockMvc.perform(post("/auth/google/mobile")
				.contentType("application/json")
				.content("""
						{"authorizationCode":"regular-it","codeVerifier":"verifier-regular-it","redirectUri":"com.googleusercontent.apps.255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe:/oauthredirect"}
						"""))
			.andExpect(status().isOk()).andReturn();
		var token = objectMapper.readTree(login.getResponse().getContentAsByteArray()).path("accessToken").asText();

		mockMvc.perform(get("/api/admin/access/users").header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden());
	}
}
