package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSourceAdministrationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import java.time.Duration;
import java.util.UUID;

public final class ManageEditorialSource {
 private final EditorialSourceAdministrationPort sources; private final UuidGenerator ids;
 public ManageEditorialSource(EditorialSourceAdministrationPort sources,UuidGenerator ids){this.sources=sources;this.ids=ids;}
 public UUID register(Request request){UUID id=ids.generate();sources.register(id,request.name(),request.accessMode(),request.authorityLevel(),request.endpoint(),Duration.ofSeconds(request.pollingFrequencySeconds()));return id;}
 public void revise(UUID id,Request request){sources.revise(id,request.name(),request.accessMode(),request.authorityLevel(),request.endpoint(),Duration.ofSeconds(request.pollingFrequencySeconds()),request.enabled());}
 public record Request(String name,String accessMode,String authorityLevel,String endpoint,long pollingFrequencySeconds,boolean enabled) { }
}
