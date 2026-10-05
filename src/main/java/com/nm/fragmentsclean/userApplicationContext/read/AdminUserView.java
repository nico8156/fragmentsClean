package com.nm.fragmentsclean.userApplicationContext.read;
import java.time.Instant;import java.util.UUID;
public record AdminUserView(UUID userId,String displayName,String avatarUrl,String status,Instant createdAt,Instant updatedAt) {}
