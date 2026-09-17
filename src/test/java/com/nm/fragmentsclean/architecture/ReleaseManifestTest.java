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

    @Test void article_curation_manifest_is_rendered_and_hashes_its_sql_fragment() throws Exception {
        copy();
        var first = render("a".repeat(40), "article-curation-2026-09.psql");
        assertThat(first.exit).isZero();
        assertThat(first.output).contains("article-curation-2026-09", "ALTER TABLE articles")
                .doesNotContain("\\ir ");
        var fragment = temporary.resolve("2026-09-15-article-curation.sql");
        Files.writeString(fragment, Files.readString(fragment) + "\n-- Reviewed change\n");
        var changed = render("a".repeat(40), "article-curation-2026-09.psql");
        assertThat(changed.exit).isZero();
        assertThat(changed.output.lines().findFirst()).isNotEqualTo(first.output.lines().findFirst());
    }

    @Test void messaging_safety_manifest_is_rendered_and_hashes_its_sql_fragment() throws Exception {
        copy();
        var first = render("a".repeat(40), "messaging-safety-2026-09.psql");
        assertThat(first.exit).isZero();
        assertThat(first.output).contains("messaging-safety-2026-09", "lease_owner")
                .doesNotContain("\\ir ");
        var fragment = temporary.resolve("2026-09-17-inbox-lease-owner.sql");
        Files.writeString(fragment, Files.readString(fragment) + "\n-- Reviewed change\n");
        var changed = render("a".repeat(40), "messaging-safety-2026-09.psql");
        assertThat(changed.exit).isZero();
        assertThat(changed.output.lines().findFirst()).isNotEqualTo(first.output.lines().findFirst());
    }

    @Test void account_erasure_safety_manifest_is_rendered_and_hashes_its_sql_fragment() throws Exception {
        copy();
        var first = render("a".repeat(40), "account-erasure-safety-2026-09.psql");
        assertThat(first.exit).isZero();
        assertThat(first.output).contains("account-erasure-safety-2026-09", "account_erasure_barriers")
                .doesNotContain("\\ir ");
        var fragment = temporary.resolve("2026-09-17-account-erasure-barriers.sql");
        Files.writeString(fragment, Files.readString(fragment) + "\n-- Reviewed change\n");
        var changed = render("a".repeat(40), "account-erasure-safety-2026-09.psql");
        assertThat(changed.exit).isZero();
        assertThat(changed.output.lines().findFirst()).isNotEqualTo(first.output.lines().findFirst());
    }

    @Test void projection_sync_audience_manifest_is_rendered_and_hashes_its_sql_fragment() throws Exception {
        copy();
        var first = render("a".repeat(40), "projection-sync-audience-2026-09.psql");
        assertThat(first.exit).isZero();
        assertThat(first.output).contains("projection-sync-audience-2026-09", "recipient_id")
                .doesNotContain("\\ir ");
        var fragment = temporary.resolve("2026-09-17-projection-sync-audience.sql");
        Files.writeString(fragment, Files.readString(fragment) + "\n-- Reviewed change\n");
        var changed = render("a".repeat(40), "projection-sync-audience-2026-09.psql");
        assertThat(changed.exit).isZero();
        assertThat(changed.output.lines().findFirst()).isNotEqualTo(first.output.lines().findFirst());
    }

    @Test void refresh_token_hardening_manifest_is_rendered_and_hashes_its_sql_fragment() throws Exception {
        copy();
        var first = render("a".repeat(40), "refresh-token-hardening-2026-09.psql");
        assertThat(first.exit).isZero();
        assertThat(first.output).contains("refresh-token-hardening-2026-09", "token_hash")
                .doesNotContain("\\ir ");
        var fragment = temporary.resolve("2026-09-17-refresh-token-hashing.sql");
        Files.writeString(fragment, Files.readString(fragment) + "\n-- Reviewed change\n");
        var changed = render("a".repeat(40), "refresh-token-hardening-2026-09.psql");
        assertThat(changed.exit).isZero();
        assertThat(changed.output.lines().findFirst()).isNotEqualTo(first.output.lines().findFirst());
    }

    @Test void logout_revocation_manifest_is_rendered_and_hashes_its_sql_fragment() throws Exception {
        copy();
        var first = render("a".repeat(40), "logout-revocation-2026-09.psql");
        assertThat(first.exit).isZero();
        assertThat(first.output).contains("logout-revocation-2026-09", "family_id")
                .doesNotContain("\\ir ");
        var fragment = temporary.resolve("2026-09-17-refresh-token-family.sql");
        Files.writeString(fragment, Files.readString(fragment) + "\n-- Reviewed change\n");
        var changed = render("a".repeat(40), "logout-revocation-2026-09.psql");
        assertThat(changed.exit).isZero();
        assertThat(changed.output.lines().findFirst()).isNotEqualTo(first.output.lines().findFirst());
    }

    @Test void outbox_delivery_manifest_is_rendered_and_hashes_its_sql_fragment() throws Exception {
        copy();
        var first = render("a".repeat(40), "outbox-delivery-2026-09.psql");
        assertThat(first.exit).isZero();
        assertThat(first.output).contains("outbox-delivery-2026-09", "next_attempt_at", "lease_owner")
                .doesNotContain("\\ir ");
        var fragment = temporary.resolve("2026-09-17-outbox-delivery.sql");
        Files.writeString(fragment, Files.readString(fragment) + "\n-- Reviewed change\n");
        var changed = render("a".repeat(40), "outbox-delivery-2026-09.psql");
        assertThat(changed.exit).isZero();
        assertThat(changed.output.lines().findFirst()).isNotEqualTo(first.output.lines().findFirst());
    }

    private void copy() throws Exception {
        try (var files = Files.list(RELEASE)) {
            for (var file : files.toList()) Files.copy(file, temporary.resolve(file.getFileName()));
        }
    }

    private Result render(String revision) throws Exception {
        return render(revision, "app-store-2026-09.psql");
    }

    private Result render(String revision, String driver) throws Exception {
        var process = new ProcessBuilder("bash", RENDERER.toAbsolutePath().toString(), temporary.toString(), revision, driver)
                .redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        return new Result(process.waitFor(), output);
    }
    private record Result(int exit, String output) {}
}
