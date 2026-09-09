package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.EditorialSourceStudioCatalog;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ManageEditorialSource;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.AnalyzeStudioEditorialSignals;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/** Studio primary adapter. It delegates both writes and reads through admin ACL ports. */
@RestController
@RequestMapping("/api/admin/editorial/sources")
public final class AdminEditorialSourcesController {
 private final ManageEditorialSource manage; private final EditorialSourceStudioCatalog catalog; private final AnalyzeStudioEditorialSignals analysis;
 public AdminEditorialSourcesController(ManageEditorialSource manage, EditorialSourceStudioCatalog catalog, AnalyzeStudioEditorialSignals analysis){this.manage=manage;this.catalog=catalog;this.analysis=analysis;}
 @GetMapping public SourceListResponse list(){return new SourceListResponse(catalog.listSources());}
 @PostMapping public ResponseEntity<SourceAcceptedResponse> register(@RequestBody SourceRequest request){UUID id=manage.register(request.toModel());return ResponseEntity.accepted().body(new SourceAcceptedResponse(id));}
 @PutMapping("/{sourceId}") public ResponseEntity<Void> revise(@PathVariable UUID sourceId,@RequestBody SourceRequest request){manage.revise(sourceId,request.toModel());return ResponseEntity.accepted().build();}
 @PostMapping("/analysis") public ResponseEntity<Void> analyzePendingSignals(){analysis.execute();return ResponseEntity.noContent().build();}
 @GetMapping("/{sourceId}/signals") public SignalListResponse signals(@PathVariable UUID sourceId,@RequestParam(defaultValue="50") int limit){return new SignalListResponse(catalog.listSignals(sourceId,limit));}
 public record SourceRequest(String name,String accessMode,String authorityLevel,String endpoint,long pollingFrequencySeconds,boolean enabled){ManageEditorialSource.Request toModel(){return new ManageEditorialSource.Request(name,accessMode,authorityLevel,endpoint,pollingFrequencySeconds,enabled);}}
 public record SourceAcceptedResponse(UUID sourceId) { }
 public record SourceListResponse(List<EditorialSourceStudioCatalog.Source> items) { }
 public record SignalListResponse(List<EditorialSourceStudioCatalog.Signal> items) { }
}
