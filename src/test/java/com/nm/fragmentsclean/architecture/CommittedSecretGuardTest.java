package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommittedSecretGuardTest {
  private static final Path SCRIPT =
      Path.of("scripts/validate-no-committed-secrets.sh").toAbsolutePath();

  @TempDir Path temporary;

  @Test
  void source_code_pem_delimiters_without_key_material_are_not_false_positives() throws Exception {
    Files.writeString(
        temporary.resolve("Parser.java"),
        "pem.replace(\"-----BEGIN PRIVATE KEY-----\", \"\");");

    assertThat(run().exit()).isZero();
  }

  @Test
  void actual_multiline_private_key_material_is_refused_without_printing_it() throws Exception {
    Files.writeString(
        temporary.resolve("leaked.pem"),
        "-----BEGIN PRIVATE KEY-----\n"
            + "A".repeat(64)
            + "\n-----END PRIVATE KEY-----\n");

    var result = run();

    assertThat(result.exit()).isNotZero();
    assertThat(result.output()).contains("leaked.pem").doesNotContain("A".repeat(64));
  }

  private Result run() throws Exception {
    var process =
        new ProcessBuilder("bash", SCRIPT.toString(), temporary.toString())
            .redirectErrorStream(true)
            .start();
    String output = new String(process.getInputStream().readAllBytes());
    return new Result(process.waitFor(), output);
  }

  private record Result(int exit, String output) {}
}
