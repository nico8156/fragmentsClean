package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity(name="experience_reports")
public class ExperienceReportJpaEntity {
    @Id private UUID reportId;@Column(nullable=false)private UUID experienceId;@Column(nullable=false)private UUID coffeeId;@Column(nullable=false)private UUID authorId;@Column(nullable=false)private UUID reporterId;
    @Enumerated(EnumType.STRING)@Column(nullable=false)private ExperienceReportReason reason;@Column(length=1000)private String details;
    @Enumerated(EnumType.STRING)@Column(nullable=false)private ExperienceReportStatus status;@Column(nullable=false)private Instant createdAt;private Instant resolvedAt;@Column(nullable=false)private long version;
    protected ExperienceReportJpaEntity(){}
    public ExperienceReportJpaEntity(UUID reportId,UUID experienceId,UUID coffeeId,UUID authorId,UUID reporterId,ExperienceReportReason reason,String details,ExperienceReportStatus status,Instant createdAt,Instant resolvedAt,long version){this.reportId=reportId;this.experienceId=experienceId;this.coffeeId=coffeeId;this.authorId=authorId;this.reporterId=reporterId;this.reason=reason;this.details=details;this.status=status;this.createdAt=createdAt;this.resolvedAt=resolvedAt;this.version=version;}
    public UUID getReportId(){return reportId;}public UUID getExperienceId(){return experienceId;}public UUID getCoffeeId(){return coffeeId;}public UUID getAuthorId(){return authorId;}public UUID getReporterId(){return reporterId;}public ExperienceReportReason getReason(){return reason;}public String getDetails(){return details;}public ExperienceReportStatus getStatus(){return status;}public Instant getCreatedAt(){return createdAt;}public Instant getResolvedAt(){return resolvedAt;}public long getVersion(){return version;}
}
