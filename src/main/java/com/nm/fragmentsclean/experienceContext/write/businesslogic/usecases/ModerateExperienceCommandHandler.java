package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import java.util.LinkedHashMap;
import java.util.UUID;

@Transactional
public class ModerateExperienceCommandHandler implements CommandHandler<ModerateExperienceCommand>{
    private final ExperienceRepository experiences;private final ExperienceReportRepository reports;private final DomainEventPublisher events;private final DateTimeProvider clock;
    public ModerateExperienceCommandHandler(ExperienceRepository experiences,ExperienceReportRepository reports,DomainEventPublisher events,DateTimeProvider clock){this.experiences=experiences;this.reports=reports;this.events=events;this.clock=clock;}
    @Override public void execute(ModerateExperienceCommand command){var report=reports.byId(command.reportId()).orElseThrow(()->new BusinessCommandRejectedException("REPORT_NOT_FOUND","Report does not exist"));if(!report.toSnapshot().experienceId().equals(command.experienceId()))throw new BusinessCommandRejectedException("REPORT_EXPERIENCE_MISMATCH","Report does not belong to experience");if(command.reason()!=null&&command.reason().length()>1000)throw new BusinessCommandRejectedException("MODERATION_REASON_TOO_LONG","Moderation reason exceeds 1000 characters");var experience=experiences.byId(command.experienceId()).orElseThrow(()->new BusinessCommandRejectedException("EXPERIENCE_NOT_FOUND","Experience does not exist"));var now=clock.now();var reportStatus=command.decision()==ExperienceModerationStatus.HIDDEN?ExperienceReportStatus.RESOLVED:ExperienceReportStatus.DISMISSED;boolean experienceChanged=experience.moderate(command.decision(),now);var toClose=new LinkedHashMap<UUID,ExperienceReport>();toClose.put(report.toSnapshot().reportId(),report);reports.openByExperience(command.experienceId()).forEach(item->toClose.put(item.toSnapshot().reportId(),item));boolean reportChanged=false;for(var item:toClose.values()){boolean changed=item.resolve(reportStatus,now);reportChanged|=changed;if(changed)reports.save(item);}if(!experienceChanged&&!reportChanged)return;if(experienceChanged){experiences.save(experience);experience.registerChange(command.commandId(),command.decision()==ExperienceModerationStatus.HIDDEN?"MODERATED_HIDDEN":"MODERATED_RESTORED",command.clientAt(),now);}experience.registerModerationDecision(command.commandId(),command.actionId(),command.reportId(),command.operatorId(),reportStatus,command.reason(),command.clientAt(),now);ExperienceEvents.publish(experience,events);}
}
