# Mobile Architecture

The mobile app lives in `/Users/nicolasmaldiney/fragmentsCleanFront`.

## Flow

```text
Screen
-> ViewModel hook
-> Redux selector/listener/thunk
-> Gateway port
-> Secondary adapter
-> Backend
```

Screens render. They do not fetch.

View models derive UI state and expose callbacks. They do not instantiate concrete gateways.

Redux listener/use cases orchestrate reads and writes.

Gateway ports define external contracts.

Secondary adapters implement HTTP, WebSocket, SecureStore, MMKV, location, and native APIs.

## Mobile Write Flow

```text
UI intent
-> listener
-> validation
-> optimistic reducer
-> local outbox
-> HTTP command
-> awaiting ACK
-> socket ACK or command-status polling
-> reconcile or rollback
```

Rollback is reserved for explicit business rejection.

