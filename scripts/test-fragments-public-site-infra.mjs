import { test } from 'node:test';
import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { resolve } from 'node:path';
import { setTimeout as delay } from 'node:timers/promises';

const docker = (...args) => execFileSync('docker', args, { encoding: 'utf8', timeout: 30000 }).trim();

test('real Caddy serves the pages, protects its root and preserves both API routes', { timeout: 90000 }, async (t) => {
  const suffix = randomUUID().slice(0, 12);
  const network = `fragments-public-test-${suffix}`;
  const containers = [];
  docker('info', '--format', '{{.ServerVersion}}');
  const networkId = docker('network', 'create', network);
  t.after(() => {
    for (const id of containers.reverse()) docker('rm', '--force', '--volumes', id);
    docker('network', 'rm', networkId);
  });
  containers.push(docker('run', '--detach', '--network', network,
    '--network-alias', 'fragments-backend', 'caddy:2-alpine',
    'caddy', 'respond', '--listen', ':8080', '--body', 'fragments-test-upstream'));
  const proxy = docker('run', '--detach', '--network', network,
    '--publish', '127.0.0.1::8080', '--env', 'FRAGMENTS_API_DOMAIN=http://:8080',
    '--mount', `type=bind,source=${resolve('infra/aws/compose/platform/staging/Caddyfile')},target=/etc/caddy/Caddyfile,readonly`,
    '--mount', `type=bind,source=${resolve('infra/aws/compose/platform/staging/fragments/public-site')},target=/data/fragments-public,readonly`,
    'caddy:2-alpine');
  containers.push(proxy);
  const address = docker('port', proxy, '8080/tcp');
  assert.match(address, /^127\.0\.0\.1:\d+$/);
  const base = `http://${address}`;
  let ready = false;
  for (let attempt = 0; attempt < 30; attempt++) {
    try { ready = (await fetch(base, { signal: AbortSignal.timeout(1000) })).ok; } catch {}
    if (ready) break;
    await delay(300);
  }
  assert.ok(ready, 'Caddy starts with the real candidate configuration');
  for (const path of ['/', '/legal/', '/legal/confidentialite.html', '/legal/conditions.html']) {
    const response = await fetch(base + path);
    assert.equal(response.status, 200, path);
    assert.match(response.headers.get('content-type'), /text\/html/);
    assert.match(response.headers.get('content-security-policy'), /default-src 'none'/);
    assert.equal(response.headers.get('set-cookie'), null);
    assert.match(await response.text(), /Fragments/);
  }
  const css = await fetch(base + '/legal/site.css');
  assert.equal(css.status, 200);
  assert.match(css.headers.get('content-type'), /text\/css/);
  assert.equal((await fetch(base + '/legal/absent.html')).status, 404);
  assert.equal((await fetch(base + '/legal/caddy/certificates')).status, 404);
  for (const path of ['/api/coffees', '/api/sync/events', '/api/admin/sync/events']) {
    const response = await fetch(base + path);
    assert.equal(response.status, 200, path);
    assert.equal(await response.text(), 'fragments-test-upstream');
  }
});
