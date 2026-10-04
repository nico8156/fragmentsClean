package com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMedia;
import java.util.List;
import java.util.UUID;
public interface ExperienceMediaCatalogScan {
    record Batch(boolean due, List<ExperienceMedia.Snapshot> items) { }
    Batch lockNext(int limit);
    void advance(UUID lastId, boolean complete);
}
