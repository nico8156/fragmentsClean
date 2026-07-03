package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.projectionSync;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fragments.sync.sse")
public class ProjectionSyncProperties {
	private long timeoutMs = 300_000;
	private long heartbeatIntervalMs = 25_000;
	private long retryMs = 5_000;
	private long pollIntervalMs = 1_000;
	private int replayBatchSize = 100;

	public long getTimeoutMs() {
		return timeoutMs;
	}

	public void setTimeoutMs(long timeoutMs) {
		this.timeoutMs = timeoutMs;
	}

	public long getHeartbeatIntervalMs() {
		return heartbeatIntervalMs;
	}

	public void setHeartbeatIntervalMs(long heartbeatIntervalMs) {
		this.heartbeatIntervalMs = heartbeatIntervalMs;
	}

	public long getRetryMs() {
		return retryMs;
	}

	public void setRetryMs(long retryMs) {
		this.retryMs = retryMs;
	}

	public long getPollIntervalMs() {
		return pollIntervalMs;
	}

	public void setPollIntervalMs(long pollIntervalMs) {
		this.pollIntervalMs = pollIntervalMs;
	}

	public int getReplayBatchSize() {
		return replayBatchSize;
	}

	public void setReplayBatchSize(int replayBatchSize) {
		this.replayBatchSize = replayBatchSize;
	}
}
