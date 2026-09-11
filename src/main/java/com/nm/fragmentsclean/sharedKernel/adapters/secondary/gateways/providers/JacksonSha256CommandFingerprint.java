package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandFingerprint;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public final class JacksonSha256CommandFingerprint implements CommandFingerprint {
    private final ObjectMapper objectMapper;

    public JacksonSha256CommandFingerprint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String fingerprint(AuthenticatedCommand command) {
        try {
            byte[] canonicalPayload = objectMapper.writeValueAsString(command).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalPayload));
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Cannot fingerprint authenticated command", exception);
        }
    }
}
