package com.nm.fragmentsclean.experienceContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import java.util.Locale;
import java.util.Set;

public final class ExperienceContentPolicy {
    public static final int MAX_LENGTH = 4000;
    private final Set<String> forbiddenTerms;

    public ExperienceContentPolicy(Set<String> forbiddenTerms) {
        this.forbiddenTerms = forbiddenTerms == null ? Set.of() : forbiddenTerms.stream()
                .map(value -> value.toLowerCase(Locale.ROOT).strip())
                .filter(value -> !value.isBlank()).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public String normalizeDraft(String message) {
        if (message == null || message.isBlank()) return null;
        return validate(message);
    }

    public String requirePublishable(String message) {
        String normalized = normalizeDraft(message);
        if (normalized == null) {
            throw new BusinessCommandRejectedException("EXPERIENCE_EMPTY", "An experience needs a message before publication");
        }
        return normalized;
    }

    private String validate(String message) {
        String normalized = message.strip();
        if (normalized.length() > MAX_LENGTH) {
            throw new BusinessCommandRejectedException("EXPERIENCE_TOO_LONG", "Experience exceeds 4000 characters");
        }
        if (normalized.codePoints().anyMatch(code -> Character.isISOControl(code) && code != '\n' && code != '\t')) {
            throw new BusinessCommandRejectedException("EXPERIENCE_INVALID_CHARACTERS", "Experience contains invalid characters");
        }
        String folded = normalized.toLowerCase(Locale.ROOT);
        if (forbiddenTerms.stream().anyMatch(folded::contains)) {
            throw new BusinessCommandRejectedException("EXPERIENCE_CONTENT_REJECTED", "Experience violates community rules");
        }
        return normalized;
    }
}
