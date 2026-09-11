package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.UserBlockRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.UserBlock;
import jakarta.transaction.Transactional;

@Transactional
public class SetUserBlockCommandHandler implements CommandHandler<SetUserBlockCommand> {
    private final UserBlockRepository blocks;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;

    public SetUserBlockCommandHandler(UserBlockRepository blocks, DomainEventPublisher events, DateTimeProvider clock) {
        this.blocks = blocks; this.events = events; this.clock = clock;
    }

    @Override public void execute(SetUserBlockCommand command) {
        if (command.blockerId().equals(command.blockedUserId())) {
            throw new BusinessCommandRejectedException("USER_SELF_BLOCK", "A user cannot block themselves");
        }
        var now = clock.now();
        var found = blocks.byUsers(command.blockerId(), command.blockedUserId());
        UserBlock block;
        if (found.isEmpty()) {
            if (!command.active()) return;
            block = UserBlock.create(command.blockId(), command.blockerId(), command.blockedUserId(), now);
        } else {
            block = found.get();
            if (!block.toSnapshot().blockId().equals(command.blockId())) {
                throw new BusinessCommandRejectedException("USER_BLOCK_ID_CONFLICT", "Block id conflicts with an existing relation");
            }
            if (!block.setActive(command.active(), now)) return;
        }
        blocks.save(block);
        block.registerChangedEvent(command.commandId(), command.clientAt(), now);
        block.domainEvents().forEach(events::publish);
        block.clearDomainEvents();
    }
}
