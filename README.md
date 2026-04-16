# LUMEN

This repository has been trimmed to the modules needed for the upcoming multi-subject authentication and session-management work.

Active modules:
- `lumen-boot`
- `lumen-auth`
- `lumen-upms`
- `lumen-common`

Removed from the main build:
- `lumen-register`
- `lumen-gateway`
- `lumen-visual`

Runtime prerequisites:
- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 7.0+

Recommended entrypoint:
- `lumen-boot`
- `docker-compose.yml`
