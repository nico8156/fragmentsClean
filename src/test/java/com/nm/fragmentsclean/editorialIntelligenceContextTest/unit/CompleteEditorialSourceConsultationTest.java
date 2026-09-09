package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.CompleteEditorialSourceConsultation;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import org.junit.jupiter.api.Test; import java.time.*; import java.util.*; import static org.assertj.core.api.Assertions.*;
class CompleteEditorialSourceConsultationTest {
 @Test void stores_signals_before_advancing_checkpoint_and_releases_lease(){
  var now=Instant.parse("2023-10-01T11:00:00Z"); var id=UUID.randomUUID(); var source=EditorialSource.register(id,"SCA",EditorialSourceAccessMode.RSS,EditorialAuthorityLevel.AUTHORITATIVE,"https://sca.coffee",Duration.ofHours(6),now); source.claimConsultation("w",now,now.plusSeconds(60));
  var repo=new SourceRepo(source); var signals=new Signals(); var item=new SourceSignal(UUID.randomUUID(),id,"x","Title",null,"https://x",null,null,now,"f");
  assertThat(new CompleteEditorialSourceConsultation(repo,signals,new DeterministicDateTimeProvider()).execute(id,"w",List.of(item),"e","Mon, 08 Sep 2026 10:00:00 GMT","x",null)).isEqualTo(1);
  assertThat(repo.source.snapshot().leaseOwner()).isNull(); assertThat(repo.source.snapshot().checkpoint().lastExternalId()).isEqualTo("x");
 }
 @Test void rejects_foreign_signal_without_advancing_checkpoint(){
  var now=Instant.parse("2023-10-01T11:00:00Z"); var id=UUID.randomUUID(); var source=EditorialSource.register(id,"SCA",EditorialSourceAccessMode.RSS,EditorialAuthorityLevel.AUTHORITATIVE,"https://sca.coffee",Duration.ofHours(6),now); source.claimConsultation("w",now,now.plusSeconds(60)); var repo=new SourceRepo(source);
  var foreign=new SourceSignal(UUID.randomUUID(),UUID.randomUUID(),"x","Title",null,"https://x",null,null,now,"f");
  assertThatThrownBy(()->new CompleteEditorialSourceConsultation(repo,new Signals(),new DeterministicDateTimeProvider()).execute(id,"w",List.of(foreign),null,null,null,null)).hasMessageContaining("mismatch"); assertThat(repo.source.snapshot().leaseOwner()).isEqualTo("w");
 }
 @Test void does_not_advance_the_source_when_signal_persistence_fails(){
  var now=Instant.parse("2023-10-01T11:00:00Z"); var id=UUID.randomUUID(); var source=EditorialSource.register(id,"SCA",EditorialSourceAccessMode.RSS,EditorialAuthorityLevel.AUTHORITATIVE,"https://sca.coffee",Duration.ofHours(6),now); source.claimConsultation("w",now,now.plusSeconds(60)); var repo=new SourceRepo(source);
  var item=new SourceSignal(UUID.randomUUID(),id,"x","Title",null,"https://x",null,null,now,"f");
  assertThatThrownBy(()->new CompleteEditorialSourceConsultation(repo,signals -> { throw new IllegalStateException("database unavailable"); },new DeterministicDateTimeProvider()).execute(id,"w",List.of(item),null,null,"x",null)).hasMessageContaining("database unavailable");
  assertThat(repo.source.snapshot().leaseOwner()).isEqualTo("w"); assertThat(repo.source.snapshot().checkpoint().lastExternalId()).isNull();
 }
 static final class SourceRepo implements EditorialSourceRepository { EditorialSource source; SourceRepo(EditorialSource s){source=s;} public Optional<EditorialSource> byId(UUID i){return Optional.of(source);} public List<EditorialSource> dueAt(Instant n,int l){return List.of();} public void save(EditorialSource s){source=s;} }
 static final class Signals implements SourceSignalRepository { public int saveIgnoringDuplicate(List<SourceSignal> s){return s.size();} }
}
