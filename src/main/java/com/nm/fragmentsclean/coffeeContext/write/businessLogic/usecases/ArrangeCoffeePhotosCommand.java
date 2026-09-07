package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

public record ArrangeCoffeePhotosCommand(UUID commandId, UUID coffeeId, List<UUID> orderedPhotoIds,
        UUID coverPhotoId, Instant clientAt) implements Command {
    public ArrangeCoffeePhotosCommand { orderedPhotoIds = orderedPhotoIds == null ? List.of() : List.copyOf(orderedPhotoIds); }
}
