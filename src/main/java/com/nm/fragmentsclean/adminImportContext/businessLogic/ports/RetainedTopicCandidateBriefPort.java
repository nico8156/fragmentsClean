package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.util.UUID;
import java.util.List;

public interface RetainedTopicCandidateBriefPort {
    Brief load(UUID candidateId, String locale);

    record Brief(UUID candidateId, String subject, String editorialAngle, String locale, List<Provenance> provenance) {
        public Brief { provenance = List.copyOf(provenance); }
    }
    record Provenance(String sourceName, String sourceUrl, String publishedAt, String authorityLevel) { }
}
