package com.nm.fragmentsclean.sharedKernel.businesslogic.media;

public final class PrivateMediaReferences {
  private static final String AVATAR = "media:avatar:";
  private static final String EXPERIENCE = "media:experience:";
  private PrivateMediaReferences() {}
  public static String avatar(String objectKey) { return AVATAR + objectKey; }
  public static String experience(String objectKey) { return EXPERIENCE + objectKey; }
  public static String objectKey(String reference) {
    if (reference == null) return null;
    if (reference.startsWith(AVATAR)) return reference.substring(AVATAR.length());
    if (reference.startsWith(EXPERIENCE)) return reference.substring(EXPERIENCE.length());
    return null;
  }
}
