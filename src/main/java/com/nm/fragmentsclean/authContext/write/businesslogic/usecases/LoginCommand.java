package com.nm.fragmentsclean.authContext.write.businesslogic.usecases;

import java.util.List;

public record LoginCommand(
        String provider,
        String idToken,
        List<String> scopes
) {
}
