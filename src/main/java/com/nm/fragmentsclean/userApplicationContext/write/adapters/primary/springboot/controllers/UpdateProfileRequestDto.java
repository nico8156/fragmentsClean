package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.controllers;

import java.util.UUID;

public record UpdateProfileRequestDto(UUID commandId, String displayName) {}
