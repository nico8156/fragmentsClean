package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

import java.util.Map;

public final class IntegrationEventTypeCatalog {

    private static final Map<String, String> STABLE_TYPES = Map.ofEntries(
            Map.entry("ArticleCreatedEvent", "article.created"),
            Map.entry("AuthUserCreatedEvent", "auth.user.created"),
            Map.entry("AuthUserLoggedInEvent", "auth.user.logged_in"),
            Map.entry("CoffeeCreatedEvent", "coffee.created"),
            Map.entry("CoffeeDeletedEvent", "coffee.deleted"),
            Map.entry("CoffeeOpeningHoursImportedEvent", "coffee.opening_hours_imported"),
            Map.entry("CoffeePhotosImportedEvent", "coffee.photos_imported"),
            Map.entry("AppUserCreatedEvent", "app.user.created"),
            Map.entry("AppUserProfileUpdatedEvent", "app.user.profile_updated"),
            Map.entry("LikeSetEvent", "social.like.set"),
            Map.entry("CommentCreatedEvent", "social.comment.created"),
            Map.entry("CommentUpdatedEvent", "social.comment.updated"),
            Map.entry("CommentDeletedEvent", "social.comment.deleted"),
            Map.entry("TicketVerifyAcceptedEvent", "ticket.verify.accepted"),
            Map.entry("TicketVerificationCompletedEvent", "ticket.verification.completed")
    );

    private IntegrationEventTypeCatalog() {
    }

    public static String stableTypeForClassName(String className) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return STABLE_TYPES.getOrDefault(simpleName, simpleName);
    }

    public static int currentVersion(String stableEventType) {
        return 1;
    }
}
