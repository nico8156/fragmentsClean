package com.nm.fragmentsclean.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReleaseManifestTest {
    private static final Path RELEASE = Path.of("src/main/resources/db/release");
    private static final Path RENDERER = Path.of("infra/aws/compose/platform/staging/fragments/render-release-migration.sh");
    @TempDir Path temporary;

    @Test void manifest_hash_is_stable_across_source_revisions_but_covers_every_fragment() throws Exception {
        copy();
        var first = render("a".repeat(40));
        assertThat(first.exit).isZero();
        assertThat(first.output).doesNotContain("\\ir ");
        var second = render("b".repeat(40));
        assertThat(second.exit).isZero();
        assertThat(first.output.lines().findFirst()).isEqualTo(second.output.lines().findFirst());
        var fragment = temporary.resolve("2026-09-11-account-lifecycle.sql");
        Files.writeString(fragment, Files.readString(fragment) + "\n-- Reviewed change\n");
        var changed = render("a".repeat(40));
        assertThat(changed.exit).isZero();
        assertThat(changed.output.lines().findFirst()).isNotEqualTo(first.output.lines().findFirst());
    }

    @Test void fragment_transaction_control_is_refused_before_any_sql_is_emitted() throws Exception {
        copy();
        var fragment = temporary.resolve("2026-09-11-account-lifecycle.sql");
        Files.writeString(fragment, "COMMIT;\n" + Files.readString(fragment));
        var result = render("a".repeat(40));
        assertThat(result.exit).isNotZero();
        assertThat(result.output).doesNotContain("\\set release_checksum", "CREATE TABLE");
    }

    @Test void escaping_the_manifest_directory_is_refused() throws Exception {
        copy();
        var driver = temporary.resolve("app-store-2026-09.psql");
        Files.writeString(driver, Files.readString(driver).replace("\\ir 2026-09-11-account-lifecycle.sql", "\\ir ../unexpected.sql"));
        assertThat(render("a".repeat(40)).exit).isNotZero();
    }

    private void copy() throws Exception {
        try (var files = Files.list(RELEASE)) {
            for (var file : files.toList()) Files.copy(file, temporary.resolve(file.getFileName()));
        }
    }

    private Result render(String revision) throws Exception {
        var process = new ProcessBuilder("bash", RENDERER.toAbsolutePath().toString(), temporary.toString(), revision)
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        return new Result(process.waitFor(), output);
    }
    private record Result(int exit, String output) {}
}
