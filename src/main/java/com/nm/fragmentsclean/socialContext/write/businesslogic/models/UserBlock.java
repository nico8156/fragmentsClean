package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;
import java.time.Instant;
import java.util.UUID;

public final class UserBlock extends AggregateRoot {
    private final UUID blockerId;
    private final UUID blockedUserId;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    private UserBlock(UserBlockSnapshot snapshot) {
        super(snapshot.blockId());
        blockerId = snapshot.blockerId();
        blockedUserId = snapshot.blockedUserId();
        active = snapshot.active();
        createdAt = snapshot.createdAt();
        updatedAt = snapshot.updatedAt();
        version = snapshot.version();
    }

    public static UserBlock create(UUID id, UUID blockerId, UUID blockedUserId, Instant now) {
        if (blockerId.equals(blockedUserId)) throw new IllegalArgumentException("A user cannot block themselves");
        return new UserBlock(new UserBlockSnapshot(id, blockerId, blockedUserId, true, now, now, 0));
    }

    public static UserBlock fromSnapshot(UserBlockSnapshot snapshot) { return new UserBlock(snapshot); }

    public boolean setActive(boolean requested, Instant now) {
        if (active == requested) return false;
        active = requested;
        updatedAt = now;
        version++;
        return true;
    }

    public void registerChangedEvent(UUID commandId, Instant clientAt, Instant now) {
        registerEvent(new UserBlockChangedEvent(UUID.randomUUID(), commandId, id, blockerId,
                blockedUserId, active, version, now, clientAt));
    }

    public UserBlockSnapshot toSnapshot() {
        return new UserBlockSnapshot(id, blockerId, blockedUserId, active, createdAt, updatedAt, version);
    }

    public record UserBlockSnapshot(UUID blockId, UUID blockerId, UUID blockedUserId, boolean active,
                                    Instant createdAt, Instant updatedAt, long version) { }
}
