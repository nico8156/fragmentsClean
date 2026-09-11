package com.nm.fragmentsclean.experienceContextTest.unit;

import static org.assertj.core.api.Assertions.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExperienceTest {
    private static final UUID ID=UUID.randomUUID(),USER=UUID.randomUUID(),COFFEE=UUID.randomUUID();
    private static final Instant NOW=Instant.parse("2026-09-11T12:00:00Z");
    private final ExperienceContentPolicy policy=new ExperienceContentPolicy(Set.of("forbidden"));

    @Test void draft_may_be_empty_but_publication_requires_text(){
        var experience=Experience.create(ID,USER,COFFEE,null,ExperiencePublicationStatus.DRAFT,policy,NOW);
        assertThat(experience.toSnapshot().message()).isNull();
        assertThatThrownBy(()->experience.publish(USER,policy,NOW.plusSeconds(1)))
                .isInstanceOf(BusinessCommandRejectedException.class).hasMessageContaining("message");
    }
    @Test void publication_and_edits_are_owned_and_filtered(){
        var experience=Experience.create(ID,USER,COFFEE,"  Une belle visite  ",ExperiencePublicationStatus.DRAFT,policy,NOW);
        assertThat(experience.publish(USER,policy,NOW.plusSeconds(1))).isTrue();
        assertThat(experience.toSnapshot().message()).isEqualTo("Une belle visite");
        assertThatThrownBy(()->experience.updateMessage(UUID.randomUUID(),"autre",policy,NOW.plusSeconds(2)))
                .isInstanceOf(BusinessCommandRejectedException.class);
        assertThatThrownBy(()->experience.updateMessage(USER,"forbidden words",policy,NOW.plusSeconds(2)))
                .isInstanceOf(BusinessCommandRejectedException.class);
    }
    @Test void deletion_and_moderation_are_idempotent(){
        var experience=Experience.create(ID,USER,COFFEE,"Visite",ExperiencePublicationStatus.PUBLISHED,policy,NOW);
        assertThat(experience.moderate(ExperienceModerationStatus.HIDDEN,NOW.plusSeconds(1))).isTrue();
        assertThat(experience.moderate(ExperienceModerationStatus.HIDDEN,NOW.plusSeconds(2))).isFalse();
        assertThat(experience.delete(USER,NOW.plusSeconds(3))).isTrue();
        assertThat(experience.delete(USER,NOW.plusSeconds(4))).isFalse();
        assertThatThrownBy(()->experience.moderate(ExperienceModerationStatus.VISIBLE,NOW.plusSeconds(5)))
                .isInstanceOf(BusinessCommandRejectedException.class);
    }
}
