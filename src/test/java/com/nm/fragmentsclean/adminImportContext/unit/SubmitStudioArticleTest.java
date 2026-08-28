package com.nm.fragmentsclean.adminImportContext.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleBlock;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleImageRef;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleSubmission;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SubmitStudioArticle;

class SubmitStudioArticleTest {
	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID REVIEW_COMMAND_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
	private static final UUID PUBLISH_COMMAND_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
	private static final UUID ARTICLE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID REVISION_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
	private static final UUID AUTHOR_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
	private static final Instant NOW = Instant.parse("2026-07-10T10:15:30Z");

	@Test
	void maps_studio_submission_to_article_command() {
		var authoringPort = new RecordingArticleAuthoringPort();
		var useCase = new SubmitStudioArticle(
				authoringPort,
				new SequenceUuidGenerator(COMMAND_ID, REVIEW_COMMAND_ID, PUBLISH_COMMAND_ID, ARTICLE_ID, REVISION_ID),
				() -> NOW);

		var result = useCase.execute(new StudioArticleSubmission(
				null,
				null,
				"rennes-coffee-guide",
				"fr-FR",
				AUTHOR_ID,
				"Fragments Studio",
				"Guide cafe Rennes",
				"Intro",
				List.of(new StudioArticleBlock(
						"Premier arret",
						"Un paragraphe",
						new StudioArticleImageRef("s3://bucket/articles/photo.jpg", "https://preview/photo.jpg", 1200, 800, "Photo"))),
				"Conclusion",
				new StudioArticleImageRef("s3://bucket/articles/cover.jpg", "https://preview/cover.jpg", 1600, 900, "Cover"),
				List.of("coffee", "rennes"),
				null,
				List.of()));

		assertThat(result.commandId()).isEqualTo(PUBLISH_COMMAND_ID);
		assertThat(result.articleId()).isEqualTo(ARTICLE_ID);
		assertThat(authoringPort.command).isNotNull();
		assertThat(authoringPort.command.commandId()).isEqualTo(COMMAND_ID);
		assertThat(authoringPort.command.articleId()).isEqualTo(ARTICLE_ID);
		assertThat(authoringPort.command.revisionId()).isEqualTo(REVISION_ID);
		assertThat(authoringPort.command.slug()).isEqualTo("rennes-coffee-guide");
		assertThat(authoringPort.command.coverUrl()).isEqualTo("s3://bucket/articles/cover.jpg");
		assertThat(authoringPort.command.blocks().getFirst().heading()).isEqualTo("Premier arret");
		assertThat(authoringPort.command.blocks().getFirst().photo().url()).isEqualTo("s3://bucket/articles/photo.jpg");
		assertThat(authoringPort.command.readingTimeMin()).isGreaterThanOrEqualTo(1);
		assertThat(authoringPort.command.clientAt()).isEqualTo(NOW);
	}

	private static class RecordingArticleAuthoringPort implements ArticleAuthoringPort {
		StudioArticleCommand command;

		@Override
		public void saveDraft(StudioArticleCommand command) {
			this.command = command;
		}
		public void submitForReview(UUID commandId, Instant clientAt, UUID articleId) { }
		public void publish(UUID commandId, Instant clientAt, UUID articleId, UUID revisionId) { }
		public void archive(UUID commandId, Instant clientAt, UUID articleId) { }
	}

	private static class SequenceUuidGenerator implements UuidGenerator {
		private final UUID[] values;
		private int calls;

		SequenceUuidGenerator(UUID... values) {
			this.values = values;
		}

		@Override
		public UUID generate() {
			calls += 1;
			return values[calls - 1];
		}
	}
}
