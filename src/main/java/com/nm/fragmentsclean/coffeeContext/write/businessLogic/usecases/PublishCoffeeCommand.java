package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;
import java.time.Instant;
import java.util.UUID;

public record PublishCoffeeCommand(UUID commandId, UUID coffeeId, Instant clientAt) implements Command { }
