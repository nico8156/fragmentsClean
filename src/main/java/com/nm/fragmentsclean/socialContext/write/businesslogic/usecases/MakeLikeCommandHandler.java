package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import jakarta.transaction.Transactional;


@Transactional
public class MakeLikeCommandHandler implements CommandHandler<MakeLikeCommand> {

    private final LikeRepository likeRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;

    public MakeLikeCommandHandler(LikeRepository likeRepository,
                                  DomainEventPublisher eventPublisher,
                                  DateTimeProvider dateTimeProvider) {
        this.likeRepository = likeRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void execute(MakeLikeCommand cmd) {

        var now = dateTimeProvider.now();

        Like like = likeRepository.byId(cmd.likeId())
                .orElseGet(() -> Like.createNew(
                        cmd.likeId(),
                        cmd.userId(),
                        cmd.targetId(),
                        now
                ));

        // Sécurité : vérifie cohérence likeId→(userId,targetId)
        var snap = like.toSnapshot();
        if (!snap.userId().equals(cmd.userId())
                || !snap.targetId().equals(cmd.targetId())) {
            throw new IllegalStateException("LikeId mismatch with user/target");
        }

        // mutation locale
        boolean changed = like.applyState(cmd.value(), now);

        // on persiste l'état du like
        likeRepository.save(like);

        // compute count après persistance
        long count = likeRepository.countByTargetId(cmd.targetId());

        // si changement : publier event riche
        if (changed) {
            like.registerLikeSetEvent(
                    cmd.commandId(),
                    cmd.clientAt(),
                    count,
                    now
            );
        }

        // publication outbox
        like.domainEvents().forEach(eventPublisher::publish);
        like.clearDomainEvents();
    }
}
