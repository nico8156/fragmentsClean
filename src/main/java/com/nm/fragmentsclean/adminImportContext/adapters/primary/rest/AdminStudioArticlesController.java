package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.*;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.StudioArticleDraftCatalog;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/studio/articles")
public final class AdminStudioArticlesController {
    private final SubmitStudioArticle submit;
    private final SaveStudioArticleDraft save;
    private final ArchiveStudioArticle archive;
    private final StoreStudioArticleImage storeImage;
    private final StudioArticleDraftCatalog catalog;
    private final RecordAdminAudit audit;
    private final DateTimeProvider clock;

    @Autowired
    public AdminStudioArticlesController(SubmitStudioArticle submit, SaveStudioArticleDraft save,
                                         ArchiveStudioArticle archive, StoreStudioArticleImage storeImage,
                                         StudioArticleDraftCatalog catalog, RecordAdminAudit audit,
                                         DateTimeProvider clock) {
        this.submit = submit; this.save = save; this.archive = archive;
        this.storeImage = storeImage; this.catalog = catalog; this.audit = audit;
        this.clock = clock;
    }

    @GetMapping
    public ArticleListResponse listArticles() {
        return new ArticleListResponse(catalog.list().stream().map(ArticleDocumentResponse::from).toList());
    }

    @PutMapping("/{articleId}")
    public ResponseEntity<ArticleDocumentResponse> saveDraft(@PathVariable UUID articleId,
                                                              @RequestBody ArticleDraftRequest body,
                                                              Authentication authentication) {
        if (body.articleId() != null && !articleId.equals(body.articleId())) {
            throw new IllegalArgumentException("Article path and payload identifiers differ");
        }
        var result = save.execute(body.withArticleId(articleId).toSubmission());
        var document = catalog.byId(articleId).orElseThrow();
        record(authentication, "ARTICLE_DRAFT_SAVED", articleId, result.commandId(), "APPLIED");
        return ResponseEntity.accepted().body(ArticleDocumentResponse.from(document, result.commandId()));
    }

    @PostMapping
    public ResponseEntity<ArticleSubmittedResponse> submitArticle(@RequestBody ArticleDraftRequest body,
                                                                   Authentication authentication) {
        var result = submit.execute(body.toSubmission());
        record(authentication, "ARTICLE_SUBMITTED", result.articleId(), result.commandId(), result.status());
        return ResponseEntity.accepted().body(ArticleSubmittedResponse.from(result));
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<CommandAccepted> archiveArticle(@PathVariable UUID articleId,
                                                           Authentication authentication) {
        UUID commandId = archive.execute(articleId);
        record(authentication, "ARTICLE_ARCHIVED", articleId, commandId, "APPLIED");
        return ResponseEntity.accepted().body(new CommandAccepted(commandId, articleId, "PENDING"));
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArticleImageResponse> uploadImage(@RequestPart("articleId") String rawArticleId,
                                                             @RequestPart(value = "alt", required = false) String alt,
                                                             @RequestPart("image") MultipartFile image,
                                                             Authentication authentication) throws IOException {
        var asset = storeImage.execute(UUID.fromString(rawArticleId), image.getOriginalFilename(),
                image.getContentType(), image.getBytes(), alt);
        record(authentication, "ARTICLE_IMAGE_UPLOADED", asset.assetId(), null, "APPLIED");
        return ResponseEntity.status(HttpStatus.CREATED).body(ArticleImageResponse.from(asset));
    }

    private void record(Authentication authentication, String action, UUID targetId,
                        UUID commandId, String outcome) {
        if (authentication != null) {
            audit.execute(UUID.fromString(authentication.getName()), action, "ARTICLE", targetId,
                    commandId, outcome, clock.now());
        }
    }

    public record ArticleDraftRequest(UUID articleId, UUID revisionId, String slug, String locale,
                                      UUID authorId, String authorName, String title, String intro,
                                      List<BlockRequest> blocks, String conclusion, ImageRequest cover,
                                      List<String> tags, Integer readingTimeMin, List<UUID> coffeeIds) {
        ArticleDraftRequest withArticleId(UUID id) {
            return new ArticleDraftRequest(id, revisionId, slug, locale, authorId, authorName, title,
                    intro, blocks, conclusion, cover, tags, readingTimeMin, coffeeIds);
        }
        StudioArticleSubmission toSubmission() {
            return new StudioArticleSubmission(articleId, revisionId, slug, locale, authorId, authorName,
                    title, intro, blocks == null ? List.of() : blocks.stream().map(BlockRequest::toModel).toList(),
                    conclusion, cover == null ? null : cover.toModel(), tags == null ? List.of() : tags,
                    readingTimeMin, coffeeIds == null ? List.of() : coffeeIds);
        }
        static ArticleDraftRequest from(StudioArticleSubmission source) {
            return new ArticleDraftRequest(source.articleId(), source.revisionId(), source.slug(), source.locale(),
                    source.authorId(), source.authorName(), source.title(), source.intro(),
                    source.blocks().stream().map(BlockRequest::from).toList(), source.conclusion(),
                    ImageRequest.from(source.cover()), source.tags(), source.readingTimeMin(), source.coffeeIds());
        }
    }

    public record BlockRequest(String heading, String paragraph, ImageRequest photo) {
        StudioArticleBlock toModel() {
            return new StudioArticleBlock(heading, paragraph, photo == null ? null : photo.toModel());
        }
        static BlockRequest from(StudioArticleBlock source) {
            return new BlockRequest(source.heading(), source.paragraph(), ImageRequest.from(source.photo()));
        }
    }

    public record ImageRequest(UUID assetId, String url, String previewUrl,
                               Integer width, Integer height, String alt) {
        StudioArticleImageRef toModel() { return new StudioArticleImageRef(url, previewUrl, width, height, alt); }
        static ImageRequest from(StudioArticleImageRef source) {
            return source == null ? null : new ImageRequest(
                    null, source.url(), source.previewUrl(), source.width(), source.height(), source.alt());
        }
    }

    public record ArticleDocumentResponse(UUID articleId, UUID revisionId, String status,
                                          ArticleDraftRequest draft, Instant createdAt, Instant updatedAt,
                                          Instant publishedAt, UUID lastCommandId) {
        static ArticleDocumentResponse from(StudioArticleDraftDocument source) { return from(source, null); }
        static ArticleDocumentResponse from(StudioArticleDraftDocument source, UUID commandId) {
            return new ArticleDocumentResponse(source.articleId(), source.revisionId(), source.status(),
                    ArticleDraftRequest.from(source.draft()), source.createdAt(), source.updatedAt(),
                    source.publishedAt(), commandId);
        }
    }

    public record ArticleListResponse(List<ArticleDocumentResponse> items) { }
    public record CommandAccepted(UUID commandId, UUID articleId, String status) { }
    public record ArticleSubmittedResponse(UUID commandId, UUID articleId, String slug,
                                           String locale, String status) {
        static ArticleSubmittedResponse from(StudioArticleCreationResult source) {
            return new ArticleSubmittedResponse(source.commandId(), source.articleId(), source.slug(),
                    source.locale(), source.status());
        }
    }
    public record ArticleImageResponse(UUID assetId, String url, String previewUrl,
                                       Integer width, Integer height, String alt) {
        static ArticleImageResponse from(StudioArticleImageAsset source) {
            return new ArticleImageResponse(source.assetId(), source.url(), source.previewUrl(),
                    source.width(), source.height(), source.alt());
        }
    }
}
