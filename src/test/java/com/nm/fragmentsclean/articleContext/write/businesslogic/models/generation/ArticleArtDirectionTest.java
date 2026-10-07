package com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation;
import org.junit.jupiter.api.Test;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleDomainException;
import static org.junit.jupiter.api.Assertions.*;
class ArticleArtDirectionTest {
    @Test void legacyRequestsKeepTheOriginalDirection() {
        assertEquals(ArticleArtDirection.ORIGINAL, ArticleArtDirection.from(null));
        assertEquals(ArticleArtDirection.ORIGINAL, ArticleArtDirection.from("  "));
    }
    @Test void explicitDirectionsAreNormalizedAndUnknownValuesAreRejected() {
        assertEquals(ArticleArtDirection.CONTEMPLATIVE, ArticleArtDirection.from(" contemplative "));
        assertThrows(ArticleDomainException.class, () -> ArticleArtDirection.from("unknown"));
    }
}
