package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.SpringOutboxEventRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.*;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.CommentJpaEntity;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "admin.security.bootstrap-user-ids=99999999-9999-9999-9999-999999999999")
class WriteModerationControllerIT extends AbstractBaseE2E {
    @Autowired MockMvc mvc;
    @Autowired SpringCommentRepository comments;
    @Autowired SpringContentReportRepository reports;
    @Autowired SpringUserBlockRepository blocks;
    @Autowired SpringOutboxEventRepository outbox;
    @Autowired DateTimeProvider dateTimeProvider;
    private final UUID author=UUID.randomUUID(), reporter=UUID.randomUUID(), operator=UUID.fromString("99999999-9999-9999-9999-999999999999");
    private final UUID commentId=UUID.randomUUID(), targetId=UUID.randomUUID();

    @BeforeEach void setup() {
        reports.deleteAll(); blocks.deleteAll(); comments.deleteAll(); outbox.deleteAll();
        ((DeterministicDateTimeProvider) dateTimeProvider).instantOfNow=Instant.parse("2026-09-11T10:00:00Z");
        comments.save(new CommentJpaEntity(commentId,targetId,author,null,"visible content",
                Instant.parse("2026-09-11T09:00:00Z"),null,null,ModerationStatus.PUBLISHED,0));
    }

    @Test void report_block_and_admin_decision_follow_authenticated_command_paths() throws Exception {
        UUID reportId=UUID.randomUUID(), blockId=UUID.randomUUID();
        mvc.perform(post("/api/social/comments/{id}/reports",commentId).with(user(reporter))
                .contentType("application/json").content("""
                    {"commandId":"%s","reportId":"%s","reason":"SPAM","details":"Repeated links","at":"2026-09-11T09:59:00Z"}
                    """.formatted(UUID.randomUUID(),reportId))).andExpect(status().isAccepted());
        mvc.perform(post("/api/social/blocks").with(user(reporter)).contentType("application/json").content("""
                    {"commandId":"%s","blockId":"%s","blockedUserId":"%s","active":true,"at":"2026-09-11T09:59:00Z"}
                    """.formatted(UUID.randomUUID(),blockId,author))).andExpect(status().isAccepted());
        mvc.perform(post("/api/admin/moderation/reports/{id}/decision",reportId).with(user(operator))
                .contentType("application/json").content("""
                    {"commandId":"%s","actionId":"%s","commentId":"%s","decision":"HIDDEN","reason":"Confirmed spam","at":"2026-09-11T10:00:00Z"}
                    """.formatted(UUID.randomUUID(),UUID.randomUUID(),commentId))).andExpect(status().isAccepted());

        assertThat(reports.findById(reportId).orElseThrow().getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(blocks.findByBlockerIdAndBlockedUserId(reporter,author)).get().extracting(b -> b.isActive()).isEqualTo(true);
        assertThat(comments.findById(commentId).orElseThrow().getModeration()).isEqualTo(ModerationStatus.HIDDEN);
        assertThat(outbox.findAll()).hasSize(3);
    }

    @Test void ordinary_user_cannot_apply_an_admin_moderation_decision() throws Exception {
        mvc.perform(post("/api/admin/moderation/reports/{id}/decision",UUID.randomUUID()).with(user(reporter))
                .contentType("application/json").content("""
                    {"commandId":"%s","actionId":"%s","commentId":"%s","decision":"HIDDEN","at":"2026-09-11T10:00:00Z"}
                    """.formatted(UUID.randomUUID(),UUID.randomUUID(),commentId)))
                .andExpect(status().isForbidden());
        assertThat(outbox.findAll()).isEmpty();
    }

    @Test void invalid_report_reason_is_a_client_error_and_never_creates_a_command() throws Exception {
        mvc.perform(post("/api/social/comments/{id}/reports",commentId).with(user(reporter))
                .contentType("application/json").content("""
                    {"commandId":"%s","reportId":"%s","reason":"NOT_A_REASON","at":"2026-09-11T10:00:00Z"}
                    """.formatted(UUID.randomUUID(),UUID.randomUUID())))
                .andExpect(status().isBadRequest());
        assertThat(outbox.findAll()).isEmpty();
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user(UUID id) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("roles", List.of("USER","ADMIN")));
    }
}
