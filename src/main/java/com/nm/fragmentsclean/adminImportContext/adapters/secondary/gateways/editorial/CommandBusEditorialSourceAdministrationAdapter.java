package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.editorial;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSourceAdministrationPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.*;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import java.time.Duration;
import java.util.UUID;

public final class CommandBusEditorialSourceAdministrationAdapter implements EditorialSourceAdministrationPort {
 private final CommandBus commands; public CommandBusEditorialSourceAdministrationAdapter(CommandBus commands){this.commands=commands;}
 public void register(UUID id,String name,String mode,String level,String endpoint,Duration frequency){commands.dispatch(new RegisterEditorialSourceCommand(id,name,EditorialSourceAccessMode.valueOf(mode),EditorialAuthorityLevel.valueOf(level),endpoint,frequency));}
 public void revise(UUID id,String name,String mode,String level,String endpoint,Duration frequency,boolean enabled){commands.dispatch(new ReviseEditorialSourceCommand(id,name,EditorialSourceAccessMode.valueOf(mode),EditorialAuthorityLevel.valueOf(level),endpoint,frequency,enabled));}
}
