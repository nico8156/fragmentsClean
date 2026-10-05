# Studio exclusive owner access — milestone 5c

Google OAuth remains the identity provider. The existing admin security chain
owns authorization for all `/api/admin/**`, including Projection Sync SSE.
`ADMIN_SECURITY_EXCLUSIVE_OWNER_ID` selects a single Authentication user UUID.
While configured, bootstrap emails/other IDs and persisted admin grants do not
allow access. Mutations of `/api/admin/access/users` are forbidden, including
for the owner. Existing grants are retained rather than deleted.

Blank configuration retains historical allowlist behavior for compatibility;
it is not an exclusive mode. Invalid UUID configuration fails binding/startup.
The staging bootstrap sets the owner from the existing bootstrap UUID parameter
and rejects a missing/invalid/multiple value before preparing its env file.
No SSM change or deployment was executed during this task.

GET `/api/admin/access/me` is an authorization probe through the same security
chain. Its response contains only `userId` and `exclusiveOwner`; anonymous
requests receive 401 and unauthorized authenticated requests receive 403.
The compatible backend must deploy before the Studio bundle using this probe.
Before activation, associate the configured Authentication UUID with Nicolas's
Google login, then verify owner and other account on the deployed environment.
Rollback to blank owner configuration re-enables historical grants.

BEHAVIOUR / security adapter iteration: RED exposed a persisted admin returning
200 instead of 403, plus the missing probe returning 404. Minimum green reuses
existing policy and authorization manager. A manual policy mutation authorizing
all identities in exclusive mode was killed by AdminAccessPolicyTest; exact
source restoration and final green verified.

Final verification: 28 tests across StudioExclusiveOwnerIT (3),
AdminAccessPolicyTest (4), AdminAccessSecurityIT (2), DeploymentSafetyIT (8),
BoundedContextArchitectureTest (11). Deployment tests use fake AWS calls and
local disposable containers, preserving live env on invalid owner settings.
OpenAPI updated. No business tables, domain events or mobile auth behavior
changed. Audit-search work is separate and not part of this focused commit.
