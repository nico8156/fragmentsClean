package com.nm.fragmentsclean.platform.eventing;

import java.util.Map;

public final class IntegrationEventTypeCatalog {

  private static final Map<String, String> STABLE_TYPES =
      Map.ofEntries(
          Map.entry("ArticleCreatedEvent", "article.created"),
          Map.entry("ArticleDraftCreatedEvent", "article.draft.created"),
          Map.entry("ArticleDraftEditedEvent", "article.draft.edited"),
          Map.entry("ArticleRevisionSubmittedEvent", "article.revision.submitted"),
          Map.entry("ArticleRevisionPublishedEvent", "article.revision.published"),
          Map.entry("ArticleArchivedEvent", "article.archived"),
          Map.entry("ArticleGenerationRequestedEvent", "article.generation.requested"),
          Map.entry("ArticleGenerationCompletedEvent", "article.generation.completed"),
          Map.entry("ArticleGeneratedRevisionEditedEvent", "article.generated_revision.edited"),
          Map.entry("AuthUserCreatedEvent", "auth.user.created"),
          Map.entry("AuthUserLoggedInEvent", "auth.user.logged_in"),
          Map.entry("CoffeeArchivedEvent", "coffee.archived"),
          Map.entry("CoffeeCreatedEvent", "coffee.created"),
          Map.entry("CoffeeDetailsEditedEvent", "coffee.details_edited"),
          Map.entry("CoffeeDeletedEvent", "coffee.deleted"),
          Map.entry("CoffeePhotoAddedEvent", "coffee.photo_added"),
          Map.entry("CoffeePhotoDeletedEvent", "coffee.photo_deleted"),
          Map.entry("CoffeeOpeningHoursImportedEvent", "coffee.opening_hours_imported"),
          Map.entry("CoffeeOpeningHoursUpdatedEvent", "coffee.opening_hours_updated"),
          Map.entry("CoffeePhotosImportedEvent", "coffee.photos_imported"),
          Map.entry("CoffeePhotosArrangedEvent", "coffee.photos_arranged"),
          Map.entry("CoffeePublishedEvent", "coffee.published"),
          Map.entry("CoffeeUnpublishedEvent", "coffee.unpublished"),
          Map.entry("AppUserCreatedEvent", "app.user.created"),
          Map.entry("AppUserProfileUpdatedEvent", "app.user.profile_updated"),
          Map.entry("AppUserDeletionRequestedEvent", "app.user.deletion_requested"),
          Map.entry("AuthenticationAccountDataErasedEvent", "account.data_erased"),
          Map.entry("SocialAccountDataErasedEvent", "account.data_erased"),
          Map.entry("TicketAccountDataErasedEvent", "account.data_erased"),
          Map.entry("ExperienceAccountDataErasedEvent", "account.data_erased"),
          Map.entry("SavedCoffeeSetEvent", "user.saved_coffee.set"),
          Map.entry("LikeSetEvent", "social.like.set"),
          Map.entry("CommentCreatedEvent", "social.comment.created"),
          Map.entry("CommentUpdatedEvent", "social.comment.updated"),
          Map.entry("CommentDeletedEvent", "social.comment.deleted"),
          Map.entry("CommentReportedEvent", "social.comment.reported"),
          Map.entry("CommentModeratedEvent", "social.comment.moderated"),
          Map.entry("UserBlockChangedEvent", "social.user_block.changed"),
          Map.entry("TicketVerifyAcceptedEvent", "ticket.verify.accepted"),
          Map.entry("TicketVerificationCompletedEvent", "ticket.verification.completed"),
          Map.entry("TicketAdminUpdatedEvent", "ticket.admin.updated"),
          Map.entry("TicketAdminDeletedEvent", "ticket.admin.deleted"),
          Map.entry("ExperienceLifecycleChangedEvent", "experience.lifecycle.changed"),
          Map.entry("ExperienceSnapshotChangedEvent", "experience.snapshot.changed"),
          Map.entry("ExperienceReportedEvent", "experience.reported"),
          Map.entry("ExperienceModerationDecidedEvent", "experience.moderated"),
          Map.entry("ExperienceMediaChangedEvent", "experience.media.changed"));

  private IntegrationEventTypeCatalog() {}

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
