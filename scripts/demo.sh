#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

BASE_URL="${BASE_URL:-http://localhost:8080}"
DEMO_USER="${DEMO_USER:-00000000-0000-0000-0000-000000000001}"

TICKET_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
COMMAND_ID="$(uuidgen | tr '[:upper:]' '[:lower:]')"
CLIENT_AT="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

echo "TicketId:  $TICKET_ID"
echo "CommandId: $COMMAND_ID"
echo "ClientAt:  $CLIENT_AT"
echo

# --- POST verify (async) ---
curl -s -i -X POST "$BASE_URL/api/tickets/verify" \
  -H "Content-Type: application/json" \
  -H "X-DEMO-USER: $DEMO_USER" \
  -d "{
    \"commandId\": \"$COMMAND_ID\",
    \"ticketId\": \"$TICKET_ID\",
    \"ocrText\": \"CARREFOUR CITY\\nTOTAL 12,34 EUR\\n\",
    \"imageRef\": null,
    \"clientAt\": \"$CLIENT_AT\"
  }"

echo
echo "Polling /api/tickets/$TICKET_ID/status ..."
echo

LAST=""
for i in {1..40}; do
  RESP="$(curl -s "$BASE_URL/api/tickets/$TICKET_ID/status" || true)"

  if ! echo "$RESP" | grep -q '"status"'; then
    echo "(not ready yet)"
    sleep 1
    continue
  fi

  # Print only when response changes (less noisy)
  if [[ "$RESP" != "$LAST" ]]; then
    echo "$RESP"
    LAST="$RESP"
  fi

  # Stop on final statuses (adapted to your read model)
  if echo "$RESP" | grep -Eq '"status"\s*:\s*"(CONFIRMED|REJECTED|FAILED)"'; then
    echo
    echo "✅ Demo completed."
    exit 0
  fi

  sleep 1
done

echo
echo "❌ Timeout waiting for ticket verification result."
exit 1
