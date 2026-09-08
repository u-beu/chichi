# chichi (Main Web Application) Guidelines

## Tech Stack
- Java 17, Spring Boot
- SSR: Thymeleaf
- Persistence: Spring Data JPA, Querydsl, Redis
- Build Tool: Gradle

## Common Commands
- Build & Run: `./gradlew bootRun`
- Test: `./gradlew test`
- Build JAR: `./gradlew bootJar`

## Development Rules
- **UI/UX:** Render views using Thymeleaf templates (YouTube Music-inspired design).
- **Data Operations:**
    - Check Database first before Redis operations for long-term data consistency (e.g., music likes).
    - Preserve chronological order from Redis IDs when retrieving song lists via Querydsl.
- **Pub/Sub:** Publish play request events to the `playback` channel on Redis Queue when a user clicks a song on the UI.
- **REST API:** Provide API endpoints to receive playback metadata sent directly from `chichi-bot`.

## Code Style & Architecture
- **Layer Architecture:** Keep business logic strictly within Service classes; Controllers should only handle requests and responses.
- **DTOs:** Perform Entity to DTO conversions in the Service layer. Do not expose JPA Entities directly to views or APIs.
- **Error Handling:** Use custom runtime exceptions and handle global exceptions centrally using `@RestControllerAdvice` or `@ControllerAdvice`.
- **Testing:** Prefer unit tests (`@ExtendWith(MockitoExtension.class)`) over slow integration tests (`@SpringBootTest`) whenever possible.

## Output Style
- Concise responses only.