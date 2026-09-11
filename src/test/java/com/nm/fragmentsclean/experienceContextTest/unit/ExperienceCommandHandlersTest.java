package com.nm.fragmentsclean.experienceContextTest.unit;

import static org.assertj.core.api.Assertions.*;
import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.fake.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExperienceCommandHandlersTest {
    private final UUID user=UUID.randomUUID(),coffee=UUID.randomUUID(),experienceId=UUID.randomUUID();
    private final FakeExperienceRepository experiences=new FakeExperienceRepository();
    private final FakeExperienceReportRepository reports=new FakeExperienceReportRepository();
    private final FakeDomainEventPublisher events=new FakeDomainEventPublisher();
    private final DeterministicDateTimeProvider clock=new DeterministicDateTimeProvider();
    private final ExperienceContentPolicy policy=new ExperienceContentPolicy(Set.of());
    private final Instant clientAt=Instant.parse("2026-09-11T12:00:00Z");

    @Test void creates_a_published_experience_without_a_ticket_and_emits_snapshot_and_pass_fact(){
        var handler=new CreateExperienceCommandHandler(experiences,id->id.equals(coffee),policy,events,clock);
        handler.execute(new CreateExperienceCommand(UUID.randomUUID(),experienceId,user,coffee,"Très bon café",ExperiencePublicationStatus.PUBLISHED,clientAt));
        assertThat(experiences.byId(experienceId)).isPresent();
        assertThat(events.published).extracting(Object::getClass).containsExactly(
                ExperienceSnapshotChangedEvent.class,ExperienceLifecycleChangedEvent.class);
    }
    @Test void rejects_an_unknown_coffee_and_conflicting_replay(){
        var handler=new CreateExperienceCommandHandler(experiences,id->false,policy,events,clock);
        assertThatThrownBy(()->handler.execute(new CreateExperienceCommand(UUID.randomUUID(),experienceId,user,coffee,"Visite",ExperiencePublicationStatus.PUBLISHED,clientAt)))
                .isInstanceOf(BusinessCommandRejectedException.class);
        new CreateExperienceCommandHandler(experiences,id->true,policy,events,clock).execute(
                new CreateExperienceCommand(UUID.randomUUID(),experienceId,user,coffee,"Visite",ExperiencePublicationStatus.PUBLISHED,clientAt));
        assertThatThrownBy(()->new CreateExperienceCommandHandler(experiences,id->true,policy,events,clock).execute(
                new CreateExperienceCommand(UUID.randomUUID(),experienceId,UUID.randomUUID(),coffee,"Visite",ExperiencePublicationStatus.PUBLISHED,clientAt)))
                .isInstanceOf(BusinessCommandRejectedException.class);
    }
    @Test void reports_then_moderates_and_closes_all_open_reports(){
        new CreateExperienceCommandHandler(experiences,id->true,policy,events,clock).execute(
                new CreateExperienceCommand(UUID.randomUUID(),experienceId,user,coffee,"Visite",ExperiencePublicationStatus.PUBLISHED,clientAt));
        UUID reporter=UUID.randomUUID(),reportId=UUID.randomUUID();
        new ReportExperienceCommandHandler(experiences,reports,events,clock).execute(
                new ReportExperienceCommand(UUID.randomUUID(),reportId,experienceId,reporter,ExperienceReportReason.SPAM,null,clientAt));
        new ModerateExperienceCommandHandler(experiences,reports,events,clock).execute(
                new ModerateExperienceCommand(UUID.randomUUID(),UUID.randomUUID(),reportId,experienceId,
                        UUID.randomUUID(),ExperienceModerationStatus.HIDDEN,"spam",clientAt));
        assertThat(experiences.byId(experienceId).orElseThrow().toSnapshot().moderationStatus())
                .isEqualTo(ExperienceModerationStatus.HIDDEN);
        assertThat(reports.byId(reportId).orElseThrow().toSnapshot().status()).isEqualTo(ExperienceReportStatus.RESOLVED);
        assertThat(events.published).anyMatch(ExperienceModerationDecidedEvent.class::isInstance);

        UUID secondReportId = UUID.randomUUID();
        new ReportExperienceCommandHandler(experiences, reports, events, clock).execute(
                new ReportExperienceCommand(UUID.randomUUID(), secondReportId, experienceId,
                        UUID.randomUUID(), ExperienceReportReason.OTHER, "nouveau signalement", clientAt));
        events.published.clear();

        new ModerateExperienceCommandHandler(experiences, reports, events, clock).execute(
                new ModerateExperienceCommand(UUID.randomUUID(), UUID.randomUUID(), secondReportId,
                        experienceId, UUID.randomUUID(), ExperienceModerationStatus.HIDDEN,
                        "reste masqué", clientAt));

        assertThat(events.published).extracting(Object::getClass)
                .containsExactly(ExperienceModerationDecidedEvent.class);
    }
}
