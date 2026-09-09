package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

/** Explicit operator or scheduler intent to analyse the current editorial signal batch. */
public record AnalyzeEditorialSignalsCommand(int limit) implements Command { }
