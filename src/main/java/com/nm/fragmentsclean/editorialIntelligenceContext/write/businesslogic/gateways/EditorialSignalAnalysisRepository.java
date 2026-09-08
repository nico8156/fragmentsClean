package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.SourceSignal; import java.util.*;
public interface EditorialSignalAnalysisRepository { List<SourceSignal> newSignals(int limit); void markAnalyzed(List<UUID> signalIds); }
