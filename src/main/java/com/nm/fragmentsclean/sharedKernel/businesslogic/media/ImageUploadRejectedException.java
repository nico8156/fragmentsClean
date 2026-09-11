package com.nm.fragmentsclean.sharedKernel.businesslogic.media;

public final class ImageUploadRejectedException extends RuntimeException {
  private final String code;

  public ImageUploadRejectedException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() { return code; }
}
