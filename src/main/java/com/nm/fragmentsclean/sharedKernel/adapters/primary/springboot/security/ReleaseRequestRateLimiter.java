package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** Single-node release guard for costly and abuse-sensitive HTTP intentions. */
public final class ReleaseRequestRateLimiter {
    private final boolean enabled;
    private final List<Policy> policies;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public ReleaseRequestRateLimiter(boolean enabled, int ticketLimit, int mediaLimit, int ugcLimit) {
        this(enabled, List.of(
                policy("ticket", ticketLimit, Duration.ofMinutes(1), request ->
                        request.method().equals("POST") && request.path().equals("/api/tickets/verify")),
                policy("media", mediaLimit, Duration.ofMinutes(1), request ->
                        request.method().equals("POST") && isPrivateMediaWrite(request.path())),
                policy("ugc", ugcLimit, Duration.ofMinutes(1), request ->
                        isUserGeneratedContentWrite(request.method(), request.path()))));
    }

    ReleaseRequestRateLimiter(boolean enabled, List<Policy> policies) {
        this.enabled = enabled;
        this.policies = List.copyOf(policies);
    }

    public Decision acquire(String method, String path, String principal, Instant now) {
        Objects.requireNonNull(now);
        if (!enabled) return Decision.allowed();
        var request = new Request(normalize(method), normalizePath(path));
        var policy = policies.stream().filter(candidate -> candidate.matches().test(request)).findFirst().orElse(null);
        if (policy == null) return Decision.allowed();

        if (principal == null || principal.isBlank()) return Decision.allowed();
        String identity = "user:" + principal;
        String key = policy.name() + ":" + identity;
        var decision = new DecisionHolder();
        windows.compute(key, (ignored, current) -> {
            var active = current == null || !now.isBefore(current.endsAt())
                    ? new Window(now.plus(policy.window()), 0)
                    : current;
            if (active.count() >= policy.limit()) {
                decision.value = Decision.rejected(secondsUntil(active.endsAt(), now));
                return active;
            }
            decision.value = Decision.allowed();
            return new Window(active.endsAt(), active.count() + 1);
        });
        if (windows.size() > 10_000) windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().endsAt()));
        return decision.value;
    }

    private static boolean isPrivateMediaWrite(String path) {
        return path.equals("/api/users/me/avatar/upload-intents")
                || path.matches("/api/users/me/avatar/[^/]+/confirm")
                || path.matches("/api/experiences/[^/]+/media/upload-intents")
                || path.matches("/api/experiences/[^/]+/media/[^/]+/confirm");
    }

    private static boolean isUserGeneratedContentWrite(String method, String path) {
        if (!(method.equals("POST") || method.equals("PATCH") || method.equals("DELETE"))) return false;
        if (path.startsWith("/api/experiences/") && path.contains("/media/")) return false;
        return path.equals("/api/experiences") || path.startsWith("/api/experiences/")
                || path.startsWith("/api/social/comments") || path.equals("/api/social/blocks");
    }

    private static Policy policy(String name, int limit, Duration window, Predicate<Request> matches) {
        if (limit <= 0) throw new IllegalArgumentException(name + " rate limit must be positive");
        return new Policy(name, limit, window, matches);
    }

    private static long secondsUntil(Instant end, Instant now) {
        return Math.max(1, Duration.between(now, end).toSeconds());
    }

    private static String normalize(String value) { return value == null ? "" : value.toUpperCase(); }
    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) return "/";
        return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    record Policy(String name, int limit, Duration window, Predicate<Request> matches) { }
    record Request(String method, String path) { }
    private record Window(Instant endsAt, int count) { }
    private static final class DecisionHolder { private Decision value; }

    public record Decision(boolean permitted, long retryAfterSeconds) {
        static Decision allowed() { return new Decision(true, 0); }
        static Decision rejected(long retryAfterSeconds) { return new Decision(false, retryAfterSeconds); }
    }
}
