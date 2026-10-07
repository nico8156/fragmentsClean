package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.StudioArticleGenerationCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.StartStudioArticleGeneration;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudioArticleArtDirectionWebTest {
    @Test void acceptsAnExplicitMoodAndLegacyRequestsThroughTheExistingEndpoint() throws Exception {
        var captured = new AtomicReference<StudioArticleGenerationCommand>();
        var start = new StartStudioArticleGeneration(captured::set, UUID::randomUUID, () -> Instant.parse("2026-10-07T10:00:00Z"));
        var mvc = MockMvcBuilders.standaloneSetup(new AdminStudioArticleGenerationController(start, null, null, null)).build();
        var principal = new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null);
        mvc.perform(post("/api/admin/studio/article-generations").principal(principal).contentType("application/json")
                .content("{\"subject\":\"Café et silence\",\"locale\":\"fr-FR\",\"artDirection\":\"CONTEMPLATIVE\"}")).andExpect(status().isAccepted());
        assertThat(captured.get().artDirection()).isEqualTo("CONTEMPLATIVE");
        mvc.perform(post("/api/admin/studio/article-generations").principal(principal).contentType("application/json")
                .content("{\"subject\":\"Café et silence\"}")).andExpect(status().isAccepted());
        assertThat(captured.get().artDirection()).isNull();
    }
}
