package com.nm.fragmentsclean.authContext.read.adapters.primary.springboot.dto.mapper;

import com.nm.fragmentsclean.authContext.read.adapters.primary.springboot.dto.CurrentUserResponseDto;
import com.nm.fragmentsclean.authContext.read.usecases.GetCurrentUserResult;

import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.AppUserDto;
import com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto.mapper.RefreshSessionDtoMapper;

public final class CurrentUserDtoMapper {

    private CurrentUserDtoMapper() {
    }

    public static CurrentUserResponseDto toResponse(GetCurrentUserResult result) {

        AppUserDto userDto = RefreshSessionDtoMapper.toAppUserDto(
                result.userSnapshot()
        );

        return new CurrentUserResponseDto(
                userDto,
                result.serverTime().toString()
        );
    }
}
