package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.*;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StartStudioArticleGenerationTest {
    @Test void createsStableIdentitiesAndDispatchesOneGenerationIntent() {
        var values=new ArrayDeque<>(List.of(uuid(1),uuid(2),uuid(3),uuid(4)));
        var fake=new FakePort(); DateTimeProvider clock=()->Instant.parse("2026-08-27T18:00:00Z");
        var useCase=new StartStudioArticleGeneration(fake,values::removeFirst,clock);
        var result=useCase.execute(new StudioArticleGenerationRequest("Comprendre le café filtre","fr-FR",uuid(9),"Nicolas"));
        assertEquals(uuid(2),result.sagaId()); assertEquals(uuid(3),result.articleId()); assertEquals("ACCEPTED",result.status());
        assertEquals("comprendre-le-cafe-filtre-00000000",fake.command.slug()); assertEquals(uuid(4),fake.command.revisionId());
    }
    private static UUID uuid(int value){return UUID.fromString("00000000-0000-0000-0000-00000000000"+value);}
    private static final class FakePort implements ArticleGenerationAuthoringPort { StudioArticleGenerationCommand command; public void requestGeneration(StudioArticleGenerationCommand command){this.command=command;} }
}
