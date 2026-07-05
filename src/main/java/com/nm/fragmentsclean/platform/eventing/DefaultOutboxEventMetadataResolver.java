package com.nm.fragmentsclean.platform.eventing;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserLoggedInEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadata;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataResolver;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultOutboxEventMetadataResolver implements OutboxEventMetadataResolver {
	private static final Logger log = LoggerFactory.getLogger(DefaultOutboxEventMetadataResolver.class);

	@Override
	public OutboxEventMetadata resolve(DomainEvent event) {
		if (event instanceof ArticleCreatedEvent articleEvent) {
			return aggregate("Article", articleEvent.articleId().toString(), "article");
		}
		if (event instanceof TicketVerifyAcceptedEvent ticketEvent) {
			return new OutboxEventMetadata("Ticket", ticketEvent.ticketId().toString(), "user:" + ticketEvent.userId());
		}
		if (event instanceof TicketVerificationCompletedEvent ticketEvent) {
			return new OutboxEventMetadata("Ticket", ticketEvent.ticketId().toString(), "user:" + ticketEvent.userId());
		}
		if (event instanceof CoffeeCreatedEvent coffeeEvent) {
			return aggregate("Coffee", coffeeEvent.coffeeId().toString(), "coffee");
		}
		if (event instanceof CoffeeArchivedEvent coffeeEvent) {
			return aggregate("Coffee", coffeeEvent.coffeeId().toString(), "coffee");
		}
		if (event instanceof CoffeeDeletedEvent coffeeEvent) {
			return aggregate("Coffee", coffeeEvent.coffeeId().toString(), "coffee");
		}
		if (event instanceof CoffeeOpeningHoursImportedEvent coffeeEvent) {
			return aggregate("Coffee", coffeeEvent.coffeeId().toString(), "coffee");
		}
		if (event instanceof CoffeePhotosImportedEvent coffeeEvent) {
			return aggregate("Coffee", coffeeEvent.coffeeId().toString(), "coffee");
		}
		if (event instanceof CoffeePhotoAddedEvent coffeeEvent) {
			return aggregate("Coffee", coffeeEvent.coffeeId().toString(), "coffee");
		}
		if (event instanceof CoffeePhotoDeletedEvent coffeeEvent) {
			return aggregate("Coffee", coffeeEvent.coffeeId().toString(), "coffee");
		}
		if (event instanceof AuthUserCreatedEvent authEvent) {
			return aggregate("AuthUser", authEvent.authUserId().toString(), "authUser");
		}
		if (event instanceof AuthUserLoggedInEvent authEvent) {
			return aggregate("AuthUser", authEvent.authUserId().toString(), "authUser");
		}
		if (event instanceof AppUserCreatedEvent appEvent) {
			return aggregate("AppUser", appEvent.userId().toString(), "appUser");
		}
		if (event instanceof AppUserProfileUpdatedEvent appEvent) {
			return aggregate("AppUser", appEvent.userId().toString(), "appUser");
		}
		if (event instanceof LikeSetEvent likeEvent) {
			return new OutboxEventMetadata("Like", likeEvent.likeId().toString(), "user:" + likeEvent.userId());
		}
		if (event instanceof CommentCreatedEvent commentEvent) {
			return new OutboxEventMetadata("Comment", commentEvent.commentId().toString(), "user:" + commentEvent.authorId());
		}
		if (event instanceof CommentUpdatedEvent commentEvent) {
			return new OutboxEventMetadata("Comment", commentEvent.commentId().toString(), "user:" + commentEvent.authorId());
		}
		if (event instanceof CommentDeletedEvent commentEvent) {
			return new OutboxEventMetadata("Comment", commentEvent.commentId().toString(), "user:" + commentEvent.authorId());
		}

		log.warn("Persisting domain event of unknown type in outbox type={}", event.getClass().getName());
		return new OutboxEventMetadata("Unknown", "unknown", "global");
	}

	private OutboxEventMetadata aggregate(String aggregateType, String aggregateId, String streamPrefix) {
		return new OutboxEventMetadata(aggregateType, aggregateId, streamPrefix + ":" + aggregateId);
	}
}
