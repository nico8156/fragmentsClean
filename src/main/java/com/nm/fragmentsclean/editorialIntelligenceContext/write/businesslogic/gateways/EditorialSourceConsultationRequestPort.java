package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ConsultEditorialSourceCommand;

public interface EditorialSourceConsultationRequestPort {
    void request(ConsultEditorialSourceCommand command);
}
