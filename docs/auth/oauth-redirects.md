# OAuth redirect contract

This document defines the redirect contract for the mobile client and Studio.
The values are configuration inputs; this document does not modify Google
Cloud Console.

## Mobile client

The mobile flow uses Authorization Code + PKCE with a native custom scheme:

```text
Google authorization
  -> com.googleusercontent.apps.<client-prefix>:/oauthredirect
  -> POST /auth/google/mobile
  -> Fragments access/refresh token response
```

The exact value is supplied to the app through
`EXPO_PUBLIC_GOOGLE_MOBILE_IOS_REDIRECT_URI` and to the backend through
`GOOGLE_MOBILE_IOS_REDIRECT_URI`.

The backend rejects a redirect URI that does not exactly match its configured
value. The mobile client ID and redirect URI are public build configuration;
they are not secrets.

## Studio browser

Studio uses Authorization Code + PKCE in the browser:

```text
Studio
  -> GET /auth/google/studio/config
  -> Google authorization
  -> https://<studio-host>/auth/callback
  -> POST /auth/google/studio
  -> Fragments admin access/refresh token response
```

Recommended callbacks:

```text
https://studio-staging.anchor-event.fr/auth/callback
https://fragments.anchor-event.fr/auth/callback
```

Only the callback matching the deployed environment may be sent by that
environment. The backend compares the received value exactly with
`GOOGLE_STUDIO_REDIRECT_URI`.

The browser receives a client ID, never a Google client secret. PKCE verifier
and OAuth state are kept in the browser session for the login round-trip.

## Google Console checklist

Before enabling a Studio deployment:

1. register the exact HTTPS callback for staging;
2. register the exact HTTPS callback for production;
3. confirm that the selected Google OAuth client supports the browser redirect;
4. keep the mobile native redirect registered separately;
5. verify that no client secret is included in Vite, Expo, Docker, or GitHub
   public build variables.

The same Google project may be used for mobile and Studio. Reusing one client
ID is possible only if Google accepts both the native and browser redirect
types for that client. A separate public web client ID is the safer fallback;
it still does not require exposing a client secret when PKCE is used.

## CORS and cookie implications

The API CORS allowlist must contain only the browser origins that need access:

```text
https://studio-staging.anchor-event.fr
https://fragments.anchor-event.fr
```

The native mobile app does not need to be added as a browser CORS origin. The
Studio session uses bearer tokens and must not require wildcard origins.

## Change procedure

Domain, Caddy, backend, and Studio changes must be released in this order:

```text
DNS certificate/host readiness
-> Caddy route
-> backend allowed origin and OAuth redirect
-> Studio public build
-> Google Console callback verification
-> end-to-end login smoke test
```
