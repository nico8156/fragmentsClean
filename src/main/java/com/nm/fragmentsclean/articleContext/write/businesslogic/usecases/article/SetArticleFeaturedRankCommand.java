package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;
import java.time.Instant;
import java.util.UUID;

public record SetArticleFeaturedRankCommand(UUID commandId, Instant clientAt, UUID articleId,
                                            Integer featuredRank) implements Command { }
