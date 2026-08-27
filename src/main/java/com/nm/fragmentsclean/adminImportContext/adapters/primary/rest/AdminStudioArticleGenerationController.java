package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.*;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StartStudioArticleGeneration;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/studio/article-generations")
public final class AdminStudioArticleGenerationController {
    private final StartStudioArticleGeneration start;
    public AdminStudioArticleGenerationController(StartStudioArticleGeneration start){this.start=start;}
    @PostMapping public ResponseEntity<StudioArticleGenerationResult> generate(@RequestBody Request body, Authentication auth){
        UUID operatorId=UUID.fromString(auth.getName()); String operatorName=auth.getName();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(start.execute(new StudioArticleGenerationRequest(body.subject(),body.locale(),operatorId,operatorName)));
    }
    public record Request(String subject,String locale) { }
}
