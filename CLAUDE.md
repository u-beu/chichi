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
- **UI/UX:**
  - Thymeleaf 템플릿 기반 뷰 랜더링 (YouTube Music 스타일 디자인 반영)
  - SSE(Server-Sent Events)를 통한 웹 UI 실시간 재생 상태 업데이트
- **Data Operations:**
  - **데이터 정합성:** 장기 데이터(예: 노래 좋아요) 관리 시 Redis 작업 전 Database 우선 조회
  - **정렬 유지:** Querydsl 노래 목록 조회 시 Redis ID의 chronological order 유지
- **Messaging & Event Flow (Redis Queue):**
  - **Playback Request:** 웹 UI 노래 클릭 시 Redis Queue로 재생 요청 이벤트 Publish
  - **Playback Metadata Sync & Analysis Trigger:**
    - `chichi-bot` 발행 재생 음원 데이터(제목, 가수, URL, 썸네일) 메시지 수신
    - DB 저장 및 SSE 기반 UI 업데이트
    - DB 내 음원 분석 데이터 존재 여부 확인 후, 미존재 시 `chichi-bot` Worker로 분석 요청 메시지 Publish
  - **Audio Analysis Sync:** `chichi-bot` Worker의 PyTorch 분석 완료 메시지 수신 -> DB 저장
  - **Queue Schema (Redis List, `chichi-bot` 문서 기준):**
    - 발행(`LPUSH`) 재생 요청: `bot:commands` -> `{"action": "playback", "user_id", "query"}` (`guild_id` 없음, 봇이 `user_id`로 음성채널 접속 길드를 직접 탐색)
    - 발행(`LPUSH`) 분석 요청: `worker:analysis-requests` -> `{"videoId"}`
    - 소비 재생 기록: `spring:play-history` <- `{"title", "uploader", "image", "videoId", "discordId"}`
    - 소비 재생 실패 알림: `spring:playback-result` <- `{"discordId", "status": "not_in_voice"}`
    - 소비 분석 결과: `spring:analysis-results` <- 성공 `{"videoId", "genre", "mood"}` / 실패 `{"videoId", "error"}`
  - **통신 원칙:** `chichi-bot`과 REST 직접 통신 금지, Redis List 기반 메시지 버스만 사용

## Code Style & Architecture
- **Layer Architecture:** 비즈니스 로직은 엄격히 Service 클래스에 작성 (Controller는 요청/응답 처리만 담당)
- **DTOs:** Entity -> DTO 변환은 Service 계층에서 수행 (JPA Entity의 뷰/API 직접 노출 금지)
- **Error Handling:** 커스텀 Runtime Exception 사용 및 `@RestControllerAdvice` / `@ControllerAdvice` 기반 중앙 예외 처리
- **Testing:** 느린 Integration Test (`@SpringBootTest`) 대신 Unit Test (`@ExtendWith(MockitoExtension.class)`) 지향

## Output Style
- 서론/군더더기 없이 핵심 위주로 직관적이고 간결하게 답변
- 한국어로 항상 답변