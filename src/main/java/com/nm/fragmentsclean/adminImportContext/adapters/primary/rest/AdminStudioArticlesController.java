package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleBlock;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCreationResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleDocument;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageAsset;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageRef;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleSubmission;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.StudioArticleDocumentRepository;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StoreStudioArticleImage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SubmitStudioArticle;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.RecordAdminAudit;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

@RestController
@RequestMapping("/api/admin/studio/articles")
public class AdminStudioArticlesController {
	private final SubmitStudioArticle submitStudioArticle;
	private final StoreStudioArticleImage storeStudioArticleImage;
	private final StudioArticleDocumentRepository studioArticleDocumentRepository;
	private final DateTimeProvider dateTimeProvider;
	private final ObjectMapper objectMapper;
	private final RecordAdminAudit recordAdminAudit;

	public AdminStudioArticlesController(
			SubmitStudioArticle submitStudioArticle,
			StoreStudioArticleImage storeStudioArticleImage,
			StudioArticleDocumentRepository studioArticleDocumentRepository,
			DateTimeProvider dateTimeProvider,
			ObjectMapper objectMapper,
			RecordAdminAudit recordAdminAudit) {
		this.submitStudioArticle = submitStudioArticle;
		this.storeStudioArticleImage = storeStudioArticleImage;
		this.studioArticleDocumentRepository = studioArticleDocumentRepository;
		this.dateTimeProvider = dateTimeProvider;
		this.objectMapper = objectMapper;
		this.recordAdminAudit = recordAdminAudit;
	}

	public AdminStudioArticlesController(
			SubmitStudioArticle submitStudioArticle,
			StoreStudioArticleImage storeStudioArticleImage,
			StudioArticleDocumentRepository studioArticleDocumentRepository,
			DateTimeProvider dateTimeProvider,
			ObjectMapper objectMapper) {
		this(submitStudioArticle, storeStudioArticleImage, studioArticleDocumentRepository, dateTimeProvider,
				objectMapper, new RecordAdminAudit(entry -> {}));
	}

	@GetMapping
	public AdminStudioArticleListResponse listArticles() {
		return new AdminStudioArticleListResponse(
				studioArticleDocumentRepository.list().stream()
						.map(AdminStudioArticleDocumentResponse::from)
						.toList());
	}

	@PutMapping("/{articleId}")
	public AdminStudioArticleDocumentResponse saveDraft(
			@PathVariable UUID articleId,
			@RequestBody AdminStudioArticleSubmitRequest body, Authentication authentication) {
		var document = saveDocument(articleId, "draft", body, null);
		audit(authentication, "ARTICLE_DRAFT_SAVED", articleId, null, "APPLIED");
		return AdminStudioArticleDocumentResponse.from(document);
	}

	public AdminStudioArticleDocumentResponse saveDraft(UUID articleId, AdminStudioArticleSubmitRequest body) {
		return AdminStudioArticleDocumentResponse.from(saveDocument(articleId, "draft", body, null));
	}

	@DeleteMapping("/{articleId}")
	public ResponseEntity<AdminStudioArticleDocumentResponse> deleteArticle(@PathVariable UUID articleId, Authentication authentication) {
		Instant now = dateTimeProvider.now();
		StudioArticleDocument existing = studioArticleDocumentRepository.findById(articleId)
				.orElseGet(() -> new StudioArticleDocument(articleId, "draft", "{}", now, now, null, null, null));
		StudioArticleDocument deleted = new StudioArticleDocument(
				articleId,
				"deleted",
				existing.payloadJson(),
				existing.createdAt(),
				now,
				existing.publishedAt(),
				now,
				existing.lastCommandId());
		studioArticleDocumentRepository.save(deleted);
		audit(authentication, "ARTICLE_DELETED", articleId, existing.lastCommandId(), "APPLIED");
		return ResponseEntity.accepted().body(AdminStudioArticleDocumentResponse.from(deleted));
	}

	public ResponseEntity<AdminStudioArticleDocumentResponse> deleteArticle(UUID articleId) {
		return deleteArticle(articleId, null);
	}

	@PostMapping
	public ResponseEntity<AdminStudioArticleSubmittedResponse> submitArticle(
			@RequestBody AdminStudioArticleSubmitRequest body, Authentication authentication) {
		StudioArticleCreationResult result = submitStudioArticle.execute(new StudioArticleSubmission(
				body.articleId(),
				body.slug(),
				body.locale(),
				body.authorId(),
				body.authorName(),
				body.title(),
				body.intro(),
				body.blocks() == null ? List.of() : body.blocks().stream().map(AdminStudioArticleBlockRequest::toDomain).toList(),
				body.conclusion(),
				body.cover() == null ? null : body.cover().toDomain(),
				body.tags() == null ? List.of() : body.tags(),
				body.readingTimeMin(),
				body.coffeeIds() == null ? List.of() : body.coffeeIds()
		));
		saveDocument(result.articleId(), "published", body.withArticleId(result.articleId()), result.commandId());
		audit(authentication, "ARTICLE_SUBMITTED", result.articleId(), result.commandId(), result.status());
		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(AdminStudioArticleSubmittedResponse.from(result));
	}

	public ResponseEntity<AdminStudioArticleSubmittedResponse> submitArticle(AdminStudioArticleSubmitRequest body) {
		return submitArticle(body, null);
	}

