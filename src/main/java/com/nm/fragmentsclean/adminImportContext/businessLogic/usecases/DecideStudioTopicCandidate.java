package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.TopicCandidateDecisionPort; import java.util.UUID;
public final class DecideStudioTopicCandidate {private final TopicCandidateDecisionPort port;public DecideStudioTopicCandidate(TopicCandidateDecisionPort port){this.port=port;}public void execute(UUID id,String decision){port.decide(id,decision);}}
