package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleCreatedEventHandler;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeArchivedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeCreatedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeDeletedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeOpeningHoursImportedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoAddedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoDeletedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotosImportedEventHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ImportGoogleOpeningHoursForCoffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ImportGooglePhotosForCoffee;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.InboxMessageRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.socialContext.read.projectors.UserSocialProjectionProjector;
import com.nm.fragmentsclean.socialContext.read.projections.CommentCreatedEventHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentDeletedEventHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentUpdatedEventHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerificationCompletedEventHandler;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketVerifyAcceptedEventHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.ProcessTicketVerificationEventHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserProfileUpdatedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.AuthUserCreatedEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.*;

@Component
public class SqsIntegrationEventRouter implements SqsIntegrationEventRouting {

    private static final Logger log = LoggerFactory.getLogger(SqsIntegrationEventRouter.class);

    private final ObjectMapper objectMapper;
    private final InboxMessageRepository inbox;
    private final ArticleCreatedEventHandler articleCreatedHandler;
    private final CoffeeArchivedEventHandler coffeeArchivedHandler;
    private final CoffeeCreatedEventHandler coffeeCreatedHandler;
    private final CoffeeDeletedEventHandler coffeeDeletedHandler;
    private final ImportGoogleOpeningHoursForCoffee importGoogleOpeningHoursForCoffee;
    private final ImportGooglePhotosForCoffee importGooglePhotosForCoffee;
    private final CoffeeOpeningHoursImportedEventHandler coffeeOpeningHoursImportedHandler;
    private final CoffeePhotosImportedEventHandler coffeePhotosImportedHandler;
    private final CoffeePhotoAddedEventHandler coffeePhotoAddedHandler;
    private final CoffeePhotoDeletedEventHandler coffeePhotoDeletedHandler;
    private final AuthUserCreatedEventHandler authUserCreatedHandler;
    private final CommentCreatedEventHandler commentCreatedHandler;
    private final CommentUpdatedEventHandler commentUpdatedHandler;
    private final CommentDeletedEventHandler commentDeletedHandler;
    private final TicketVerifyAcceptedEventHandler ticketAcceptedReadHandler;
    private final TicketVerificationCompletedEventHandler ticketCompletedReadHandler;
    private final ProcessTicketVerificationEventHandler processTicketVerificationHandler;
    private final UserSocialProjectionProjector userSocialProjectionProjector;

    public SqsIntegrationEventRouter(
            ObjectMapper objectMapper,
            InboxMessageRepository inbox,
            ArticleCreatedEventHandler articleCreatedHandler,
            CoffeeArchivedEventHandler coffeeArchivedHandler,
            CoffeeCreatedEventHandler coffeeCreatedHandler,
            CoffeeDeletedEventHandler coffeeDeletedHandler,
            ImportGoogleOpeningHoursForCoffee importGoogleOpeningHoursForCoffee,
            ImportGooglePhotosForCoffee importGooglePhotosForCoffee,
            CoffeeOpeningHoursImportedEventHandler coffeeOpeningHoursImportedHandler,
            CoffeePhotosImportedEventHandler coffeePhotosImportedHandler,
            CoffeePhotoAddedEventHandler coffeePhotoAddedHandler,
            CoffeePhotoDeletedEventHandler coffeePhotoDeletedHandler,
            AuthUserCreatedEventHandler authUserCreatedHandler,
            CommentCreatedEventHandler commentCreatedHandler,
            CommentUpdatedEventHandler commentUpdatedHandler,
            CommentDeletedEventHandler commentDeletedHandler,
            TicketVerifyAcceptedEventHandler ticketAcceptedReadHandler,
            TicketVerificationCompletedEventHandler ticketCompletedReadHandler,
            ProcessTicketVerificationEventHandler processTicketVerificationHandler,
            UserSocialProjectionProjector userSocialProjectionProjector
    ) {
        this.objectMapper = objectMapper;
        this.inbox = inbox;
        this.articleCreatedHandler = articleCreatedHandler;
        this.coffeeArchivedHandler = coffeeArchivedHandler;
        this.coffeeCreatedHandler = coffeeCreatedHandler;
        this.coffeeDeletedHandler = coffeeDeletedHandler;
        this.importGoogleOpeningHoursForCoffee = importGoogleOpeningHoursForCoffee;
        this.importGooglePhotosForCoffee = importGooglePhotosForCoffee;
        this.coffeeOpeningHoursImportedHandler = coffeeOpeningHoursImportedHandler;
        this.coffeePhotosImportedHandler = coffeePhotosImportedHandler;
        this.coffeePhotoAddedHandler = coffeePhotoAddedHandler;
        this.coffeePhotoDeletedHandler = coffeePhotoDeletedHandler;
        this.authUserCreatedHandler = authUserCreatedHandler;
        this.commentCreatedHandler = commentCreatedHandler;
        this.commentUpdatedHandler = commentUpdatedHandler;
        this.commentDeletedHandler = commentDeletedHandler;
        this.ticketAcceptedReadHandler = ticketAcceptedReadHandler;
        this.ticketCompletedReadHandler = ticketCompletedReadHandler;
        this.processTicketVerificationHandler = processTicketVerificationHandler;
        this.userSocialProjectionProjector = userSocialProjectionProjector;
    }

