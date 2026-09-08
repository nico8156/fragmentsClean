package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.SourceSignal;
import java.util.List;

public interface SourceSignalRepository {
    int saveIgnoringDuplicate(List<SourceSignal> signals);
}
