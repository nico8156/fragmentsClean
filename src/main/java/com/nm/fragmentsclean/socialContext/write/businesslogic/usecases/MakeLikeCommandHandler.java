package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import jakarta.transaction.Transactional;

import java.util.UUID;


@Transactional
public class MakeLikeCommandHandler implements CommandHandler<MakeLikeCommand> {

    private final LikeRepository likeRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;
    private final CommandStatusRecorder commandStatusRecorder;

    public MakeLikeCommandHandler(LikeRepository likeRepository,
                                  DomainEventPublisher eventPublisher,
                                  DateTimeProvider dateTimeProvider,
                                  CommandStatusRecorder commandStatusRecorder) {
        this.likeRepository = likeRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
        this.commandStatusRecorder = commandStatusRecorder;
    }

    @Override
    public void execute(MakeLikeCommand cmd) {

        var now = dateTimeProvider.now();
        UUID commandId = UUID.fromString(cmd.commandId());

        if (commandStatusRecorder.isApplied(commandId)) {
            return;
        }

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
        like.applyState(cmd.value(), now);

        // on persiste l'état du like
        likeRepository.save(like);

        // compute count après persistance
        long count = likeRepository.countByTargetId(cmd.targetId());

        like.registerLikeSetEvent(
                cmd.commandId(),
                cmd.clientAt(),
                count,
                now
        );

        // publication outbox
        like.domainEvents().forEach(eventPublisher::publish);
        like.clearDomainEvents();

        commandStatusRecorder.markApplied(
                commandId,
                "Like",
                cmd.likeId().toString(),
                "social.like.set",
                now
        );
    }
}
