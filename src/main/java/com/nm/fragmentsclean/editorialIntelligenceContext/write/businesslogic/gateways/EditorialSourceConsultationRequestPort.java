package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ClaimEditorialSourceConsultationCommand;

public interface EditorialSourceConsultationRequestPort {
    void request(ClaimEditorialSourceConsultationCommand command);
}
