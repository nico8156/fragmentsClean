package com.nm.fragmentsclean.experienceContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;
import java.time.Instant;
import java.util.UUID;

public final class Experience extends AggregateRoot {
    private final UUID userId;
    private final UUID coffeeId;
    private final Instant createdAt;
    private String message;
    private ExperiencePublicationStatus publicationStatus;
    private ExperienceModerationStatus moderationStatus;
    private Instant updatedAt;
    private Instant deletedAt;
    private long version;

    private Experience(Snapshot snapshot) {
        super(snapshot.experienceId());
        userId = snapshot.userId(); coffeeId = snapshot.coffeeId(); message = snapshot.message();
        publicationStatus = snapshot.publicationStatus(); moderationStatus = snapshot.moderationStatus();
        createdAt = snapshot.createdAt(); updatedAt = snapshot.updatedAt(); deletedAt = snapshot.deletedAt();
        version = snapshot.version();
    }

    public static Experience create(UUID experienceId, UUID userId, UUID coffeeId, String message,
                                    ExperiencePublicationStatus requestedStatus,
                                    ExperienceContentPolicy policy, Instant now) {
        if (requestedStatus == ExperiencePublicationStatus.DELETED) {
            throw new BusinessCommandRejectedException("EXPERIENCE_STATUS_INVALID", "A new experience cannot be deleted");
        }
        String normalized = requestedStatus == ExperiencePublicationStatus.PUBLISHED
                ? policy.requirePublishable(message) : policy.normalizeDraft(message);
        return new Experience(new Snapshot(experienceId, userId, coffeeId, normalized, requestedStatus,
                ExperienceModerationStatus.VISIBLE, now, now, null, 0));
    }

    public static Experience fromSnapshot(Snapshot snapshot) { return new Experience(snapshot); }

    public boolean updateMessage(UUID requesterId, String newMessage, ExperienceContentPolicy policy, Instant now) {
        requireOwner(requesterId); requireNotDeleted();
        String normalized = publicationStatus == ExperiencePublicationStatus.PUBLISHED
                ? policy.requirePublishable(newMessage) : policy.normalizeDraft(newMessage);
        if (java.util.Objects.equals(message, normalized)) return false;
        message = normalized; updatedAt = now; version++; return true;
    }

    public boolean publish(UUID requesterId, ExperienceContentPolicy policy, Instant now) {
        requireOwner(requesterId); requireNotDeleted();
        message = policy.requirePublishable(message);
        if (publicationStatus == ExperiencePublicationStatus.PUBLISHED) return false;
        publicationStatus = ExperiencePublicationStatus.PUBLISHED; updatedAt = now; version++; return true;
    }

    public boolean delete(UUID requesterId, Instant now) {
        requireOwner(requesterId);
        if (publicationStatus == ExperiencePublicationStatus.DELETED) return false;
        publicationStatus = ExperiencePublicationStatus.DELETED; deletedAt = now; updatedAt = now; version++; return true;
    }

    public boolean moderate(ExperienceModerationStatus decision, Instant now) {
        requireNotDeleted();
        if (moderationStatus == decision) return false;
        moderationStatus = decision; updatedAt = now; version++; return true;
    }

    public void registerChange(UUID commandId, String reason, Instant clientAt, Instant now) {
        registerEvent(new ExperienceSnapshotChangedEvent(UUID.randomUUID(), commandId, id, userId, coffeeId,
                message, publicationStatus, moderationStatus, reason, version, createdAt, updatedAt,
                deletedAt, now, clientAt));
        registerEvent(new ExperienceLifecycleChangedEvent(UUID.randomUUID(), id, userId, coffeeId,
                publicationStatus, moderationStatus, reason, version, now));
    }

    public void registerModerationDecision(UUID commandId, UUID actionId, UUID reportId, UUID operatorId,
                                           ExperienceReportStatus reportStatus, String reason,
                                           Instant clientAt, Instant now) {
        registerEvent(new ExperienceModerationDecidedEvent(UUID.randomUUID(), commandId, actionId,
                reportId, id, coffeeId, userId, operatorId, moderationStatus, reportStatus,
                reason, version, now, clientAt));
    }

    public Snapshot toSnapshot() {
        return new Snapshot(id, userId, coffeeId, message, publicationStatus, moderationStatus,
                createdAt, updatedAt, deletedAt, version);
    }

    private void requireOwner(UUID requesterId) {
        if (!userId.equals(requesterId)) throw new BusinessCommandRejectedException(
                "EXPERIENCE_FORBIDDEN", "Only the experience owner may change it");
    }
    private void requireNotDeleted() {
        if (publicationStatus == ExperiencePublicationStatus.DELETED) throw new BusinessCommandRejectedException(
                "EXPERIENCE_DELETED", "Deleted experience cannot be changed");
    }

    public record Snapshot(UUID experienceId, UUID userId, UUID coffeeId, String message,
                           ExperiencePublicationStatus publicationStatus,
                           ExperienceModerationStatus moderationStatus, Instant createdAt,
                           Instant updatedAt, Instant deletedAt, long version) { }
}
