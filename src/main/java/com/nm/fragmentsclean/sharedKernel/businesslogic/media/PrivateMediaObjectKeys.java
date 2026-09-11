package com.nm.fragmentsclean.sharedKernel.businesslogic.media;

import java.util.UUID;

public final class PrivateMediaObjectKeys {
  private final String prefix;
  public PrivateMediaObjectKeys(String prefix) { this.prefix = normalize(prefix); }
  public String pendingExperience(UUID experienceId, UUID mediaId) { return prefix + "/pending/experiences/" + experienceId + "/" + mediaId; }
  public String experience(UUID experienceId, UUID mediaId) { return prefix + "/experiences/" + experienceId + "/" + mediaId + ".jpg"; }
  public String pendingAvatar(UUID userId, UUID mediaId) { return prefix + "/pending/avatars/" + userId + "/" + mediaId; }
  public String avatar(UUID userId, UUID mediaId) { return prefix + "/avatars/" + userId + "/" + mediaId + ".jpg"; }
  private static String normalize(String value) {
    String result = value == null || value.isBlank() ? "fragments/staging/private-media" : value.strip();
    while (result.startsWith("/")) result = result.substring(1);
    while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
    return result;
  }
}
