package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticlePublicationApprovalPort;
import java.util.UUID;

public final class ApproveStudioArticlePublication {
    private final ArticlePublicationApprovalPort approvals;

    public ApproveStudioArticlePublication(ArticlePublicationApprovalPort approvals) {
        this.approvals = approvals;
    }

    public UUID execute(String token) {
        if (token == null || token.isBlank()) throw new IllegalArgumentException("approval token is required");
        return approvals.approve(token.trim());
    }
}
