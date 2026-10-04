package com.nm.fragmentsclean.experienceContext.read;
import java.util.Optional;
import org.springframework.stereotype.Service;
@Service public class ReadExperienceMediaLifecycleQueryHandler {
    private final ExperienceMediaLifecycleReadRepository repository;
    public ReadExperienceMediaLifecycleQueryHandler(ExperienceMediaLifecycleReadRepository repository) { this.repository=repository; }
    public Optional<ExperienceMediaLifecycleView> handle(ReadExperienceMediaLifecycleQuery query) { return repository.byId(query.mediaId()); }
}
