package com.nm.fragmentsclean.socialContextTest.endtoend.adapters.primary.springboot.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import com.nm.fragmentsclean.socialContext.read.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcModerationProjectionRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jdbc.JdbcSocialAccountDataEraser;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.*;

class ModerationReadModelIT extends AbstractBaseE2E {
    @Autowired JdbcTemplate jdbc;
    @Autowired ListCommentsQueryHandler comments;
    @Autowired ListBlockedUsersQueryHandler blockedUsers;
    @Autowired ListModerationReportsQueryHandler reports;
    @Autowired JdbcModerationProjectionRepository projections;
    private final UUID requester=UUID.randomUUID(), target=UUID.randomUUID();

    @BeforeEach void clean() {
        jdbc.update("DELETE FROM social_moderation_actions_projection");
        jdbc.update("DELETE FROM social_content_reports_projection");
        jdbc.update("DELETE FROM social_user_blocks_projection");
        jdbc.update("DELETE FROM social_comments_projection");
        jdbc.update("DELETE FROM user_social_projection");
    }

    @Test void comments_hide_global_moderation_personal_reports_and_blocked_authors() {
        UUID visibleAuthor=UUID.randomUUID(), reportedAuthor=UUID.randomUUID(), blockedAuthor=UUID.randomUUID();
        UUID visible=comment(visibleAuthor,"visible", "PUBLISHED", 1);
        UUID hidden=comment(UUID.randomUUID(),"hidden", "HIDDEN", 2);
        UUID reported=comment(reportedAuthor,"reported", "PUBLISHED", 3);
        UUID blocked=comment(blockedAuthor,"blocked", "PUBLISHED", 4);
        jdbc.update("INSERT INTO social_content_reports_projection VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(),reported,target,reportedAuthor,requester,"SPAM",null,"OPEN",Timestamp.from(now()),null,0);
        jdbc.update("INSERT INTO social_user_blocks_projection VALUES (?,?,?,?,?,?)",
                UUID.randomUUID(),requester,blockedAuthor,true,Timestamp.from(now()),0);

        var result=comments.handle(new ListCommentsQuery(requester,target,null,20,"retrieve"));
        assertThat(result.items()).extracting(item -> item.id()).containsExactly(visible);
        assertThat(result.items()).noneMatch(item -> item.id().equals(hidden) || item.id().equals(reported) || item.id().equals(blocked));
    }

    @Test void exposes_blocked_users_and_moderation_queue_from_local_read_models() {
        UUID author=UUID.randomUUID(), comment=comment(author,"reported content","PUBLISHED",1);
        UUID blockId=UUID.randomUUID(), reportId=UUID.randomUUID(), actionId=UUID.randomUUID();
        jdbc.update("INSERT INTO user_social_projection VALUES (?,?,?,?,?,?)",author,"Author",null,
                Timestamp.from(now()),Timestamp.from(now()),1);
        jdbc.update("INSERT INTO social_user_blocks_projection VALUES (?,?,?,?,?,?)",blockId,requester,author,true,Timestamp.from(now()),2);
        jdbc.update("INSERT INTO social_content_reports_projection VALUES (?,?,?,?,?,?,?,?,?,?,?)",reportId,comment,target,author,
                requester,"HARASSMENT","details","OPEN",Timestamp.from(now()),null,0);
        jdbc.update("INSERT INTO social_moderation_actions_projection VALUES (?,?,?,?,?,?,?)",actionId,reportId,comment,
                UUID.randomUUID(),"HIDDEN","history",Timestamp.from(now()));

        assertThat(blockedUsers.handle(requester)).singleElement().satisfies(view -> assertThat(view.userId()).isEqualTo(author));
        assertThat(reports.handle("OPEN",50)).singleElement().satisfies(view -> {
            assertThat(view.content()).isEqualTo("reported content");
            assertThat(view.authorName()).isEqualTo("Author");
            assertThat(view.reportCount()).isEqualTo(1);
            assertThat(view.actions()).singleElement().satisfies(action -> assertThat(action.actionId()).isEqualTo(actionId));
        });
    }

    @Test void projection_adapter_is_duplicate_safe_for_redelivery() {
        UUID author=UUID.randomUUID(), comment=comment(author,"content","PUBLISHED",0), reportId=UUID.randomUUID();
        var reported=new CommentReportedEvent(UUID.randomUUID(),UUID.randomUUID(),reportId,comment,target,author,requester,
                ReportReason.SPAM,null,ReportStatus.OPEN,0,now(),now());
        projections.apply(reported); projections.apply(reported);
        var moderated=new CommentModeratedEvent(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),reportId,comment,target,
                author,UUID.randomUUID(),ModerationStatus.HIDDEN,ReportStatus.RESOLVED,"confirmed",1,now(),now());
        projections.apply(moderated); projections.apply(moderated);
        var restored=new CommentModeratedEvent(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),reportId,comment,target,
                author,UUID.randomUUID(),ModerationStatus.PUBLISHED,ReportStatus.DISMISSED,"restored",2,now(),now());
        projections.apply(restored); projections.apply(restored);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM social_content_reports_projection WHERE report_id=?",Long.class,reportId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM social_content_reports_projection WHERE report_id=?",String.class,reportId)).isEqualTo("DISMISSED");
        assertThat(jdbc.queryForObject("SELECT version FROM social_content_reports_projection WHERE report_id=?",Long.class,reportId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM social_moderation_actions_projection WHERE report_id=?",Long.class,reportId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT moderation FROM social_comments_projection WHERE id=?",String.class,comment)).isEqualTo("PUBLISHED");
    }

    @Test void account_erasure_removes_reports_and_their_moderation_history() {
        UUID author=UUID.randomUUID(), comment=comment(author,"content","PUBLISHED",0), reportId=UUID.randomUUID();
        jdbc.update("INSERT INTO social_content_reports_projection VALUES (?,?,?,?,?,?,?,?,?,?,?)",reportId,comment,target,author,
                requester,"SPAM",null,"RESOLVED",Timestamp.from(now()),Timestamp.from(now()),1);
        jdbc.update("INSERT INTO social_moderation_actions_projection VALUES (?,?,?,?,?,?,?)",UUID.randomUUID(),reportId,comment,
                UUID.randomUUID(),"HIDDEN","confirmed",Timestamp.from(now()));

        new JdbcSocialAccountDataEraser(jdbc).erase(requester);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM social_content_reports_projection WHERE report_id=?",Long.class,reportId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM social_moderation_actions_projection WHERE report_id=?",Long.class,reportId)).isZero();
    }

    private UUID comment(UUID author,String body,String moderation,long version) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO social_comments_projection VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",id,target,author,null,body,
                Timestamp.from(now().plusSeconds(version)),null,null,moderation,0,0,version);
        return id;
    }
    private Instant now(){ return Instant.parse("2026-09-11T10:00:00Z"); }
}
