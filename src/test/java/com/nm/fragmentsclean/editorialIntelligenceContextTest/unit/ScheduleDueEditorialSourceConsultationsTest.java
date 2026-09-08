package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import org.junit.jupiter.api.Test;
import java.time.*; import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class ScheduleDueEditorialSourceConsultationsTest {
 @Test void submits_one_claim_per_due_source_without_consulting_them() {
  var source=EditorialSource.register(UUID.randomUUID(),"SCA",EditorialSourceAccessMode.RSS,EditorialAuthorityLevel.AUTHORITATIVE,"https://sca.coffee",Duration.ofHours(6),Instant.parse("2023-10-01T11:00:00Z"));
  var requests=new ArrayList<ClaimEditorialSourceConsultationCommand>();
  EditorialSourceRepository repo=new EditorialSourceRepository(){ public Optional<EditorialSource> byId(UUID id){return Optional.empty();} public List<EditorialSource> dueAt(Instant now,int limit){return List.of(source);} public void save(EditorialSource s){} };
  var useCase=new ScheduleDueEditorialSourceConsultations(repo,requests::add,new DeterministicDateTimeProvider());
  assertThat(useCase.execute(20,Duration.ofMinutes(5),"scheduler")).isEqualTo(1);
  assertThat(requests).singleElement().satisfies(c -> { assertThat(c.sourceId()).isEqualTo(source.snapshot().id()); assertThat(c.leaseUntil()).isEqualTo(Instant.parse("2023-10-01T11:05:00Z")); });
 }
 @Test void ignores_invalid_scheduler_configuration() {
  EditorialSourceRepository repo=new EditorialSourceRepository(){ public Optional<EditorialSource> byId(UUID id){return Optional.empty();} public List<EditorialSource> dueAt(Instant now,int limit){throw new AssertionError("must not query");} public void save(EditorialSource s){} };
  var useCase=new ScheduleDueEditorialSourceConsultations(repo,c -> { throw new AssertionError("must not dispatch"); },new DeterministicDateTimeProvider());
  assertThat(useCase.execute(0,Duration.ofMinutes(5),"scheduler")).isZero();
 }
}
