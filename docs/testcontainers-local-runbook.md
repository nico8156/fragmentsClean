# Testcontainers Local Runbook

## Stable Command

Use the repository runner for backend tests that require Testcontainers:

```bash
scripts/backend-testcontainers -q -Dtest=SqsIntegrationEventConsumerLocalStackTest test
```

Without arguments, the runner executes the full Maven test suite:

```bash
scripts/backend-testcontainers
```

## What The Runner Does

The runner:
- checks that Docker is reachable before Maven starts;
- resolves `DOCKER_HOST` from the active Docker context;
- sets `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock` for Unix socket Docker hosts;
- keeps Ryuk enabled so Testcontainers can clean up containers after the JVM exits.

This mirrors the Anchor local runner pattern and avoids one-off Docker environment tweaks per test class.

## Diagnostics

If Testcontainers fails before the test starts, verify Docker first:

```bash
docker context ls
docker info
docker run --rm busybox sh -c 'echo probe-ok'
```

For SQS adapter tests, LocalStack is started by Testcontainers. No AWS credentials are required for those tests.
