package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Command;

public record CreateArticleCommand (String title) implements Command {
}
