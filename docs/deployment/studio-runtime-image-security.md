# Studio staging — runtime image security correction

CHORE, 05/10/2026. Backend deployment run 37316478292 passed release
verification and built the ARM64 image, then failed the existing Trivy gate.
No image was pushed to ECR and the SSM deployment step was skipped.

The pinned runtime image contained libssl3t64, openssl and
openssl-provider-legacy 3.5.5-1ubuntu3.5. All three findings refer to the
same CVE-2026-84782, rather than three distinct CVEs. Ubuntu documents the
fix in 3.5.5-1ubuntu3.6:
https://ubuntu.com/security/CVE-2026-84782

Only the official eclipse-temurin:21-jre runtime index digest changes:
49e21e16e3c86eb7816a44a67549910ed090fbeb40c29c525d58bf5e02e91b0f
→ cff19e6215689161eb6162c11b86b0c60ddf802164f2eaf48d570f8fb79a36c5.
The ARM64 child manifest is
c357bafb2068784c48246c68acb9502f87b5a20e4399fdc484f51a4055a65039.
An ephemeral read-only, network-disabled ARM64 container confirms all three
OpenSSL packages at 3.5.5-1ubuntu3.7. No application, schema, contract,
Trivy exclusion or severity threshold changes.

The first local package-check command returned the correct AMD64 versions
but failed due to a shell format variable; the corrected ARM64 check above
is the successful evidence. The corrected base-image scan and release
workflow results must be recorded separately: package version checks alone
do not certify the complete application image.

Local verification complete: Trivy 0.70.0, remote ARM64 image scan,
HIGH/CRITICAL threshold, exit 0 with zero findings for the Ubuntu layer and
the included Go binary. ReleaseWorkflowGuardrailTest: two tests, zero failures
or errors. Logs: /private/tmp/studio-runtime-base-trivy.log and
/private/tmp/studio-runtime-guardrails.log. The deployment workflow must still
build and scan the complete application image before any ECR push or SSM step.
