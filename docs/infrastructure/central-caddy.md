# Central Caddy

The platform Compose file starts exactly one Caddy instance for the shared
staging host. It is the only service allowed to bind host ports 80 and 443.

## Routing

```text
ANCHOR_WEB_DOMAIN      -> anchor-web:3000
ANCHOR_API_DOMAIN      -> anchor-backend:8080
FRAGMENTS_API_DOMAIN   -> fragments-backend:8080
STUDIO_DOMAIN          -> fragments-studio:4173
```

The upstream names are Docker DNS names on the corresponding external edge
networks. Anchor and Fragments Compose projects must create and join their own
edge network; Caddy must never join a private database network.

## SSE behavior

Fragments projection sync endpoints are handled separately with
`flush_interval -1`. This prevents proxy buffering from delaying projection
notifications. SSE is still only a freshness signal; it is not a command ACK or
business source of truth.

## TLS and domains

Domains are supplied through the platform `.env` file and are not hardcoded as
AWS resources. Caddy manages certificates through its `/data` volume. DNS must
point to the platform Elastic IP before the service is started publicly.

The first activation must verify all four hosts independently. A failed Caddy
configuration must not be used as a reason to change DNS blindly.

## Safety rules

- Never start Anchor or Fragments Caddy alongside this service on the shared
  host.
- Do not publish backend ports 8080 directly.
- Do not put AWS, OAuth, or application secrets in the Caddyfile.
- Keep `/data` and `/config` persistent and backed up according to the platform
  operations policy.
- Validate the Caddyfile before reload.

## Local validation

With Docker available and the two external edge networks created:

```bash
docker compose --env-file .env -f docker-compose.yml config
docker run --rm \
  -v "$PWD/Caddyfile:/etc/caddy/Caddyfile:ro" \
  caddy:2-alpine caddy validate --config /etc/caddy/Caddyfile
```

The second command requires the domain variables to be available to the
container. It validates configuration only; it does not obtain certificates or
change DNS.
