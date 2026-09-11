package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake;

import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.UserBlockRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.UserBlock;
import java.util.*;

public final class FakeUserBlockRepository implements UserBlockRepository {
    private final Map<UUID, UserBlock.UserBlockSnapshot> blocks = new HashMap<>();
    @Override public Optional<UserBlock> byUsers(UUID blockerId, UUID blockedUserId) {
        return blocks.values().stream().filter(s -> s.blockerId().equals(blockerId) && s.blockedUserId().equals(blockedUserId))
                .findFirst().map(UserBlock::fromSnapshot);
    }
    @Override public void save(UserBlock block) { blocks.put(block.toSnapshot().blockId(), block.toSnapshot()); }
    public List<UserBlock.UserBlockSnapshot> allSnapshots() { return List.copyOf(blocks.values()); }
}
