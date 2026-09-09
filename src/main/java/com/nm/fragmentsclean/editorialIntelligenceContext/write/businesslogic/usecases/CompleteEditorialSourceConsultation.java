package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.SourceSignal;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import java.time.Instant; import java.util.*;

@Component public class CompleteEditorialSourceConsultation {
 private final EditorialSourceRepository sources; private final SourceSignalRepository signals; private final DateTimeProvider clock;
 public CompleteEditorialSourceConsultation(EditorialSourceRepository sources,SourceSignalRepository signals,DateTimeProvider clock){this.sources=sources;this.signals=signals;this.clock=clock;}
 @Transactional public int execute(UUID sourceId,String workerId,List<SourceSignal> discovered,String etag,String lastModified,String lastExternalId,Instant lastPublishedAt){
  var source=sources.byId(sourceId).orElseThrow(()->new IllegalStateException("Editorial source not found: "+sourceId));
  var items=discovered==null?List.<SourceSignal>of():List.copyOf(discovered);
  if(items.stream().anyMatch(signal->!sourceId.equals(signal.sourceId()))) throw new IllegalArgumentException("Signal source mismatch");
  int count=signals.saveIgnoringDuplicate(items);
  source.completeConsultation(workerId,count,etag,lastModified,lastExternalId,lastPublishedAt,clock.now()); sources.save(source); return count;
 }
}
