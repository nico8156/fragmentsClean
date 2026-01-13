package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ReadCommentsControllerIT extends AbstractBaseE2E {

	private static final UUID TARGET_ID = UUID.fromString("e67b3b3b-3b3b-3b3b-3b3b-03b3b3b3b3b3");
	private static final UUID AUTHOR_1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID AUTHOR_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private UUID COMMENT_1_ID;
	private UUID COMMENT_2_ID;

	@BeforeEach
	void setup() {
		jdbcTemplate.update("DELETE FROM social_comments_projection");

		Instant older = Instant.parse("2024-01-01T10:00:00Z");
		Instant newer = Instant.parse("2024-01-01T11:00:00Z");
		Instant deleted = Instant.parse("2024-01-01T12:00:00Z");

		COMMENT_1_ID = UUID.randomUUID();
		COMMENT_2_ID = UUID.randomUUID();
		UUID DELETED_COMMENT_ID = UUID.randomUUID();

		// Comment plus ancien
		jdbcTemplate.update("""
				INSERT INTO social_comments_projection
				(id, target_id, parent_id, author_id, body,
				 created_at, edited_at, deleted_at,
				 moderation, like_count, reply_count, version)
				VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
				""",
				COMMENT_1_ID,
				TARGET_ID,
				null,
				AUTHOR_1,
				"older comment",
				Timestamp.from(older),
				null,
				null,
				"ACCEPTED",
				1L,
				0L,
				1L);

		// Comment plus récent
		jdbcTemplate.update("""
				INSERT INTO social_comments_projection
				(id, target_id, parent_id, author_id, body,
				 created_at, edited_at, deleted_at,
				 moderation, like_count, reply_count, version)
				VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
				""",
				COMMENT_2_ID,
				TARGET_ID,
				null,
				AUTHOR_2,
				"newer comment",
				Timestamp.from(newer),
				null,
				null,
				"ACCEPTED",
				2L,
				1L,
				2L);

		// Comment supprimé → doit être filtré côté read
		jdbcTemplate.update("""
				INSERT INTO social_comments_projection
				(id, target_id, parent_id, author_id, body,
				 created_at, edited_at, deleted_at,
				 moderation, like_count, reply_count, version)
				VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
				""",
				DELETED_COMMENT_ID,
				TARGET_ID,
				null,
				AUTHOR_1,
				"deleted comment",
				Timestamp.from(newer),
				null,
				Timestamp.from(deleted),
				"ACCEPTED",
				5L,
				0L,
				3L);
	}

	@Test
	void can_list_comments_for_target_with_pagination_metadata() throws Exception {
		mockMvc.perform(
				get("/api/social/comments")
						.param("targetId", TARGET_ID.toString())
						.param("limit", "2")
						.param("op", "retrieve"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))

				// root fields
				.andExpect(jsonPath("$.targetId", is(TARGET_ID.toString())))
				.andExpect(jsonPath("$.op", is("retrieve")))
				.andExpect(jsonPath("$.items", hasSize(2)))
				.andExpect(jsonPath("$.nextCursor", anyOf(nullValue(), notNullValue())))
				.andExpect(jsonPath("$.prevCursor", anyOf(nullValue(), notNullValue())))
				.andExpect(jsonPath("$.serverTime", notNullValue()))

				// items[0] = plus récent
				.andExpect(jsonPath("$.items[0].id", is(COMMENT_2_ID.toString())))
				.andExpect(jsonPath("$.items[0].targetId", is(TARGET_ID.toString())))
				.andExpect(jsonPath("$.items[0].authorId", is(AUTHOR_2.toString())))
				.andExpect(jsonPath("$.items[0].body", is("newer comment")))
				.andExpect(jsonPath("$.items[0].createdAt", notNullValue()))
				// deletedAt est omis quand null
				.andExpect(jsonPath("$.items[0].deletedAt").doesNotExist())
				.andExpect(jsonPath("$.items[0].likeCount", is(2)))
				.andExpect(jsonPath("$.items[0].replyCount", is(1)))
				.andExpect(jsonPath("$.items[0].moderation").doesNotExist())
				.andExpect(jsonPath("$.items[0].version", is(2)))

				// items[1] = plus ancien
				.andExpect(jsonPath("$.items[1].id", is(COMMENT_1_ID.toString())))
				.andExpect(jsonPath("$.items[1].authorId", is(AUTHOR_1.toString())))
				.andExpect(jsonPath("$.items[1].body", is("older comment")));
	}

	@Test
	void can_list_older_comments_using_cursor() throws Exception {
		Instant newer = Instant.parse("2024-01-01T11:00:00Z");
		String cursor = newer.toEpochMilli() + ":" + COMMENT_2_ID;

		mockMvc.perform(
				get("/api/social/comments")
						.param("targetId", TARGET_ID.toString())
						.param("limit", "1")
						.param("op", "older")
						.param("cursor", cursor))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))

				.andExpect(jsonPath("$.targetId", is(TARGET_ID.toString())))
				.andExpect(jsonPath("$.op", is("older")))
				.andExpect(jsonPath("$.items", hasSize(1)))

				// On doit obtenir le commentaire plus ancien
				.andExpect(jsonPath("$.items[0].id", is(COMMENT_1_ID.toString())))
				.andExpect(jsonPath("$.items[0].body", is("older comment")))
				// deletedAt est omis quand null
				.andExpect(jsonPath("$.items[0].deletedAt").doesNotExist())
				.andExpect(jsonPath("$.items[0].createdAt", notNullValue()))

				.andExpect(jsonPath("$.serverTime", notNullValue()));
	}

	@Test
	void returns_empty_list_when_no_comments_for_target() throws Exception {
		UUID otherTarget = UUID.fromString("33333333-3333-3333-3333-333333333333");

		mockMvc.perform(
				get("/api/social/comments")
						.param("targetId", otherTarget.toString())
						.param("limit", "10")
						.param("op", "retrieve"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))

				.andExpect(jsonPath("$.targetId", is(otherTarget.toString())))
				.andExpect(jsonPath("$.op", is("retrieve")))
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.nextCursor", anyOf(nullValue(), is(nullValue()))))
				.andExpect(jsonPath("$.prevCursor", anyOf(nullValue(), is(nullValue()))))
				.andExpect(jsonPath("$.serverTime", notNullValue()));
	}
}
