package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;

@Transactional
public class ReportExperienceCommandHandler implements CommandHandler<ReportExperienceCommand>{
    private final ExperienceRepository experiences;private final ExperienceReportRepository reports;private final DomainEventPublisher events;private final DateTimeProvider clock;
    public ReportExperienceCommandHandler(ExperienceRepository experiences,ExperienceReportRepository reports,DomainEventPublisher events,DateTimeProvider clock){this.experiences=experiences;this.reports=reports;this.events=events;this.clock=clock;}
    @Override public void execute(ReportExperienceCommand command){var existing=reports.byReporterAndExperience(command.reporterId(),command.experienceId());if(existing.isPresent()){if(!existing.get().toSnapshot().reportId().equals(command.reportId()))throw new BusinessCommandRejectedException("EXPERIENCE_ALREADY_REPORTED","Experience was already reported");return;}var experience=experiences.byId(command.experienceId()).orElseThrow(()->new BusinessCommandRejectedException("EXPERIENCE_NOT_FOUND","Experience does not exist"));var s=experience.toSnapshot();if(s.publicationStatus()!=ExperiencePublicationStatus.PUBLISHED)throw new BusinessCommandRejectedException("EXPERIENCE_NOT_FOUND","Experience does not exist");var now=clock.now();var report=ExperienceReport.create(command.reportId(),s.experienceId(),s.coffeeId(),s.userId(),command.reporterId(),command.reason(),command.details(),now);reports.save(report);report.registerCreated(command.commandId(),command.clientAt(),now);ExperienceEvents.publish(report,events);}
}
