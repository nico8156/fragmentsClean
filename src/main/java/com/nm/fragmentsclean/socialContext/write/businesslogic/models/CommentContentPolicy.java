package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/** Pure domain policy applied consistently on comment creation and editing. */
public final class CommentContentPolicy {
    public static final int MAX_LENGTH = 4000;

    private final Set<String> forbiddenTerms;

    public CommentContentPolicy(Set<String> forbiddenTerms) {
        this.forbiddenTerms = forbiddenTerms.stream()
                .map(CommentContentPolicy::searchable)
                .filter(term -> !term.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public String validateAndNormalize(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            throw new BusinessCommandRejectedException("COMMENT_BODY_EMPTY", "Comment body cannot be empty");
        }
        String body = rawBody.strip();
        if (body.codePointCount(0, body.length()) > MAX_LENGTH) {
            throw new BusinessCommandRejectedException("COMMENT_BODY_TOO_LONG", "Comment body exceeds 4000 characters");
        }
        if (body.codePoints().anyMatch(CommentContentPolicy::isForbiddenControlCharacter)) {
            throw new BusinessCommandRejectedException("COMMENT_BODY_INVALID_CHARACTERS", "Comment body contains control characters");
        }
        String searchableBody = searchable(body);
        if (forbiddenTerms.stream().anyMatch(searchableBody::contains)) {
            throw new BusinessCommandRejectedException("COMMENT_BODY_FORBIDDEN_TERM", "Comment body violates community rules");
        }
        return body;
    }

    private static boolean isForbiddenControlCharacter(int codePoint) {
        return Character.isISOControl(codePoint) && codePoint != '\n' && codePoint != '\t';
    }

    private static String searchable(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).strip();
    }
}
