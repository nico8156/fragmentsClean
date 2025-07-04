package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Command;

public record CreateArticleCommand (String title) implements Command {
}
