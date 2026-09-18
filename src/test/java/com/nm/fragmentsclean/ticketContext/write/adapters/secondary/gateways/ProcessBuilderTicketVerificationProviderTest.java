package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.ticketEngine.ProcessBuilderTicketVerificationProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;

class ProcessBuilderTicketVerificationProviderTest {

	@Test
	void happy_path_exit0_json_success() throws Exception {
		Path fake = createFakeCli(
				"""
						#!/usr/bin/env bash
						set -euo pipefail
						cat >/dev/null
						echo '{"schema":"ticketverify.v1","result":{"status":"ok","fields":{"total":{"value":4.0,"currency":"EUR"}}}}'
						exit 0
						""");

		var provider = new ProcessBuilderTicketVerificationProvider(
				new ObjectMapper(),
				List.of("/usr/bin/env", "bash", fake.toAbsolutePath().toString()),

				Duration.ofMillis(500));

		TicketVerificationProvider.Result res = provider.verify("TOTAL 4,00", null);

		assertThat(res).isInstanceOf(TicketVerificationProvider.Approved.class);
		var ok = (TicketVerificationProvider.Approved) res;
		assertThat(ok.amountCents()).isEqualTo(400);
		assertThat(ok.currency()).isEqualTo("EUR");
		assertThat(ok.providerTraceId()).startsWith("tv:");
	}

	@Test
	void blank_ocr_text_is_rejected_without_calling_ticketverify() throws Exception {
		Path fake = createFakeCli("""
				#!/usr/bin/env bash
				set -euo pipefail
				cat >/dev/null
				echo '{"schema":"ticketverify.v1","result":{"status":"ok","fields":{"total":{"value":4.0,"currency":"EUR"}}}}'
				exit 0
				""");

		var provider = new ProcessBuilderTicketVerificationProvider(
				new ObjectMapper(),
				List.of("/usr/bin/env", "bash", fake.toAbsolutePath().toString()),

				Duration.ofMillis(500));

		TicketVerificationProvider.Result res = provider.verify("", null);

		assertThat(res).isInstanceOf(TicketVerificationProvider.Rejected.class);
		var r = (TicketVerificationProvider.Rejected) res;
		assertThat(r.reasonCode()).isEqualTo("OCR_TEXT_MISSING");
		assertThat(r.message()).contains("ocrText");
	}

	@Test
	void ticketverify_partial_result_is_rejected() throws Exception {
		Path fake = createFakeCli(
				"""
						#!/usr/bin/env bash
						set -euo pipefail
						cat >/dev/null
						echo '{"schema":"ticketverify.v1","result":{"status":"partial","fields":{"total":{"value":4.0,"currency":"EUR"}}}}'
						exit 0
						""");

		var provider = new ProcessBuilderTicketVerificationProvider(
				new ObjectMapper(),
				List.of("/usr/bin/env", "bash", fake.toAbsolutePath().toString()),
				Duration.ofMillis(500));

		TicketVerificationProvider.Result res = provider.verify("TOTAL 4,00", null);

		assertThat(res).isInstanceOf(TicketVerificationProvider.Rejected.class);
		var rejected = (TicketVerificationProvider.Rejected) res;
		assertThat(rejected.reasonCode()).isEqualTo("PARTIAL_VERIFICATION");
	}

	@Test
	void timeout_kills_process_and_returns_failed_retryable() throws Exception {
		Path fake = createFakeCli("""
				#!/usr/bin/env bash
				set -euo pipefail
				cat >/dev/null
				sleep 2
				echo '{"schema":"ticketverify.v1","result":{"status":"ok"}}'
				exit 0
				""");

		var provider = new ProcessBuilderTicketVerificationProvider(
				new ObjectMapper(),
				List.of("/usr/bin/env", "bash", fake.toAbsolutePath().toString()),

				Duration.ofMillis(150));

		TicketVerificationProvider.Result res = provider.verify("TOTAL 1,00", null);

		assertThat(res).isInstanceOf(TicketVerificationProvider.FailedRetryable.class);
		var f = (TicketVerificationProvider.FailedRetryable) res;
		assertThat(f.message()).contains("timeout");
	}

