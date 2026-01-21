package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.ticketEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;

public class ProcessBuilderTicketVerificationProvider implements TicketVerificationProvider {

	private final ObjectMapper objectMapper;
	private final List<String> command;
	private final Duration timeout;

	public ProcessBuilderTicketVerificationProvider(ObjectMapper objectMapper,
			List<String> command,
			Duration timeout) {
		this.objectMapper = objectMapper;
		this.command = List.copyOf(command);
		this.timeout = timeout;
	}

	@Override
	public Result verify(String ocrText, String imageRef) {
		// MVP: on se base sur ocrText; imageRef ignoré côté CLI (pour l’instant)
		String input = (ocrText == null) ? "" : ocrText;

		String traceId = "tv:" + UUID.randomUUID();

		Process process = null;
		List<String> cmd = new ArrayList<>(command);
		cmd.addAll(List.of("--schema", "v1", "--format", "json"));
		try {
			ProcessBuilder pb = new ProcessBuilder(
					cmd);
			pb.redirectErrorStream(false);

			process = pb.start();

			writeUtf8(process.getOutputStream(), input);

			// lire stdout/stderr en parallèle pour éviter deadlocks
			StreamCollector outCollector = new StreamCollector(process.getInputStream());
			StreamCollector errCollector = new StreamCollector(process.getErrorStream());

			Thread tOut = new Thread(outCollector, "ticketverify-stdout");
			Thread tErr = new Thread(errCollector, "ticketverify-stderr");
			tOut.start();
			tErr.start();

			boolean finished = process.waitFor(timeout.toMillis(),
					java.util.concurrent.TimeUnit.MILLISECONDS);
			if (!finished) {
				process.destroyForcibly();
				return new FailedRetryable("ticketverify timeout after " + timeout.toMillis() + "ms",
						traceId);
			}

			int exit = process.exitValue();

			// s’assurer que les threads ont fini
			tOut.join(200);
			tErr.join(200);

			String stdout = outCollector.getText();
			String stderr = errCollector.getText();

			if (stdout == null)
				stdout = "";
			if (stderr == null)
				stderr = "";

			// stdout doit être JSON ; si ce n’est pas le cas => internal
			JsonNode root = safeParseJson(stdout);
			if (root == null) {
				return new FailedRetryable("ticketverify returned non-JSON stdout (exit=" + exit + ")",
						traceId);
			}

			if (exit == 0) {
				return mapSuccess(root, traceId);
			}

			if (exit == 2) {
				// validation/input/args : côté métier -> Rejected explicable
				return mapErrorAsRejected(root, traceId);
			}

			// exit 3 ou autre : interne => retryable par défaut
			return new FailedRetryable("ticketverify failed (exit=" + exit + ")", traceId);

		} catch (Exception e) {
			return new FailedRetryable("ticketverify exception: " + e.getMessage(), traceId);
		} finally {
			if (process != null) {
				try {
					process.getInputStream().close();
				} catch (Exception ignored) {
				}
				try {
					process.getErrorStream().close();
				} catch (Exception ignored) {
				}
				try {
					process.getOutputStream().close();
				} catch (Exception ignored) {
				}
			}
		}
	}

	private void writeUtf8(OutputStream os, String input) throws IOException {
		try (OutputStreamWriter w = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
			w.write(input);
			w.flush();
		}
	}

	private JsonNode safeParseJson(String stdout) {
		try {
			if (stdout == null)
				return null;
			String s = stdout.trim();
			if (s.isEmpty())
				return null;
			return objectMapper.readTree(s);
		} catch (Exception e) {
			return null;
		}
	}

	private Result mapErrorAsRejected(JsonNode root, String traceId) {
		// CLI error shape: {"ok":false,"error":{"code":"...","message":"..."}}
		JsonNode err = root.path("error");
		String code = textOrNull(err, "code");
		String msg = textOrNull(err, "message");

		if (code == null)
			code = "VALIDATION_ERROR";
		if (msg == null)
			msg = "ticketverify validation error";

		return new Rejected(code, msg, traceId);
	}

	private Result mapSuccess(JsonNode root, String traceId) {
		// Ton JSON actuel ressemble à:
		// { "schema":"ticketverify.v1", "result": { "status":"partial|ok|reject",
		// "confidence":..., "fields":{ "total":{...}} , ... } }
		JsonNode result = root.path("result");
		String status = result.path("status").asText(null);

		// Si pas de status, on considère interne
		if (status == null) {
			return new FailedRetryable("ticketverify missing result.status", traceId);
		}

		if ("reject".equalsIgnoreCase(status)) {
			String reasonCode = "REJECT";
			String message = firstWarningMessage(result);
			if (message == null)
				message = "ticket rejected by ticketverify";
			return new Rejected(reasonCode, message, traceId);
		}

		// ok ou partial => Approved minimal
		// MVP mapping: total.value + currency
		JsonNode total = result.path("fields").path("total");
		Double value = total.path("value").isNumber() ? total.path("value").asDouble() : null;
		String currency = total.path("currency").asText(null);

		int amountCents = (value == null) ? 0 : (int) Math.round(value * 100.0);
		if (currency == null)
			currency = "EUR";

		// MVP: pas de date/merchant/payment/items => null/empty
		Instant ticketDate = null;

		return new Approved(
				amountCents,
				currency,
				ticketDate,
				null,
				null,
				null,
				List.of(),
				traceId);
	}

	private String firstWarningMessage(JsonNode result) {
		JsonNode warnings = result.path("warnings");
		if (warnings != null && warnings.isArray()) {
			for (JsonNode w : warnings) {
				String msg = w.path("message").asText(null);
				if (msg != null && !msg.isBlank())
					return msg;
			}
		}
		return null;
	}

	private String textOrNull(JsonNode node, String field) {
		if (node == null)
			return null;
		JsonNode v = node.get(field);
		if (v == null || v.isNull())
			return null;
		String s = v.asText();
		return (s == null || s.isBlank()) ? null : s;
	}

	private static class StreamCollector implements Runnable {
		private final InputStream is;
		private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		private StreamCollector(InputStream is) {
			this.is = is;
		}

		@Override
		public void run() {
			try {
				byte[] buf = new byte[4096];
				int n;
				while ((n = is.read(buf)) >= 0) {
					buffer.write(buf, 0, n);
				}
			} catch (IOException ignored) {
			}
		}

		public String getText() {
			return buffer.toString(StandardCharsets.UTF_8);
		}
	}
}
