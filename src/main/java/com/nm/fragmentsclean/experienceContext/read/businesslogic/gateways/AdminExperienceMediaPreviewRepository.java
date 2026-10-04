package com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways;
import java.util.*;
public interface AdminExperienceMediaPreviewRepository {
    Map<UUID,String> availablePreviews(List<UUID> mediaIds);
}
