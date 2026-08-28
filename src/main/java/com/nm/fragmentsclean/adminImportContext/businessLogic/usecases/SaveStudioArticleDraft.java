package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleSubmission;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

import java.util.List;
import java.util.UUID;

public final class SaveStudioArticleDraft {
    private final ArticleAuthoringPort authoring;
    private final UuidGenerator ids;
    private final DateTimeProvider clock;

    public SaveStudioArticleDraft(ArticleAuthoringPort authoring, UuidGenerator ids, DateTimeProvider clock) {
        this.authoring = authoring;
        this.ids = ids;
        this.clock = clock;
    }

    public Result execute(StudioArticleSubmission submission) {
        UUID commandId = ids.generate();
        UUID articleId = submission.articleId() == null ? ids.generate() : submission.articleId();
        UUID revisionId = submission.revisionId() == null ? ids.generate() : submission.revisionId();
        var command = new StudioArticleCommand(commandId, clock.now(), articleId, revisionId,
                required(submission.slug(), "slug"), defaulted(submission.locale(), "fr-FR"),
                java.util.Objects.requireNonNull(submission.authorId(), "authorId is required"),
                required(submission.authorName(), "authorName"), required(submission.title(), "title"),
                required(submission.intro(), "intro"),
                submission.blocks() == null ? List.of() : submission.blocks(),
                required(submission.conclusion(), "conclusion"),
                submission.cover() == null ? null : submission.cover().url(),
                submission.cover() == null ? null : submission.cover().width(),
                submission.cover() == null ? null : submission.cover().height(),
                submission.cover() == null ? null : submission.cover().alt(),
                submission.tags() == null ? List.of() : submission.tags(), submission.readingTimeMin(),
                submission.coffeeIds() == null ? List.of() : submission.coffeeIds());
        authoring.saveDraft(command);
        return new Result(commandId, articleId, revisionId, "PENDING");
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim();
    }

    private static String defaulted(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public record Result(UUID commandId, UUID articleId, UUID revisionId, String status) { }
}
