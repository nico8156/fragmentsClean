package com.nm.fragmentsclean.aticleContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommand;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/articles")
public class WriteArticleController {

    private final CommandBus commandBus;

    public WriteArticleController(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    public record CreateArticleRequestDto(
            String commandId,
            String articleId,

            String authorId,
            String authorName,

            String slug,
            String locale,

            String title,
            String intro,
            String blocksJson,
            String conclusion,

            String coverUrl,
            Integer coverWidth,
            Integer coverHeight,
            String coverAlt,

            List<String> tags,
            Integer readingTimeMin,
            List<String> coffeeIds,

            String at // clientAt (ISO-8601)
    ) {
    }

    @PostMapping
    public ResponseEntity<Void> createArticle(@RequestBody CreateArticleRequestDto body) {

        UUID commandId = UUID.fromString(body.commandId());
        UUID articleId = UUID.fromString(body.articleId());
        UUID authorId = UUID.fromString(body.authorId());
        String authorName = body.authorName();

        String slug = body.slug();
        String locale = body.locale();

        String title = body.title();
        String intro = body.intro();
        String blocksJson = body.blocksJson();
        String conclusion = body.conclusion();

        String coverUrl = body.coverUrl();
        Integer coverWidth = body.coverWidth();
        Integer coverHeight = body.coverHeight();
        String coverAlt = body.coverAlt();

        List<String> tags = body.tags() != null ? body.tags() : Collections.emptyList();
        Integer readingTimeMin = body.readingTimeMin();

        List<UUID> coffeeIds = body.coffeeIds() != null
                ? body.coffeeIds().stream().map(UUID::fromString).toList()
                : Collections.emptyList();

        Instant clientAt = Instant.parse(body.at());

        var command = new CreateArticleCommand(
                commandId,
                clientAt,
                articleId,
                slug,
                locale,
                authorId,
                authorName,
                title,
                intro,
                blocksJson,
                conclusion,
                coverUrl,
                coverWidth,
                coverHeight,
                coverAlt,
                tags,
                readingTimeMin,
                coffeeIds
        );

        commandBus.dispatch(command);

        // Même pattern que WriteCommentController : 202 Accepted
        return ResponseEntity.accepted().build();
    }
}