	@Test
	void global_deadline_also_bounds_a_child_that_never_reads_stdin() throws Exception {
		Path fake = createFakeCli("""
				#!/usr/bin/env bash
				set -euo pipefail
				while :; do :; done
				""");
		var provider = provider(fake, Duration.ofMillis(150), 512_000, 4_096);
		String largeInput = "x".repeat(256_000);

		long startedAt = System.nanoTime();
		var result = provider.verify(largeInput, null);
		long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

		assertThat(result).isInstanceOf(TicketVerificationProvider.FailedRetryable.class);
		assertThat(((TicketVerificationProvider.FailedRetryable) result).message()).contains("timeout");
		assertThat(elapsedMillis).isLessThan(1_000);
	}

	@Test
	void rejects_an_input_over_the_limit_without_starting_the_child() throws Exception {
		Path marker = Files.createTempFile("ticketverify-not-started", ".marker");
		Files.delete(marker);
		Path fake = createFakeCli("""
				#!/usr/bin/env bash
				set -euo pipefail
				touch "$1"
				""");
		var provider = new ProcessBuilderTicketVerificationProvider(
				new ObjectMapper(),
				List.of("/usr/bin/env", "bash", fake.toAbsolutePath().toString(), marker.toString()),
				Duration.ofMillis(500), 8, 4_096);

		var result = provider.verify("input larger than eight bytes", null);

		assertThat(result).isInstanceOf(TicketVerificationProvider.Rejected.class);
		assertThat(((TicketVerificationProvider.Rejected) result).reasonCode()).isEqualTo("OCR_TEXT_TOO_LARGE");
		assertThat(marker).doesNotExist();
	}

	@Test
	void bounds_child_output_and_reaps_the_process() throws Exception {
		Path pidFile = Files.createTempFile("ticketverify", ".pid");
		Path fake = createFakeCli("""
				#!/usr/bin/env bash
				set -euo pipefail
				echo $$ > "$1"
				cat >/dev/null
				while :; do printf '0123456789abcdef'; done
				""");
		var provider = new ProcessBuilderTicketVerificationProvider(
				new ObjectMapper(),
				List.of("/usr/bin/env", "bash", fake.toAbsolutePath().toString(), pidFile.toString()),
				Duration.ofSeconds(1), 4_096, 128);

		var result = provider.verify("TOTAL 1,00", null);

		assertThat(result).isInstanceOf(TicketVerificationProvider.FailedRetryable.class);
		assertThat(((TicketVerificationProvider.FailedRetryable) result).message()).contains("byte limit");
		long pid = Long.parseLong(Files.readString(pidFile).trim());
		assertThatNoException().isThrownBy(() -> awaitNotAlive(pid));
	}

	private ProcessBuilderTicketVerificationProvider provider(
			Path fake, Duration timeout, int maxInputBytes, int maxOutputBytes) {
		return new ProcessBuilderTicketVerificationProvider(
				new ObjectMapper(),
				List.of("/usr/bin/env", "bash", fake.toAbsolutePath().toString()),
				timeout, maxInputBytes, maxOutputBytes);
	}

	private void awaitNotAlive(long pid) throws InterruptedException {
		for (int attempt = 0; attempt < 20; attempt++) {
			if (ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
				Thread.sleep(10);
			} else {
				return;
			}
		}
		throw new AssertionError("ticketverify process is still alive: " + pid);
	}

	private Path createFakeCli(String content) throws IOException {
		Path dir = Files.createTempDirectory("ticketverify-fake");
		Path file = dir.resolve("ticketverify_fake.sh");
		Files.writeString(file, content);
		file.toFile().setExecutable(true);

		// On exécute le script via /usr/bin/env bash pour être portable
		// => donc binaryPath = "/usr/bin/env", args = "bash script"
		// MAIS notre provider attend un "binaryPath" direct.
		// Solution simple : créer un wrapper exécutable qui est le script lui-même
		// (shebang).
		return file;
	}
}
