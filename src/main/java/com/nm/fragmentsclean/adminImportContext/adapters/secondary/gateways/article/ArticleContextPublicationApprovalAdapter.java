package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticlePublicationApprovalPort;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.ApproveArticlePublication;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class ArticleContextPublicationApprovalAdapter implements ArticlePublicationApprovalPort {
    private final ApproveArticlePublication approvals;

    public ArticleContextPublicationApprovalAdapter(ApproveArticlePublication approvals) {
        this.approvals = approvals;
    }

    @Override
    public UUID approve(String token) {
        return approvals.execute(token);
    }
}
