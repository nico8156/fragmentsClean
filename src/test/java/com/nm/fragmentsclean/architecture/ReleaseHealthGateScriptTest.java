package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReleaseHealthGateScriptTest {
  private static final Path SCRIPT = Path.of("scripts/verify-release-health.sh").toAbsolutePath();

  @TempDir Path temporary;

  @Test
  void accepts_only_an_up_release_group_with_every_required_component() throws Exception {
    var health = write("healthy.json", """
        {
          "status": "UP",
          "components": {
            "db": {"status": "UP"},
            "messagingRuntimeHealth": {"status": "UP"},
            "articleAuthoringHealth": {"status": "UP"},
            "ticketVerificationHealth": {"status": "UP"},
            "editorialOperationsHealth": {"status": "UP"}
          }
        }
        """);

    assertThat(run(health).exit()).isZero();
  }

  @Test
  void refuses_a_degraded_component_even_if_the_envelope_claims_up() throws Exception {
    var health = write("degraded.json", """
        {
          "status": "UP",
          "components": {
            "db": {"status": "UP"},
            "messagingRuntimeHealth": {"status": "DEGRADED"},
            "articleAuthoringHealth": {"status": "UP"},
            "ticketVerificationHealth": {"status": "UP"},
            "editorialOperationsHealth": {"status": "UP"}
          }
        }
        """);

    var result = run(health);

    assertThat(result.exit()).isNotZero();
    assertThat(result.output()).contains("messagingRuntimeHealth=DEGRADED");
  }

  @Test
  void refuses_a_missing_required_component() throws Exception {
    var health = write("missing.json", """
        {
          "status": "UP",
          "components": {
            "db": {"status": "UP"},
            "messagingRuntimeHealth": {"status": "UP"},
            "articleAuthoringHealth": {"status": "UP"},
            "ticketVerificationHealth": {"status": "UP"}
          }
        }
        """);

    var result = run(health);

    assertThat(result.exit()).isNotZero();
    assertThat(result.output()).contains("editorialOperationsHealth=MISSING");
  }

  private Path write(String name, String content) throws Exception {
    return Files.writeString(temporary.resolve(name), content);
  }

  private static Result run(Path health) throws Exception {
    var process =
        new ProcessBuilder("bash", SCRIPT.toString(), health.toString())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes());
    return new Result(process.waitFor(), output);
  }

  private record Result(int exit, String output) {}
}
