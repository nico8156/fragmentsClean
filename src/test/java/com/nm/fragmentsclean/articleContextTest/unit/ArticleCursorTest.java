package com.nm.fragmentsclean.articleContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.articleContext.read.projections.ArticleCursor;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArticleCursorTest {
    private static final Instant PUBLISHED_AT = Instant.parse("2026-09-05T08:00:00Z");
    private static final UUID ARTICLE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void round_trips_an_opaque_next_cursor() {
        ArticleCursor cursor = ArticleCursor.next(PUBLISHED_AT, ARTICLE_ID);

        assertThat(ArticleCursor.decode(cursor.encode())).contains(cursor);
        assertThat(cursor.encode()).doesNotContain(PUBLISHED_AT.toString(), ARTICLE_ID.toString());
    }

    @Test
    void round_trips_an_opaque_previous_cursor() {
        ArticleCursor cursor = ArticleCursor.previous(PUBLISHED_AT, ARTICLE_ID);

        assertThat(ArticleCursor.decode(cursor.encode())).contains(cursor);
    }

    @Test
    void rejects_malformed_or_unsupported_cursors_without_exposing_parser_errors() {
        assertThat(ArticleCursor.decode("10")).isEmpty();
        assertThat(ArticleCursor.decode("not-base64!")).isEmpty();
        assertThat(ArticleCursor.decode(null)).isEmpty();
    }
}
