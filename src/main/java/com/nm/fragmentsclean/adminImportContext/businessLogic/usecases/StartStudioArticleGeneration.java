package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.*;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import java.text.Normalizer;
import java.util.Locale;

public final class StartStudioArticleGeneration {
    private final ArticleGenerationAuthoringPort port; private final UuidGenerator ids; private final DateTimeProvider clock;
    public StartStudioArticleGeneration(ArticleGenerationAuthoringPort port, UuidGenerator ids, DateTimeProvider clock) { this.port=port; this.ids=ids; this.clock=clock; }
    public StudioArticleGenerationResult execute(StudioArticleGenerationRequest request) {
        String subject=require(request.subject(),"subject"); String locale=request.locale()==null||request.locale().isBlank()?"fr-FR":request.locale().trim();
        var commandId=ids.generate(); var sagaId=ids.generate(); var articleId=ids.generate(); var revisionId=ids.generate();
        String slug=slug(subject)+"-"+articleId.toString().substring(0,8);
        port.requestGeneration(new StudioArticleGenerationCommand(commandId,clock.now(),sagaId,articleId,revisionId,subject,slug,locale,
                java.util.Objects.requireNonNull(request.operatorId(),"operatorId"),require(request.operatorName(),"operatorName")));
        return new StudioArticleGenerationResult(commandId,sagaId,articleId,revisionId,"ACCEPTED");
    }
    private static String slug(String value) { String normalized=Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)",""); return normalized.isBlank()?"article":normalized.substring(0,Math.min(80,normalized.length())); }
    private static String require(String value,String field) { if(value==null||value.isBlank()) throw new IllegalArgumentException(field+" is required"); return value.trim(); }
}
