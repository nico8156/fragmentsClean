package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReleaseWorkflowGuardrailTest {
  private static final Path CI = Path.of(".github/workflows/backend-ci.yml");
  private static final Path DEPLOY = Path.of(".github/workflows/deploy-staging-backend.yml");

  @Test
  void pull_requests_and_main_use_the_same_backend_gate_as_staging_promotion() throws Exception {
    String ci = Files.readString(CI);
    String deploy = Files.readString(DEPLOY);

    assertThat(ci)
        .contains("pull_request:", "push:", "branches: [main]", "bash scripts/verify-backend-ci.sh")
        .contains("java-version: '21'");
    assertThat(deploy)
        .contains("bash scripts/verify-backend-ci.sh")
        .contains("bash scripts/verify-release-health.sh");
  }
}
