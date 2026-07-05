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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;

public class ProcessBuilderTicketVerificationProvider implements TicketVerificationProvider {
	private static final Logger log = LoggerFactory.getLogger(TicketVerificationProvider.class);

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
		// ticketverify is a text-only engine. imageRef belongs to the capture/OCR flow.
		String input = (ocrText == null) ? "" : ocrText;
		if (!input.endsWith("\n"))
			input += "\n"; // utile

		String traceId = "tv:" + UUID.randomUUID();
		if (ocrText == null || ocrText.isBlank()) {
			return new Rejected("OCR_TEXT_MISSING", "ocrText is required for now", traceId);
		}

		Process process = null;
		List<String> cmd = new ArrayList<>(command);
		log.debug("[ticketverify] binary={}", cmd.get(0));

		cmd.addAll(List.of("--schema", "v1", "--format", "json"));

		try {
			ProcessBuilder pb = new ProcessBuilder(
					cmd);
			pb.redirectErrorStream(false);

			process = pb.start();
			// IMPORTANT: écrire + FLUSH + CLOSE => envoie EOF au binaire
			try (var os = process.getOutputStream()) {
				writeUtf8(os, input); // writeUtf8 doit écrire en UTF-8
				os.flush();
			}

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
			tOut.join();
			tErr.join();

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
				// Input validation errors are explicit business rejections for this command.
				return mapErrorAsRejected(root, traceId);
			}
			if (exit != 0) {
				log.warn("[ticketverify] failed exit={} stdoutLength={} stderrLength={} traceId={}",
						exit, stdout.length(), stderr.length(), traceId);
				return new FailedRetryable(
						"ticketverify failed (exit=" + exit + ") stderr="
								+ truncate(stderr),
						traceId);
			}

			// Defensive fallback. Non-zero technical exits normally return above.
			String errMsg = null;
			JsonNode errNode = root.path("error");
			if (!errNode.isMissingNode()) {
				errMsg = errNode.path("message").asText(null);
			}
			if (errMsg == null) {
				errMsg = "ticketverify returned no structured error (stdoutLength=" + stdout.length() + ")";
			}

			return new FailedRetryable("ticketverify failed (exit=" + exit + ") " + errMsg, traceId);
		} catch (Exception e) {
			return new FailedRetryable("ticketverify exception: " + e.getMessage(), traceId);
		} finally {
			log.debug("[ticketverify] process ended traceId={}", traceId);
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
		// "confidence":..., "fields":{ "total":{...}, "merchant":{...}, ... },
		// "warnings":[...] } }
		JsonNode result = root.path("result");
		String status = result.path("status").asText(null);

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

		if ("partial".equalsIgnoreCase(status)) {
			return new Rejected("PARTIAL_VERIFICATION", "ticketverify returned a partial result", traceId);
		}

		if (!"ok".equalsIgnoreCase(status)) {
			return new FailedRetryable("ticketverify returned unsupported status: " + status, traceId);
		}

		// ok => Approved minimal
		JsonNode fields = result.path("fields");

		// TOTAL
		JsonNode total = fields.path("total");
		Double value = total.path("value").isNumber() ? total.path("value").asDouble() : null;
		String currency = total.path("currency").asText(null);

		int amountCents = (value == null) ? 0 : (int) Math.round(value * 100.0);
		if (currency == null)
			currency = "EUR";

		// MERCHANT (NEW)
		JsonNode merchantNode = fields.path("merchant");
		String merchantName = merchantNode.path("value").asText(null);
		if (merchantName != null) {
			merchantName = merchantName.trim();
			if (merchantName.isEmpty())
				merchantName = null;
		}

		// DATETIME (optionnel) — pour l’instant ton moteur sort un string, pas
		// forcément ISO.
		// Donc on le garde null tant que ce n’est pas garanti.
		Instant ticketDate = null;
		// Si un jour tu sors un ISO 8601 strict côté engine, tu pourras activer ça :
		// String datetimeIso = fields.path("datetime").path("value").asText(null);
			// if (datetimeIso != null && !datetimeIso.isBlank()) ticketDate =
			// Instant.parse(datetimeIso);
			log.debug("[ticketverify] parsed result traceId={} hasTotal={} hasMerchant={}",
					traceId, value != null, merchantName != null);

		return new Approved(
				amountCents,
				currency,
				ticketDate,
				merchantName, // <-- ici
				null, // merchantAddress
				null, // paymentMethod
				List.of(), // lineItems
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

	private String truncate(String value) {
		if (value == null || value.length() <= 200) {
			return value;
		}
		return value.substring(0, 200) + "...";
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
