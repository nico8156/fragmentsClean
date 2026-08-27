package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationReview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleGenerationReviewPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticlePublicationApprovalPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudioArticleGenerationBoundaryTest {
    @Test
    void review_use_case_delegates_through_its_admin_port() {
        var sagaId = UUID.randomUUID();
        var expected = new StudioArticleGenerationReview(sagaId, UUID.randomUUID(), UUID.randomUUID(),
                "Sujet", "READY_FOR_REVIEW", 1, Instant.now(), null);
        ArticleGenerationReviewPort port = requested -> expected;

        assertThat(new GetStudioArticleGenerationReview(port).execute(sagaId)).isSameAs(expected);
    }

    @Test
    void approval_use_case_validates_then_delegates_the_trimmed_token() {
        var commandId = UUID.randomUUID();
        var captured = new java.util.concurrent.atomic.AtomicReference<String>();
        ArticlePublicationApprovalPort port = token -> { captured.set(token); return commandId; };

        assertThat(new ApproveStudioArticlePublication(port).execute("  signed-token  ")).isEqualTo(commandId);
        assertThat(captured.get()).isEqualTo("signed-token");
        assertThatThrownBy(() -> new ApproveStudioArticlePublication(port).execute(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
