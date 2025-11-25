package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;
import jakarta.transaction.Transactional;

@Transactional
public class MakeLikeCommandHandler implements CommandHandler<MakeLikeCommand> {

    private final LikeRepository likeRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final DateTimeProvider dateTimeProvider;

    public MakeLikeCommandHandler(LikeRepository likeRepository,
                                  DomainEventPublisher domainEventPublisher,
                                  DateTimeProvider dateTimeProvider) {
        this.likeRepository = likeRepository;
        this.domainEventPublisher = domainEventPublisher;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void execute(MakeLikeCommand command) {
        var now = dateTimeProvider.now();

        // 1. On récupère l’agrégat par son ID envoyé par le front
        var like = likeRepository.byId(command.likeId())
                .orElseGet(() -> Like.createNew(
                        command.likeId(),
                        command.userId(),
                        command.targetId(),
                        now
                ));

        // 2. Optionnel : cohérence (l’ID ne doit pas changer de user/target)
        if (!like.toSnapshot().userId().equals(command.userId())
                || !like.toSnapshot().targetId().equals(command.targetId())) {
            throw new IllegalStateException("LikeId incohérent avec userId/targetId");
        }

        // 3. On applique la commande à l’agrégat (c’est lui qui émet l’event)
        like.set(command.value(), command.commandId(), now);

        // 4. On persiste l’agrégat
        likeRepository.save(like);

        // 5. On publie les événements
        like.domainEvents().forEach(domainEventPublisher::publish);
        like.clearDomainEvents();
    }
}