	@PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AdminStudioArticleImageResponse> uploadImage(
			@RequestPart("articleId") String rawArticleId,
			@RequestPart(value = "alt", required = false) String alt,
			@RequestPart("image") MultipartFile image, Authentication authentication) throws IOException {
		StudioArticleImageAsset asset = storeStudioArticleImage.execute(
				UUID.fromString(rawArticleId),
				image.getOriginalFilename(),
				image.getContentType(),
				image.getBytes(),
				alt);
		audit(authentication, "ARTICLE_IMAGE_UPLOADED", asset.assetId(), null, "APPLIED");
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(AdminStudioArticleImageResponse.from(asset));
	}

	public ResponseEntity<AdminStudioArticleImageResponse> uploadImage(String rawArticleId, String alt, MultipartFile image) throws IOException {
		return uploadImage(rawArticleId, alt, image, null);
	}

	private void audit(Authentication authentication, String action, UUID targetId, UUID commandId, String outcome) {
		if (authentication != null) {
			recordAdminAudit.execute(UUID.fromString(authentication.getName()), action, "ARTICLE", targetId,
					commandId, outcome, dateTimeProvider.now());
		}
	}

	public record AdminStudioArticleSubmitRequest(
			UUID articleId,
			String slug,
			String locale,
			UUID authorId,
			String authorName,
			String title,
			String intro,
			List<AdminStudioArticleBlockRequest> blocks,
			String conclusion,
			AdminStudioArticleImageRequest cover,
			List<String> tags,
			Integer readingTimeMin,
			List<UUID> coffeeIds) {
		AdminStudioArticleSubmitRequest withArticleId(UUID articleId) {
			return new AdminStudioArticleSubmitRequest(
					articleId,
					slug,
					locale,
					authorId,
					authorName,
					title,
					intro,
					blocks,
					conclusion,
					cover,
					tags,
					readingTimeMin,
					coffeeIds);
		}
	}

	public record AdminStudioArticleBlockRequest(
			String heading,
			String paragraph,
			AdminStudioArticleImageRequest photo) {
		StudioArticleBlock toDomain() {
			return new StudioArticleBlock(
					heading,
					paragraph,
					photo == null ? null : photo.toDomain());
		}
	}

	public record AdminStudioArticleImageRequest(
			UUID assetId,
			String url,
			String previewUrl,
			Integer width,
			Integer height,
			String alt) {
		StudioArticleImageRef toDomain() {
			return new StudioArticleImageRef(url, width, height, alt);
		}
	}

	public record AdminStudioArticleSubmittedResponse(
			UUID commandId,
			UUID articleId,
			String slug,
			String locale,
			String status) {
		static AdminStudioArticleSubmittedResponse from(StudioArticleCreationResult result) {
			return new AdminStudioArticleSubmittedResponse(
					result.commandId(),
					result.articleId(),
					result.slug(),
					result.locale(),
					result.status());
		}
	}

	public record AdminStudioArticleImageResponse(
			UUID assetId,
			String url,
			String previewUrl,
			Integer width,
			Integer height,
			String alt) {
		static AdminStudioArticleImageResponse from(StudioArticleImageAsset asset) {
			return new AdminStudioArticleImageResponse(
					asset.assetId(),
					asset.url(),
					asset.previewUrl(),
					asset.width(),
					asset.height(),
					asset.alt());
		}
	}

	public record AdminStudioArticleListResponse(
			List<AdminStudioArticleDocumentResponse> items) {
	}

	public record AdminStudioArticleDocumentResponse(
			UUID articleId,
			String status,
			AdminStudioArticleSubmitRequest draft,
			Instant createdAt,
			Instant updatedAt,
			Instant publishedAt,
			Instant deletedAt,
			UUID lastCommandId) {
		static AdminStudioArticleDocumentResponse from(StudioArticleDocument document) {
			try {
				return new AdminStudioArticleDocumentResponse(
						document.articleId(),
						document.status(),
						new ObjectMapper().readValue(document.payloadJson(), AdminStudioArticleSubmitRequest.class),
						document.createdAt(),
						document.updatedAt(),
						document.publishedAt(),
						document.deletedAt(),
						document.lastCommandId());
			} catch (IOException exception) {
				throw new IllegalStateException("Invalid Studio article payload", exception);
			}
		}
	}

	private StudioArticleDocument saveDocument(
			UUID articleId,
			String status,
			AdminStudioArticleSubmitRequest body,
			UUID commandId) {
		Instant now = dateTimeProvider.now();
		StudioArticleDocument existing = studioArticleDocumentRepository.findById(articleId).orElse(null);
		StudioArticleDocument document = new StudioArticleDocument(
				articleId,
				status,
				toJson(body.withArticleId(articleId)),
				existing == null ? now : existing.createdAt(),
				now,
				"published".equals(status) ? now : existing == null ? null : existing.publishedAt(),
				existing == null ? null : existing.deletedAt(),
				commandId == null && existing != null ? existing.lastCommandId() : commandId);
		studioArticleDocumentRepository.save(document);
		return document;
	}

	private String toJson(AdminStudioArticleSubmitRequest body) {
		try {
			return objectMapper.writeValueAsString(body);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Invalid Studio article draft", exception);
		}
	}
}
