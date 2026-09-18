package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReleaseWorkflowGuardrailTest {
  private static final Path CI = Path.of(".github/workflows/backend-ci.yml");
  private static final Path DEPLOY = Path.of(".github/workflows/deploy-staging-backend.yml");
  private static final Path DOCKERFILE = Path.of("Dockerfile");

  @Test
  void pull_requests_and_main_use_the_same_backend_gate_as_staging_promotion() throws Exception {
    String ci = Files.readString(CI);
    String deploy = Files.readString(DEPLOY);

    assertThat(ci)
        .contains("pull_request:", "push:", "branches: [main]", "bash scripts/verify-backend-ci.sh")
        .contains(
            "java-version: '21'",
            "scan-type: sbom",
            "scan-ref: target/bom.json",
            "severity: HIGH,CRITICAL",
            "exit-code: '1'");
    assertThat(deploy)
        .contains(
            "bash scripts/verify-backend-ci.sh",
            "scan-type: image",
            "severity: HIGH,CRITICAL",
            "exit-code: '1'",
            "--load",
            "docker push")
        .contains("bash scripts/verify-release-health.sh");
    assertThat(ci).doesNotContain("@v");
    assertThat(deploy).doesNotContain("@v");
    assertImmutableActionReferences(ci);
    assertImmutableActionReferences(deploy);
  }

  @Test
  void container_base_images_are_pinned_by_digest() throws Exception {
    List<String> baseImages =
        Files.readAllLines(DOCKERFILE).stream()
            .map(String::trim)
            .filter(line -> line.startsWith("FROM "))
            .toList();

    assertThat(baseImages).hasSize(3).allMatch(line -> line.contains("@sha256:"));
  }

  private static void assertImmutableActionReferences(String workflow) {
    List<String> actionReferences =
        workflow.lines().map(String::trim).filter(line -> line.startsWith("uses: ")).toList();

    assertThat(actionReferences)
        .isNotEmpty()
        .allMatch(line -> line.matches("uses: [^@\\s]+@[0-9a-f]{40}(?:\\s+#.*)?"));
  }
}
