package com.nm.fragmentsclean.userContext.businesslogic.usecases;

import com.nm.fragmentsclean.authContext.write.businesslogic.models.events.UserAuthenticatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;
import com.nm.fragmentsclean.userContext.businesslogic.models.AppUser;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Transactional
public class OnUserAuthenticatedEventHandler implements CommandHandler<UserAuthenticatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OnUserAuthenticatedEventHandler.class);

    private final UserRepository userRepository;
    private final DateTimeProvider dateTimeProvider;

    public OnUserAuthenticatedEventHandler(UserRepository userRepository,
                                           DateTimeProvider dateTimeProvider) {
        this.userRepository = userRepository;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void execute(UserAuthenticatedEvent event) {
        var now = dateTimeProvider.now();
        var userId = event.userId();

        var maybeUser = userRepository.findById(userId);

        if (maybeUser.isEmpty()) {
            // Idempotence / robustesse : si le user n’existe pas (encore) dans le UserContext,
            // on log et on sort. Tu décideras plus tard si tu veux créer le user ici ou DLQ.
            log.warn(
                    "OnUserAuthenticatedEventHandler: user inexistant dans UserContext " +
                            "pour userId={}, provider={}, eventId={}",
                    event.userId(), event.provider(), event.eventId()
            );
            return;
        }

        AppUser user = maybeUser.get();

        // Pour l’instant, ta logique métier UserContext sur “l’utilisateur vient de s’authentifier”.
        // Tu as déjà markAuthenticated(Instant now) dans AppUser, donc on l’utilise.
        user.markAuthenticated(now);

        userRepository.save(user);

        log.info(
                "OnUserAuthenticatedEventHandler: userId={} marqué authentifié à {} (provider={}, eventId={})",
                user.id(), now, event.provider(), event.eventId()
        );
    }
}
