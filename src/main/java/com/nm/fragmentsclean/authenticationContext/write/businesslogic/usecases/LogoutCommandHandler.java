package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;

public class LogoutCommandHandler implements CommandHandler<LogoutCommand> {

    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutCommandHandler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void execute(LogoutCommand command) {
        refreshTokenRepository.findByToken(command.refreshToken())
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
        // 👆 si le token n'existe pas déjà, on ne throw pas → logout idempotent
    }
}
