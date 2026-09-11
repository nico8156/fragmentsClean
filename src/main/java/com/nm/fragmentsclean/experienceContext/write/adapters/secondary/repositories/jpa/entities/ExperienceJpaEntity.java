package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name="experiences")
public class ExperienceJpaEntity {
    @Id private UUID experienceId;
    @Column(nullable=false) private UUID userId;
    @Column(nullable=false) private UUID coffeeId;
    @Column(length=4000) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ExperiencePublicationStatus publicationStatus;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ExperienceModerationStatus moderationStatus;
    @Column(nullable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    private Instant deletedAt;
    @Column(nullable=false) private long version;
    protected ExperienceJpaEntity() { }
    public ExperienceJpaEntity(UUID experienceId,UUID userId,UUID coffeeId,String message,
            ExperiencePublicationStatus publicationStatus,ExperienceModerationStatus moderationStatus,
            Instant createdAt,Instant updatedAt,Instant deletedAt,long version){this.experienceId=experienceId;this.userId=userId;this.coffeeId=coffeeId;this.message=message;this.publicationStatus=publicationStatus;this.moderationStatus=moderationStatus;this.createdAt=createdAt;this.updatedAt=updatedAt;this.deletedAt=deletedAt;this.version=version;}
    public UUID getExperienceId(){return experienceId;}public UUID getUserId(){return userId;}public UUID getCoffeeId(){return coffeeId;}public String getMessage(){return message;}public ExperiencePublicationStatus getPublicationStatus(){return publicationStatus;}public ExperienceModerationStatus getModerationStatus(){return moderationStatus;}public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}public Instant getDeletedAt(){return deletedAt;}public long getVersion(){return version;}
}