    @Override
    public void route(IntegrationEventEnvelope envelope) {
        if (!inbox.claim(envelope)) {
            log.info("[sqs] duplicate suppressed eventId={} destination={}",
                    envelope.eventId(), envelope.destination());
            return;
        }

        try {
            dispatch(envelope);
            inbox.markProcessed(envelope);
        } catch (Exception e) {
            inbox.markFailed(envelope, e);
            throw e;
        }
    }

    private void dispatch(IntegrationEventEnvelope envelope) {
        String destination = envelope.destination();
        String type = envelope.eventType();

        if (ARTICLES_EVENTS.equals(destination) && "article.created".equals(type)) {
            articleCreatedHandler.handle(read(envelope, ArticleCreatedEvent.class));
            return;
        }
        if (COFFEES_EVENTS.equals(destination) && "coffee.created".equals(type)) {
            CoffeeCreatedEvent event = read(envelope, CoffeeCreatedEvent.class);
            coffeeCreatedHandler.handle(event);
            importGoogleOpeningHoursForCoffee.handle(event);
            importGooglePhotosForCoffee.handle(event);
            return;
        }
        if (COFFEES_EVENTS.equals(destination) && "coffee.archived".equals(type)) {
            coffeeArchivedHandler.handle(read(envelope, CoffeeArchivedEvent.class));
            return;
        }
        if (COFFEES_EVENTS.equals(destination) && "coffee.deleted".equals(type)) {
            coffeeDeletedHandler.handle(read(envelope, CoffeeDeletedEvent.class));
            return;
        }
        if (COFFEES_EVENTS.equals(destination) && "coffee.opening_hours_imported".equals(type)) {
            coffeeOpeningHoursImportedHandler.handle(read(envelope, CoffeeOpeningHoursImportedEvent.class));
            return;
        }
        if (COFFEES_EVENTS.equals(destination) && "coffee.photos_imported".equals(type)) {
            coffeePhotosImportedHandler.handle(read(envelope, CoffeePhotosImportedEvent.class));
            return;
        }
        if (COFFEES_EVENTS.equals(destination) && "coffee.photo_added".equals(type)) {
            coffeePhotoAddedHandler.handle(read(envelope, CoffeePhotoAddedEvent.class));
            return;
        }
        if (COFFEES_EVENTS.equals(destination) && "coffee.photo_deleted".equals(type)) {
            coffeePhotoDeletedHandler.handle(read(envelope, CoffeePhotoDeletedEvent.class));
            return;
        }
        if (AUTH_USERS_EVENTS.equals(destination) && "auth.user.created".equals(type)) {
            authUserCreatedHandler.handle(read(envelope, AuthUserCreatedEvent.class));
            return;
        }
        if (APP_USERS_EVENTS.equals(destination)) {
            dispatchAppUserEvent(envelope, type);
            return;
        }
        if (DOMAIN_EVENTS.equals(destination)) {
            dispatchDomainEvent(envelope, type);
            return;
        }
        if (TICKET_EVENTS.equals(destination)) {
            dispatchTicketReadEvent(envelope, type);
            return;
        }
        if (TICKET_VERIFICATION_REQUESTED.equals(destination) && "ticket.verify.accepted".equals(type)) {
            processTicketVerificationHandler.handle(read(envelope, TicketVerifyAcceptedEvent.class));
            return;
        }

        log.debug("[sqs] ignored eventId={} type={} destination={}",
                envelope.eventId(), type, destination);
    }

    private void dispatchDomainEvent(IntegrationEventEnvelope envelope, String type) {
        switch (type) {
            case "social.comment.created" -> commentCreatedHandler.handle(read(envelope, CommentCreatedEvent.class));
            case "social.comment.updated" -> commentUpdatedHandler.handle(read(envelope, CommentUpdatedEvent.class));
            case "social.comment.deleted" -> commentDeletedHandler.handle(read(envelope, CommentDeletedEvent.class));
            default -> log.debug("[sqs] ignored domain event type={}", type);
        }
    }

    private void dispatchAppUserEvent(IntegrationEventEnvelope envelope, String type) {
        switch (type) {
            case "app.user.created" -> {
                AppUserCreatedEvent event = read(envelope, AppUserCreatedEvent.class);
                userSocialProjectionProjector.upsert(
                        event.userId(), event.displayName(), event.avatarUrl(), event.version(), event.occurredAt());
            }
            case "app.user.profile_updated" -> {
                AppUserProfileUpdatedEvent event = read(envelope, AppUserProfileUpdatedEvent.class);
                userSocialProjectionProjector.upsert(
                        event.userId(), event.displayName(), event.avatarUrl(), event.version(), event.occurredAt());
            }
            default -> log.debug("[sqs] ignored app user event type={}", type);
        }
    }

    private void dispatchTicketReadEvent(IntegrationEventEnvelope envelope, String type) {
        switch (type) {
            case "ticket.verify.accepted" -> ticketAcceptedReadHandler.handle(read(envelope, TicketVerifyAcceptedEvent.class));
            case "ticket.verification.completed" -> ticketCompletedReadHandler.handle(read(envelope, TicketVerificationCompletedEvent.class));
            default -> log.debug("[sqs] ignored ticket event type={}", type);
        }
    }

    private <T> T read(IntegrationEventEnvelope envelope, Class<T> eventClass) {
        try {
            return objectMapper.readValue(envelope.payloadJson(), eventClass);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize " + envelope.eventType(), e);
        }
    }
}
