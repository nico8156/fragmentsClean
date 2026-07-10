package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleBlock;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCreationResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageAsset;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageRef;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleSubmission;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StoreStudioArticleImage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SubmitStudioArticle;

@RestController
@RequestMapping("/api/admin/studio/articles")
public class AdminStudioArticlesController {
	private final SubmitStudioArticle submitStudioArticle;
	private final StoreStudioArticleImage storeStudioArticleImage;

	public AdminStudioArticlesController(
			SubmitStudioArticle submitStudioArticle,
			StoreStudioArticleImage storeStudioArticleImage) {
		this.submitStudioArticle = submitStudioArticle;
		this.storeStudioArticleImage = storeStudioArticleImage;
	}

	@PostMapping
	public ResponseEntity<AdminStudioArticleSubmittedResponse> submitArticle(
			@RequestBody AdminStudioArticleSubmitRequest body) {
		StudioArticleCreationResult result = submitStudioArticle.execute(new StudioArticleSubmission(
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
		return ResponseEntity.status(HttpStatus.ACCEPTED)
				.body(AdminStudioArticleSubmittedResponse.from(result));
	}

	@PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<AdminStudioArticleImageResponse> uploadImage(
			@RequestPart("articleId") String rawArticleId,
			@RequestPart(value = "alt", required = false) String alt,
			@RequestPart("image") MultipartFile image) throws IOException {
		StudioArticleImageAsset asset = storeStudioArticleImage.execute(
				UUID.fromString(rawArticleId),
				image.getOriginalFilename(),
				image.getContentType(),
				image.getBytes(),
				alt);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(AdminStudioArticleImageResponse.from(asset));
	}

	public record AdminStudioArticleSubmitRequest(
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
			String url,
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
			Integer width,
			Integer height,
			String alt) {
		static AdminStudioArticleImageResponse from(StudioArticleImageAsset asset) {
			return new AdminStudioArticleImageResponse(
					asset.assetId(),
					asset.url(),
					asset.width(),
					asset.height(),
					asset.alt());
		}
	}
}
