package com.nm.fragmentsclean.socialContextTest.unit;
import static org.assertj.core.api.Assertions.*;
import java.time.Instant;import java.util.UUID;import org.junit.jupiter.api.Test;
import com.nm.fragmentsclean.socialContext.read.AdminCommentCursor;
class AdminCommentCursorTest {
 @Test void preserves_timestamp_precision(){var cursor=new AdminCommentCursor(Instant.parse("2026-10-05T00:00:00.000900Z"),UUID.randomUUID());assertThat(AdminCommentCursor.parse(cursor.encode())).isEqualTo(cursor);}
 @Test void supports_no_cursor(){assertThat(AdminCommentCursor.parse(null)).isNull();assertThat(AdminCommentCursor.parse("")).isNull();}
 @Test void rejects_malformed_and_oversized_cursors(){for(String value:java.util.List.of("invalid","a".repeat(513)))assertThatThrownBy(()->AdminCommentCursor.parse(value)).isInstanceOf(IllegalArgumentException.class);}
}
