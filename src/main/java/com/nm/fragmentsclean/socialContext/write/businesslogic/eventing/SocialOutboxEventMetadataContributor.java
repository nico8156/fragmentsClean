package com.nm.fragmentsclean.socialContext.write.businesslogic.eventing;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadata;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataContributor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SocialOutboxEventMetadataContributor implements OutboxEventMetadataContributor {
	@Override
	public Optional<OutboxEventMetadata> resolve(DomainEvent event) {
		if (event instanceof LikeSetEvent likeEvent) {
			return Optional.of(new OutboxEventMetadata(
					"Like",
					likeEvent.likeId().toString(),
					"user:" + likeEvent.userId()));
		}
		if (event instanceof CommentCreatedEvent commentEvent) {
			return Optional.of(comment(commentEvent.commentId().toString(), commentEvent.authorId().toString()));
		}
		if (event instanceof CommentUpdatedEvent commentEvent) {
			return Optional.of(comment(commentEvent.commentId().toString(), commentEvent.authorId().toString()));
		}
		if (event instanceof CommentDeletedEvent commentEvent) {
			return Optional.of(comment(commentEvent.commentId().toString(), commentEvent.authorId().toString()));
		}
		return Optional.empty();
	}

	private OutboxEventMetadata comment(String commentId, String authorId) {
		return new OutboxEventMetadata("Comment", commentId, "user:" + authorId);
	}
}
