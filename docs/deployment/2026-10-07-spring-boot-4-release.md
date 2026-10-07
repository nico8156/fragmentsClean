# Spring Boot 4 release qualification — 2026-10-07

Iteration: `CHORE`. Architecture route: technical/external adapters; bounded-context ownership, commands and integration event contracts stay unchanged.

## Trigger and decision

The article illustration staging release passed its 866 backend tests but the
mandatory Trivy gate rejected Spring MVC 6.2.19 for CVE-2026-47884. The operator
authorized extending the release to migrate the framework. No vulnerability
ignore or weakened security gate is introduced.

Spring Boot 4.0.8 manages Spring Framework 7.0.9 (the public fix) and Tomcat
11.0.24; the mandatory dependency scan requires Tomcat 11.0.26 and
Jackson 3.1.7 overrides to include their latest security patches. Java 21 is retained. Boot manages Netty again. Existing Jackson 2
contracts remain on the official `spring-boot-jackson2` compatibility module,
with Jackson 2.21.7 aligned through `jackson-2-bom.version` and explicit HTTP
mapper preference. Jackson 3 migration is deferred to a separate contract-aware
iteration. Testcontainers stays at 1.21.4 for existing container fixtures.

## Adapter changes

Boot's relocated persistence, health, JSON and test packages replace their old
imports. The security authorization manager implements Spring Security 7's
`authorize` method with the same policy. Google URI construction uses the
replacement `fromUriString` API. Tests use Framework Mockito bean overrides and
Boot's explicit test starters; existing assertions remain intact.

Boot 4 splits Jackson configuration prefixes: the established date formatting
setting moves from `spring.jackson` to `spring.jackson2`. The initial release
run detected the old key binding to a removed Jackson 3 enum before HTTP
contexts could start; that wiring error was corrected.

Mutation checkpoint: **NOT APPLICABLE**. This iteration changes dependency and
framework wiring only; existing business decisions are preserved and challenged
by the release suite, including HTTP, persistence, architecture and real
PostgreSQL migration tests.

## References

- https://spring.io/security/cve-2026-47884/
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- https://tomcat.apache.org/security-11.html
- https://github.com/FasterXML/jackson/wiki/Jackson-Release-3.1.7
- https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.8/

## Qualification

Final validation and staging evidence will be recorded after completion.
