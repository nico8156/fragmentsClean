package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;
import java.time.Instant;
import java.util.UUID;

public record WithdrawArticleCommand(UUID commandId, Instant clientAt, UUID articleId,
                                     UUID draftRevisionId) implements Command { }
