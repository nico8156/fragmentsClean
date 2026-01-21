package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways;

import static org.assertj.core.api.Assertions.assertThat;

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
	void empty_input_exit2_json_error_becomes_rejected() throws Exception {
		Path fake = createFakeCli("""
				#!/usr/bin/env bash
				set -euo pipefail
				cat >/dev/null
				echo '{"ok":false,"error":{"code":"INPUT_EMPTY","message":"stdin is empty"}}'
				exit 2
				""");

		var provider = new ProcessBuilderTicketVerificationProvider(
				new ObjectMapper(),
				List.of("/usr/bin/env", "bash", fake.toAbsolutePath().toString()),

				Duration.ofMillis(500));

		TicketVerificationProvider.Result res = provider.verify("", null);

		assertThat(res).isInstanceOf(TicketVerificationProvider.Rejected.class);
		var r = (TicketVerificationProvider.Rejected) res;
		assertThat(r.reasonCode()).isEqualTo("INPUT_EMPTY");
		assertThat(r.message()).contains("empty");
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
