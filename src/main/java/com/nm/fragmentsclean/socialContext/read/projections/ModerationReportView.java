package com.nm.fragmentsclean.socialContext.read.projections;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ModerationReportView(UUID reportId, UUID commentId, UUID targetId, UUID authorId,
                                   String authorName, UUID reporterId, String reason, String details,
                                   String status, String content, long reportCount, Instant createdAt,
                                   List<ModerationActionView> actions) { }
