import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve('infra/aws/compose/platform/staging/fragments/public-site');
const pages = ['index.html', 'tech.html', 'confidentialite.html', 'conditions.html', 'mentions-legales.html'];

test('pages are French, responsive, accessible static documents with local assets', () => {
  for (const page of pages) {
    const html = readFileSync(resolve(root, page), 'utf8');
    assert.match(html, /<html lang="fr">/);
    assert.match(html, /<meta name="viewport"/);
    assert.match(html, /<title>[^<]+<\/title>/);
    assert.equal((html.match(/<h1[ >]/g) ?? []).length, 1);
    assert.match(html, /<main id="contenu"/);
    assert.match(html, /href="#contenu"/);
    assert.doesNotMatch(html, /<script\b|<iframe\b|<form\b|\son\w+\s*=/i);
    assert.doesNotMatch(html, /(?:src|srcset)="https?:/i);
    for (const [, href] of html.matchAll(/href="([^"]+)"/g)) {
      if (href.startsWith('#') || href.startsWith('https://')) continue;
      if (href.startsWith('mailto:')) {
        assert.equal(href, 'mailto:studio@anchor-event.fr');
        continue;
      }
      const file = href === '/' || href === '/legal/' ? 'index.html' : href.replace(/^\/legal\//, '');
      assert.ok(existsSync(resolve(root, file)), `${page}: broken local link ${href}`);
    }
  }
  const css = readFileSync(resolve(root, 'site.css'), 'utf8');
  assert.doesNotMatch(css, /@import|url\(\s*['"]?https?:/i);
  assert.match(css, /:focus-visible/);
});

test('presentation follows the agreed product rules without inventing availability', () => {
  const html = readFileSync(resolve(root, 'index.html'), 'utf8');
  assert.match(html, /avec ou sans ticket/);
  assert.match(html, /certains niveaux/i);
  assert.match(html, /phase de test/i);
  assert.doesNotMatch(html, /apps\.apple\.com|testflight\.apple\.com|témoignage/i);
});

test('beta pages are publishable information without hiding retention limitations', () => {
  for (const page of pages) {
    const html = readFileSync(resolve(root, page), 'utf8');
    assert.match(html, /name="fragments-publication-status" content="beta"/);
    assert.doesNotMatch(html, /Brouillon|À compléter|À définir avant publication|À valider avant publication/);
  }
  const privacy = readFileSync(resolve(root, 'confidentialite.html'), 'utf8');
  for (const section of ['responsable', 'finalites', 'destinataires', 'conservation', 'droits', 'suppression']) {
    assert.ok(privacy.includes(`id="${section}"`));
  }
  assert.match(privacy, /sauvegardes/i);
  assert.match(privacy, /CNIL/);
  assert.match(privacy, /hors de l’Union européenne/);
  assert.match(privacy, /expiration à 30 jours/);
  assert.match(privacy, /copies techniques/);
  assert.match(privacy, /redirigés vers la boîte Gmail de l’éditeur/);
  assert.match(privacy, /Sentry/);
  assert.match(privacy, /infrastructure européenne avec ingestion en Allemagne/);
  assert.match(privacy, /diagnostics Sentry du plan Developer sont consultables pendant 30 jours/);
  assert.match(privacy, /suivi de performance et l’enregistrement de session/);
  assert.doesNotMatch(privacy, /Aucun délai automatique de purge des sauvegardes n’a été prouvé/);
});

test('pages identify the confirmed individual editor and public contact', () => {
  for (const page of pages) {
    const html = readFileSync(resolve(root, page), 'utf8');
    assert.match(html, /Nicolas Maldiney/);
    assert.match(html, /à titre personnel/);
    assert.match(html, /href="mailto:studio@anchor-event\.fr"/);
    assert.doesNotMatch(html, /identité de l’éditeur.*(?:confirmer|valider)|contact.*(?:ajouté après confirmation|doit être complété)/i);
  }
});

test('Caddy routes public files only from the dedicated root and preserves API routing', () => {
  const config = readFileSync('infra/aws/compose/platform/staging/Caddyfile', 'utf8');
  assert.match(config, /@fragmentsHome path \/\n/);
  assert.match(config, /handle_path \/legal\/\*/);
  assert.match(config, /root \* \/data\/fragments-public/);
  assert.doesNotMatch(config, /root \* \/data\s*\n/);
  assert.match(config, /@sse path \/api\/sync\/events \/api\/admin\/sync\/events/);
  assert.match(config, /flush_interval -1/);
  assert.equal((config.match(/reverse_proxy fragments-backend:8080/g) ?? []).length, 2);
  assert.match(config, /Content-Security-Policy/);
});

test('presentation is concise, with implementation details on a separate page', () => {
  const home = readFileSync(resolve(root, 'index.html'), 'utf8');
  const tech = readFileSync(resolve(root, 'tech.html'), 'utf8');
  assert.match(home, /href="\/legal\/tech\.html"/);
  assert.doesNotMatch(home, /Spring Boot|PostgreSQL|SQS|CQRS|outbox/i);
  for (const term of [/Spring Boot/, /React Native/, /PostgreSQL/, /SQS/]) {
    assert.match(tech, term);
  }
});

test('all pages expose the legal notice and tech navigation', () => {
  for (const page of pages) {
    const html = readFileSync(resolve(root, page), 'utf8');
    assert.match(html, /href="\/legal\/mentions-legales\.html"/);
    assert.match(html, /href="\/legal\/tech\.html"/);
  }
  const notice = readFileSync(resolve(root, 'mentions-legales.html'), 'utf8');
  assert.match(notice, /Amazon Web Services EMEA SARL/);
});
