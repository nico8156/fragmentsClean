package com.nm.fragmentsclean.sharedKernel.eventing;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.articleContext.write.businesslogic.eventing.ArticleOutboxEventMetadataContributor;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleStatus;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.eventing.AuthOutboxEventMetadataContributor;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.eventing.CoffeeOutboxEventMetadataContributor;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.platform.eventing.CompositeOutboxEventMetadataResolver;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadata;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxEventMetadataContributor;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.eventing.SocialOutboxEventMetadataContributor;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.eventing.TicketOutboxEventMetadataContributor;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.eventing.AppUserOutboxEventMetadataContributor;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUserCreatedEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class OutboxEventMetadataContributorTest {

	private static final Instant NOW = Instant.parse("2026-07-06T08:00:00Z");
	private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID COMMAND_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID ENTITY_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

	@Test
	void composite_uses_first_matching_contributor() {
		OutboxEventMetadataContributor ignored = event -> Optional.empty();
		OutboxEventMetadataContributor matched = event -> Optional.of(
				new OutboxEventMetadata("Matched", "entity-1", "stream:entity-1"));

		var metadata = new CompositeOutboxEventMetadataResolver(List.of(ignored, matched))
				.resolve(unknownEvent());

		assertThat(metadata).isEqualTo(new OutboxEventMetadata("Matched", "entity-1", "stream:entity-1"));
	}

	@Test
	void composite_falls_back_to_unknown_metadata_when_no_contributor_matches() {
		var metadata = new CompositeOutboxEventMetadataResolver(List.of())
				.resolve(unknownEvent());

		assertThat(metadata).isEqualTo(new OutboxEventMetadata("Unknown", "unknown", "global"));
	}

	@Test
	void auth_contributor_resolves_auth_user_events() {
		var event = new AuthUserCreatedEvent(
				EVENT_ID,
				ENTITY_ID,
				AuthProvider.GOOGLE,
				"google-user",
				"user@example.test",
				true,
				"User",
				null,
				NOW);

		assertThat(new AuthOutboxEventMetadataContributor().resolve(event))
				.contains(new OutboxEventMetadata("AuthUser", ENTITY_ID.toString(), "authUser:" + ENTITY_ID));
	}

	@Test
	void app_user_contributor_resolves_app_user_events() {
		var event = new AppUserCreatedEvent(
				EVENT_ID,
				ENTITY_ID,
				USER_ID,
				"User",
				null,
				1L,
				NOW);

		assertThat(new AppUserOutboxEventMetadataContributor().resolve(event))
				.contains(new OutboxEventMetadata("AppUser", ENTITY_ID.toString(), "appUser:" + ENTITY_ID));
	}

	@Test
	void social_contributor_resolves_like_events() {
		var event = new LikeSetEvent(
				EVENT_ID,
				"command-1",
				ENTITY_ID,
				USER_ID,
				UUID.fromString("55555555-5555-5555-5555-555555555555"),
				true,
				3L,
				1L,
				NOW,
				NOW.minusSeconds(1));

		assertThat(new SocialOutboxEventMetadataContributor().resolve(event))
				.contains(new OutboxEventMetadata("Like", ENTITY_ID.toString(), "user:" + USER_ID));
	}

	@Test
	void ticket_contributor_resolves_ticket_events() {
		var event = new TicketVerifyAcceptedEvent(
				EVENT_ID,
				COMMAND_ID,
				ENTITY_ID,
				USER_ID,
				"ticket text",
				null,
				"ACCEPTED",
				1L,
				NOW,
				NOW.minusSeconds(1));

		assertThat(new TicketOutboxEventMetadataContributor().resolve(event))
				.contains(new OutboxEventMetadata("Ticket", ENTITY_ID.toString(), "user:" + USER_ID));
	}

	@Test
	void article_contributor_resolves_article_events() {
		var event = new ArticleCreatedEvent(
				EVENT_ID,
				COMMAND_ID,
				ENTITY_ID,
				"slug",
				"fr-FR",
				USER_ID,
				"Author",
				"Title",
				"Intro",
				"[]",
				null,
				null,
				null,
				null,
				null,
				List.of("coffee"),
				2,
				List.of(),
				ArticleStatus.PUBLISHED,
				1L,
				NOW,
				NOW.minusSeconds(1));

		assertThat(new ArticleOutboxEventMetadataContributor().resolve(event))
				.contains(new OutboxEventMetadata("Article", ENTITY_ID.toString(), "article:" + ENTITY_ID));
	}

	@Test
	void coffee_contributor_resolves_coffee_events() {
		var event = new CoffeeArchivedEvent(
				EVENT_ID,
				COMMAND_ID,
				new CoffeeId(ENTITY_ID),
				1,
				NOW,
				NOW.minusSeconds(1));

		assertThat(new CoffeeOutboxEventMetadataContributor().resolve(event))
				.contains(new OutboxEventMetadata("Coffee", ENTITY_ID.toString(), "coffee:" + ENTITY_ID));
	}

	private DomainEvent unknownEvent() {
		return new DomainEvent() {
			@Override
			public UUID eventId() {
				return EVENT_ID;
			}

			@Override
			public Instant occurredAt() {
				return NOW;
			}
		};
	}
}
