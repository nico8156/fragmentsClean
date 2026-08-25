# Domain and OAuth matrix

This matrix separates observed values from the proposed shared-host target.
No DNS or OAuth provider configuration is changed by this document.

## Current / observed endpoints

| Project | Current endpoint or value | Role |
| --- | --- | --- |
| Anchor staging | `api-staging.anchor-event.fr` | Anchor public/API host used by the current Compose example |
| Fragments staging | `fragments-staging.anchor-event.fr` | Fragments API host used by mobile and Studio configuration |
| Mobile | `com.googleusercontent.apps...:/oauthredirect` | Native Google OAuth callback |
| Studio | Not deployed as a stable public host yet | Browser OAuth callback is not finalized |

The mobile redirect is a native custom-scheme value and must remain distinct
from browser redirects.

## Proposed shared-host staging names

The preferred model uses subdomains rather than path prefixes:

| Host | Consumer | Destination |
| --- | --- | --- |
| `anchor-staging.anchor-event.fr` | Anchor web/public pages | Anchor web container |
| `api-anchor-staging.anchor-event.fr` | Anchor clients and APIs | Anchor backend |
| `fragments-staging.anchor-event.fr` | Mobile API and compatibility endpoint | Fragments backend |
| `studio-staging.anchor-event.fr` | Studio browser | Studio frontend |

The exact names remain to be confirmed before Phase 1. Existing production or
staging URLs must not be repointed implicitly by a deployment workflow.

## OAuth redirects

### Mobile

```text
Google
  -> com.googleusercontent.apps...:/oauthredirect
  -> POST /auth/google/mobile
  -> Fragments JWT/session response
```

The backend compares the received redirect URI with
`GOOGLE_MOBILE_IOS_REDIRECT_URI` using an exact match.

### Studio

```text
Studio browser
  -> Google
  -> https://<confirmed-studio-host>/auth/callback
  -> POST /auth/google/studio
  -> Fragments admin JWT/session response
```

The backend currently exposes `/auth/google/studio/config` and requires a
configured `GOOGLE_STUDIO_REDIRECT_URI`. The callback host and path must be
registered exactly in Google Console before enabling the production Studio
build.

## Caddy rules for the target

- One Caddy instance owns ports 80 and 443.
- Routing should prefer hostnames over `/anchor` and `/fragments` path
  prefixes.
- Fragments SSE endpoints must keep streaming behavior and must not receive
  buffering or compression rules that break event delivery.
- API and frontend hosts need independent access logs and health checks.
- HTTP must redirect to HTTPS.
