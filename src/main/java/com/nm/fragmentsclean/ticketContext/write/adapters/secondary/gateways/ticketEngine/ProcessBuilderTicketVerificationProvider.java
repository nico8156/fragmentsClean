package com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.ticketEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
	private final int maxInputBytes;
	private final int maxOutputBytes;

	public ProcessBuilderTicketVerificationProvider(ObjectMapper objectMapper,
			List<String> command,
			Duration timeout) {
		this(objectMapper, command, timeout, 1_048_576, 262_144);
	}

	public ProcessBuilderTicketVerificationProvider(ObjectMapper objectMapper,
			List<String> command,
			Duration timeout,
			int maxInputBytes,
			int maxOutputBytes) {
		this.objectMapper = objectMapper;
		this.command = List.copyOf(command);
		if (this.command.isEmpty())
			throw new IllegalArgumentException("command must not be empty");
		if (timeout == null || timeout.isZero() || timeout.isNegative())
			throw new IllegalArgumentException("timeout must be positive");
		if (maxInputBytes < 1 || maxOutputBytes < 1)
			throw new IllegalArgumentException("I/O limits must be positive");
		this.timeout = timeout;
		this.maxInputBytes = maxInputBytes;
		this.maxOutputBytes = maxOutputBytes;
	}

	@Override
	public Result verify(String ocrText, String imageRef) {
		// ticketverify is a text-only engine. imageRef belongs to the capture/OCR flow.
		String traceId = "tv:" + UUID.randomUUID();
		if (ocrText == null || ocrText.isBlank()) {
			return new Rejected("OCR_TEXT_MISSING", "ocrText is required for now", traceId);
		}
		String input = ocrText.endsWith("\n") ? ocrText : ocrText + "\n";
		byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
		if (inputBytes.length > maxInputBytes) {
			return new Rejected("OCR_TEXT_TOO_LARGE", "ocrText exceeds the configured byte limit", traceId);
		}

		Process process = null;
		ExecutorService ioExecutor = null;
		List<CompletableFuture<?>> ioTasks = new ArrayList<>();
		List<String> cmd = new ArrayList<>(command);
		log.debug("[ticketverify] binary={}", cmd.get(0));

		cmd.addAll(List.of("--schema", "v1", "--format", "json"));
		long deadline = System.nanoTime() + timeout.toNanos();

		try {
			ProcessBuilder pb = new ProcessBuilder(
					cmd);
			pb.redirectErrorStream(false);

			process = pb.start();
			Process runningProcess = process;
			ioExecutor = Executors.newThreadPerTaskExecutor(
					Thread.ofVirtual().name("ticketverify-io-", 0).factory());
			CompletableFuture<Void> inputTask = CompletableFuture.runAsync(
					() -> writeInput(runningProcess, inputBytes), ioExecutor);
			CompletableFuture<String> stdoutTask = CompletableFuture.supplyAsync(
					() -> readBounded(runningProcess.getInputStream(), maxOutputBytes), ioExecutor);
			CompletableFuture<String> stderrTask = CompletableFuture.supplyAsync(
					() -> readBounded(runningProcess.getErrorStream(), maxOutputBytes), ioExecutor);
			ioTasks.add(inputTask);
			ioTasks.add(stdoutTask);
			ioTasks.add(stderrTask);
			stdoutTask.whenComplete((ignored, failure) -> destroyOnIoFailure(runningProcess, failure));
			stderrTask.whenComplete((ignored, failure) -> destroyOnIoFailure(runningProcess, failure));

			boolean finished = process.waitFor(remainingNanos(deadline), TimeUnit.NANOSECONDS);
			if (!finished) {
				return new FailedRetryable("ticketverify timeout after " + timeout.toMillis() + "ms",
						traceId);
			}

			int exit = process.exitValue();
			await(inputTask, deadline);
			String stdout = await(stdoutTask, deadline);
			String stderr = await(stderrTask, deadline);
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
		} catch (TimeoutException e) {
			return new FailedRetryable("ticketverify timeout after " + timeout.toMillis() + "ms", traceId);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return new FailedRetryable("ticketverify interrupted", traceId);
		} catch (ExecutionException e) {
			return new FailedRetryable("ticketverify I/O failure: " + rootMessage(e), traceId);
		} catch (Exception e) {
			return new FailedRetryable("ticketverify exception: " + e.getMessage(), traceId);
		} finally {
			ioTasks.forEach(task -> task.cancel(true));
			terminate(process);
			if (ioExecutor != null)
				ioExecutor.shutdownNow();
			log.debug("[ticketverify] process ended traceId={}", traceId);
		}
	}

	private static void writeInput(Process process, byte[] input) {
		try (var output = process.getOutputStream()) {
			output.write(input);
			output.flush();
		} catch (IOException failure) {
			throw new CompletionException(failure);
		}
	}

	private static String readBounded(InputStream input, int maxBytes) {
		try (input; var buffer = new ByteArrayOutputStream()) {
			byte[] chunk = new byte[4096];
			int read;
			while ((read = input.read(chunk)) >= 0) {
				if (read > maxBytes - buffer.size())
					throw new IOException("ticketverify output exceeds byte limit");
				buffer.write(chunk, 0, read);
			}
			return buffer.toString(StandardCharsets.UTF_8);
		} catch (IOException failure) {
			throw new CompletionException(failure);
		}
	}

	private static void destroyOnIoFailure(Process process, Throwable failure) {
		if (failure != null && process.isAlive())
			process.destroyForcibly();
	}

	private static long remainingNanos(long deadline) throws TimeoutException {
		long remaining = deadline - System.nanoTime();
		if (remaining <= 0)
			throw new TimeoutException("ticketverify deadline elapsed");
		return remaining;
	}

	private static <T> T await(CompletableFuture<T> task, long deadline)
			throws InterruptedException, ExecutionException, TimeoutException {
		return task.get(remainingNanos(deadline), TimeUnit.NANOSECONDS);
	}

	private static String rootMessage(Throwable failure) {
		Throwable current = failure;
		while (current.getCause() != null)
			current = current.getCause();
		return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
	}

	private static void terminate(Process process) {
		if (process == null)
			return;
		List<ProcessHandle> descendants = descendantsOf(process);
		descendants.forEach(ProcessHandle::destroy);
		if (process.isAlive())
			process.destroy();
		try {
			if (process.isAlive() && !process.waitFor(100, TimeUnit.MILLISECONDS)) {
				descendants.forEach(ProcessHandle::destroyForcibly);
				process.destroyForcibly();
			}
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			descendants.forEach(ProcessHandle::destroyForcibly);
			process.destroyForcibly();
		} finally {
			closeQuietly(process.getOutputStream());
			closeQuietly(process.getInputStream());
			closeQuietly(process.getErrorStream());
		}
	}

	private static List<ProcessHandle> descendantsOf(Process process) {
		try {
			return process.descendants().toList();
		} catch (RuntimeException unavailable) {
			return List.of();
		}
	}

	private static void closeQuietly(AutoCloseable stream) {
		try {
			stream.close();
		} catch (Exception ignored) {
			// Process termination is already best-effort at this point.
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

}
