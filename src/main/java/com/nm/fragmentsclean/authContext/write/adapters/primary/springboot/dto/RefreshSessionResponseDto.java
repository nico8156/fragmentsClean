package com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto;

import java.util.List;

public record RefreshSessionResponseDto(
        AppUserDto user,
        AuthTokensDto tokens,
        String provider,
        List<String> scopes,
        String serverTime
) {
}
