package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationRequestPort;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationScheduleGuard;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationIdPort;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Locale;

/** Application use case for the scheduled trigger; it never calls a provider. */
@Component
public final class ScheduleArticleGeneration {
    private final ArticleGenerationRequestPort requests;
    private final ArticleGenerationScheduleGuard guard;
    private final ArticleGenerationIdPort ids;
    private final DateTimeProvider clock;

    public ScheduleArticleGeneration(ArticleGenerationRequestPort requests,
                                     ArticleGenerationScheduleGuard guard,
                                     ArticleGenerationIdPort ids,
                                     DateTimeProvider clock) {
        this.requests = requests;
        this.guard = guard;
        this.ids = ids;
        this.clock = clock;
    }

    public boolean execute(String subject, String locale, int maxPending, int deduplicationHours) {
        if (subject == null || subject.isBlank()) return false;
        var now = clock.now();
        var normalizedLocale = locale == null || locale.isBlank() ? "fr-FR" : locale.trim();
        var cleanSubject = subject.trim();
        if (!guard.mayRequest(cleanSubject, now, maxPending, deduplicationHours)) return false;
        var articleId = ids.next();
        requests.request(new RequestArticleGenerationCommand(
                ids.next(), now, ids.next(), articleId, ids.next(), cleanSubject,
                slug(cleanSubject, articleId), normalizedLocale, new java.util.UUID(0, 0), "Scheduled Article Generator",
                ArticleAuthoringTrigger.SCHEDULED));
        return true;
    }

    private static String slug(String subject, java.util.UUID articleId) {
        var normalized = Normalizer.normalize(subject, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return (normalized.isBlank() ? "article" : normalized.substring(0, Math.min(80, normalized.length())))
                + "-" + articleId.toString().substring(0, 8);
    }
}
