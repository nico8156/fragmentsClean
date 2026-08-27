package com.nm.fragmentsclean.platform.eventing;

import java.util.Map;

public final class IntegrationEventTypeCatalog {

    private static final Map<String, String> STABLE_TYPES = Map.ofEntries(
            Map.entry("ArticleCreatedEvent", "article.created"),
            Map.entry("AuthUserCreatedEvent", "auth.user.created"),
            Map.entry("AuthUserLoggedInEvent", "auth.user.logged_in"),
            Map.entry("CoffeeArchivedEvent", "coffee.archived"),
            Map.entry("CoffeeCreatedEvent", "coffee.created"),
            Map.entry("CoffeeDeletedEvent", "coffee.deleted"),
            Map.entry("CoffeePhotoAddedEvent", "coffee.photo_added"),
            Map.entry("CoffeePhotoDeletedEvent", "coffee.photo_deleted"),
            Map.entry("CoffeeOpeningHoursImportedEvent", "coffee.opening_hours_imported"),
            Map.entry("CoffeePhotosImportedEvent", "coffee.photos_imported"),
            Map.entry("CoffeePublishedEvent", "coffee.published"),
            Map.entry("AppUserCreatedEvent", "app.user.created"),
            Map.entry("AppUserProfileUpdatedEvent", "app.user.profile_updated"),
            Map.entry("SavedCoffeeSetEvent", "user.saved_coffee.set"),
            Map.entry("LikeSetEvent", "social.like.set"),
            Map.entry("CommentCreatedEvent", "social.comment.created"),
            Map.entry("CommentUpdatedEvent", "social.comment.updated"),
            Map.entry("CommentDeletedEvent", "social.comment.deleted"),
            Map.entry("TicketVerifyAcceptedEvent", "ticket.verify.accepted"),
            Map.entry("TicketVerificationCompletedEvent", "ticket.verification.completed")
            ,Map.entry("TicketAdminUpdatedEvent", "ticket.admin.updated")
            ,Map.entry("TicketAdminDeletedEvent", "ticket.admin.deleted")
    );

    private IntegrationEventTypeCatalog() {
    }

    public static String stableTypeForClassName(String className) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return STABLE_TYPES.getOrDefault(simpleName, simpleName);
    }

    public static String stableTypeForClassName(String className, String destination) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        if ("app-users-events".equals(destination)) {
            return switch (simpleName) {
                case "CoffeeCreatedEvent" -> "coffee.saved_coffee_projection.created";
                case "CoffeeArchivedEvent" -> "coffee.saved_coffee_projection.archived";
                case "CoffeeDeletedEvent" -> "coffee.saved_coffee_projection.deleted";
                default -> stableTypeForClassName(className);
            };
        }
        return stableTypeForClassName(className);
    }

    public static int currentVersion(String stableEventType) {
        return 1;
    }
}
