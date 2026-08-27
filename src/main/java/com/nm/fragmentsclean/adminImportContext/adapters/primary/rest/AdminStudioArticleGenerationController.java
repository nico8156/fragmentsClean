package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationRequest;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioGeneratedArticleEdit;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.EditStudioGeneratedArticle;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StartStudioArticleGeneration;
import com.nm.fragmentsclean.aticleContext.read.GetArticleGenerationReview;
import com.nm.fragmentsclean.aticleContext.read.GetArticleGenerationReviewQueryHandler;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.ApproveArticlePublication;

@RestController
@RequestMapping("/api/admin/studio/article-generations")
public final class AdminStudioArticleGenerationController {
	private final StartStudioArticleGeneration start;
	private final GetArticleGenerationReviewQueryHandler reviews;
	private final EditStudioGeneratedArticle edit;
	private final ApproveArticlePublication approve;

	public AdminStudioArticleGenerationController(
			StartStudioArticleGeneration start,
			GetArticleGenerationReviewQueryHandler reviews,
			EditStudioGeneratedArticle edit,
			ApproveArticlePublication approve) {
		this.start = start;
		this.reviews = reviews;
		this.edit = edit;
		this.approve = approve;
	}

	@PostMapping
	public ResponseEntity<StudioArticleGenerationResult> generate(
			@RequestBody Request body,
			Authentication authentication) {
		UUID operatorId = UUID.fromString(authentication.getName());
		var request = new StudioArticleGenerationRequest(
				body.subject(), body.locale(), operatorId, authentication.getName());
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(start.execute(request));
	}

	@GetMapping("/{sagaId}")
	public GetArticleGenerationReview review(@PathVariable UUID sagaId) {
		return reviews.handle(sagaId);
	}

	@PutMapping("/{sagaId}/revision")
	public ResponseEntity<CommandAccepted> edit(
			@PathVariable UUID sagaId,
			@RequestBody EditRequest body) {
		UUID commandId = edit.execute(body.toModel(sagaId));
		return ResponseEntity.accepted().body(new CommandAccepted(commandId));
	}

	@PostMapping("/approvals/{token}/publish")
	public ResponseEntity<CommandAccepted> approve(@PathVariable String token) {
		return ResponseEntity.accepted().body(new CommandAccepted(approve.execute(token)));
	}

	public record Request(String subject, String locale) {
	}

	public record EditRequest(
			UUID articleId,
			UUID revisionId,
			String title,
			String introduction,
			String conclusion,
			Cover cover,
			List<Section> sections,
			List<String> tags) {
		StudioGeneratedArticleEdit toModel(UUID sagaId) {
			return new StudioGeneratedArticleEdit(
					sagaId,
					articleId,
					revisionId,
					title,
					introduction,
					conclusion,
					cover.toModel(),
					sections.stream().map(Section::toModel).toList(),
					tags);
		}
	}

	public record Cover(String storageReference, int width, int height, String alt) {
		StudioGeneratedArticleEdit.Cover toModel() {
			return new StudioGeneratedArticleEdit.Cover(storageReference, width, height, alt);
		}
	}

	public record Section(String heading, String paragraph, String storageReference, int width, int height, String alt) {
		StudioGeneratedArticleEdit.Section toModel() {
			return new StudioGeneratedArticleEdit.Section(heading, paragraph, storageReference, width, height, alt);
		}
	}

	public record CommandAccepted(UUID commandId) {
	}
}
