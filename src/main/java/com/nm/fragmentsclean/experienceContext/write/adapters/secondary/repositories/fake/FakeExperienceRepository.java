package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.fake;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.Experience;
import java.util.*;

public final class FakeExperienceRepository implements ExperienceRepository {
    private final Map<UUID,Experience> values=new HashMap<>();
    @Override public Optional<Experience> byId(UUID id){return Optional.ofNullable(values.get(id));}
    @Override public void save(Experience value){values.put(value.toSnapshot().experienceId(),value);}
}
