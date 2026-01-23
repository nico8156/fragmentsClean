package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.ticketEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
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
		// MVP: on se base sur ocrText; imageRef ignoré côté CLI (pour l’instant)
		String input = (ocrText == null) ? "" : ocrText;
		if (!input.endsWith("\n"))
			input += "\n"; // utile

		String traceId = "tv:" + UUID.randomUUID();
		if (ocrText == null || ocrText.isBlank()) {
			return new Rejected("OCR_TEXT_MISSING", "ocrText is required for now", traceId);
		}

		Process process = null;
		List<String> cmd = new ArrayList<>(command);
		log.info("[ticketverify] cmd={}", cmd);
		log.info("[ticketverify] binary={}", cmd.get(0));

		cmd.addAll(List.of("--schema", "v1", "--format", "json"));
		// check what text is sent !
		debugDumpInput(input, traceId);

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
				// validation/input/args : côté métier -> Rejected explicable
				return mapErrorAsRejected(root, traceId);
			}
			if (exit != 0) {
				log.info("[ticketverify] exit={} stdout={} stderr={}", exit, stdout, stderr);
				return new FailedRetryable(
						"ticketverify failed (exit=" + exit + ") stderr="
								+ stderr,
						traceId);
			}

			// exit 3 ou autre : interne => retryable par défaut
			String errMsg = null;
			JsonNode errNode = root.path("error");
			if (!errNode.isMissingNode()) {
				errMsg = errNode.path("message").asText(null);
			}
			if (errMsg == null) {
				// parfois c’est sous result/warnings etc., mais au moins on log stdout brut
				errMsg = "stdout=" + stdout;
			}

			return new FailedRetryable("ticketverify failed (exit=" + exit + ") " + errMsg, traceId);
		} catch (Exception e) {
			return new FailedRetryable("ticketverify exception: " + e.getMessage(), traceId);
		} finally {
			System.out.println("[ticket_engine]:ended process... ");
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
		log.info("[ticketverify] parsed total={} {} merchant={}", value, currency, merchantName);

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

	private void debugDumpInput(String input, String traceId) {
		try {
			byte[] bytes = input.getBytes(StandardCharsets.UTF_8);

			// 1) écrit le fichier brut
			Path p = Paths.get("/tmp/ticketverify-" + traceId + ".txt");
			Files.write(p, bytes);

			// 2) hash pour comparer
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] h = md.digest(bytes);
			StringBuilder sb = new StringBuilder();
			for (byte b : h)
				sb.append(String.format("%02x", b));

			// 3) détecte caractères de contrôle suspects
			int ctrl = 0;
			for (int i = 0; i < input.length(); i++) {
				char c = input.charAt(i);
				if (c < 0x20 && c != '\n' && c != '\r' && c != '\t')
					ctrl++;
				if (Character.isSurrogate(c))
					ctrl++; // surrogates = suspect
				if (c == 0)
					ctrl++; // NUL
			}

			log.info("[ticketverify][dump] traceId={} bytes={} sha256={} suspiciousCount={} path={}",
					traceId, bytes.length, sb.toString(), ctrl, p);
		} catch (Exception e) {
			log.warn("[ticketverify][dump] failed {}", e.toString());
		}
	}
}
