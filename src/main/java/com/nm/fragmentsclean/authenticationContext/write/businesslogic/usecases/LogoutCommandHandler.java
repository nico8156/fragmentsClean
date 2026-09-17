package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import org.springframework.transaction.annotation.Transactional;

public class LogoutCommandHandler implements CommandHandler<LogoutCommand> {

    private final RefreshTokenRepository refreshTokenRepository;

    public LogoutCommandHandler(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public void execute(LogoutCommand command) {
        if (command == null || command.refreshToken() == null || command.refreshToken().isBlank()) {
            return;
        }
        var familyId = refreshTokenRepository.findFamilyIdByToken(command.refreshToken());
        if (familyId.isEmpty()) return;
        refreshTokenRepository.lockFamily(familyId.get());
        refreshTokenRepository.findFamilyByTokenForUpdate(command.refreshToken()).forEach(token -> {
            if (!token.revoked()) {
                token.revoke();
                refreshTokenRepository.save(token);
            }
        });
    }
}
