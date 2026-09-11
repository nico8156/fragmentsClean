package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.fake;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceReportRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import java.util.*;

public final class FakeExperienceReportRepository implements ExperienceReportRepository{
    private final Map<UUID,ExperienceReport> values=new HashMap<>();
    @Override public Optional<ExperienceReport> byId(UUID id){return Optional.ofNullable(values.get(id));}
    @Override public Optional<ExperienceReport> byReporterAndExperience(UUID reporterId,UUID experienceId){return values.values().stream().filter(r->{var s=r.toSnapshot();return s.reporterId().equals(reporterId)&&s.experienceId().equals(experienceId);}).findFirst();}
    @Override public List<ExperienceReport> openByExperience(UUID experienceId){return values.values().stream().filter(r->{var s=r.toSnapshot();return s.experienceId().equals(experienceId)&&s.status()==ExperienceReportStatus.OPEN;}).toList();}
    @Override public void save(ExperienceReport report){values.put(report.toSnapshot().reportId(),report);}
}
