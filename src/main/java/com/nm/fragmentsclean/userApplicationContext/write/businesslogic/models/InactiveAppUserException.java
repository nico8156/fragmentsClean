package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

public final class InactiveAppUserException extends RuntimeException {
  public InactiveAppUserException() {
    super("User profile is no longer active");
  }
}
