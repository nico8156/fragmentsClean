package com.nm.fragmentsclean.aticleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleDomainException;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArticleEditorialPolicyTest {
    @Test void acceptsTheAgreedEditorialShape() {
        var draft = GeneratedArticleDraft.from("Comprendre le café", "Une introduction claire.", "Une conclusion utile.",
                "Une couverture colorée", List.of(section(1), section(2), section(3)),
                List.of(ArticleEditorialTag.CULTURE_CAFE, ArticleEditorialTag.DECOUVERTE));
        assertEquals(3, draft.sections().size());
        assertEquals(4, 1 + draft.sections().size());
    }

    @Test void rejectsTooFewOrTooManySections() {
        assertThrows(ArticleDomainException.class, () -> GeneratedArticleDraft.from("Titre", "Intro", "Conclusion", "Couverture",
                List.of(section(1), section(2)), List.of(ArticleEditorialTag.FUN)));
    }

    private GeneratedArticleSection section(int index) {
        return GeneratedArticleSection.from("Section " + index, "Paragraphe éditorial " + index, "Illustration " + index);
    }
}
