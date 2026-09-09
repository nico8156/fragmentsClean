package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class TopicCandidateTest {
 @Test void requires_provenance_and_locks_a_retained_editorial_decision() {
  assertThatThrownBy(() -> TopicCandidate.detect(UUID.randomUUID(),"Sujet","Angle",List.of(),Instant.now())).isInstanceOf(IllegalArgumentException.class);
  var candidate=TopicCandidate.detect(UUID.randomUUID(),"Variétés résistantes","Ce qu'elles changent dans la tasse",List.of(UUID.randomUUID()),Instant.now());
  candidate.retain();
  assertThat(candidate.snapshot().status()).isEqualTo(TopicCandidateStatus.RETAINED);
  assertThatThrownBy(candidate::ignore).hasMessageContaining("retained");
 }
}
