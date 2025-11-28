package com.nm.fragmentsclean.authContext.read.adapters.primary.springboot.dto;

import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.AppUserDto;


public record CurrentUserResponseDto(
        AppUserDto user,
        String serverTime
) {
}
