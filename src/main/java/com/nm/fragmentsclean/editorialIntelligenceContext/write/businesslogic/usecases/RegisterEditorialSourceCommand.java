package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;
import java.time.Duration;
import java.util.UUID;

public record RegisterEditorialSourceCommand(UUID sourceId, String name, EditorialSourceAccessMode accessMode,
        EditorialAuthorityLevel authorityLevel, String endpoint, Duration pollingFrequency) implements Command { }
