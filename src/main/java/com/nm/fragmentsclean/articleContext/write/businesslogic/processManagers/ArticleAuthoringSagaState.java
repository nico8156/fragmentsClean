package com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers;

public enum ArticleAuthoringSagaState {
    REQUESTED, GENERATION_PENDING, GENERATING, VALIDATING,
    READY_FOR_REVIEW, NOTIFICATION_PENDING, NOTIFIED, EDITING,
    PUBLICATION_REQUESTED, PUBLISHED, REJECTED, FAILED, EXPIRED, CANCELLED
}
