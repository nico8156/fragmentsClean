package com.nm.fragmentsclean.adminImportContext.businessLogic.ports; import java.util.*;
public interface TopicCandidateStudioCatalog { List<Item> list(); record Item(UUID id,String subject,String suggestedAngle,List<UUID> signalIds,String status){} }
