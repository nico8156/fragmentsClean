package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.ContentReportJpaEntity;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringContentReportRepository extends JpaRepository<ContentReportJpaEntity, UUID> {
    Optional<ContentReportJpaEntity> findByReporterIdAndCommentId(UUID reporterId, UUID commentId);
    List<ContentReportJpaEntity> findByCommentIdAndStatus(UUID commentId, ReportStatus status);
}
