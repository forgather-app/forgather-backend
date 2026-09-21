# Forgather Backend

## 개요
작가와 방문객이 연결되는 온라인 전시 공간 서비스의 백엔드 API

## 기술 스택
- Java 21, Spring Boot 3.4.7, Spring Data JPA
- MySQL 8.0, Flyway (마이그레이션)
- AWS S3 (파일 저장)
- JWT + Kakao OAuth (인증)
- Thymeleaf (관리자 페이지)

## 프로젝트 구조
```
src/main/java/com/forgather/
├── global/        # 전역 설정, 인증, 예외 처리
├── domain/        # 비즈니스 도메인
│   ├── space/     # 전시 공간
│   ├── product/   # 작품
│   ├── guestbook/ # 방명록
│   ├── upload/    # 파일 업로드
│   └── stats/     # 통계
└── back_office/   # 관리자 기능
```

## 환경 프로파일
| 프로파일 | 용도 | DB |
|---------|------|-----|
| local | 로컬 개발 | localhost MySQL |
| dev | 개발 서버 | RDS (dev) |
| prod | 운영 서버 | RDS (prod) |
| test | 테스트 | TestContainers |

## 명령어
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 단일 테스트: `./gradlew test --tests "TestClassName"`
- 로컬 실행: `./gradlew bootRun`
- 코드 포맷팅: `./gradlew spotlessApply`
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 로컬 개발 환경
- MySQL 8.0 설치 필요 (DB: `forgather_v2`, user: `root`, pw: `0000`)
- 또는 Docker: `docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=0000 -e MYSQL_DATABASE=forgather_v2 mysql:8.0`

## 브랜치 전략
- `v2/develop`: 개발 브랜치 (PR 타겟)
- `v2/main`: 운영 브랜치
- 기능 브랜치: `v2/{type}/#{issue-number}-{description}`

## 핵심 규칙
- **ALWAYS** extend `SoftDeleteEntity` for domain entities; call `entity.delete()` instead of `repository.delete()`
- **NEVER** return JPA entities from controllers; convert to `*Response` DTO
- **ALWAYS** annotate read-only service methods with `@Transactional(readOnly = true)`
- **ALWAYS** make `findBy...()` return `Optional<T>`; **ALWAYS** make `getBy...()` throw on missing

## 주의사항
- **ALWAYS** 작업 명령/컨텍스트에 작업 내용을 커밋하라는 요구사항이 없으면 임의로 커밋하지 않는다.
- **ALWAYS** register new/moved sensitive files in `.gitattributes` with `filter=git-crypt diff=git-crypt` **before** the first commit
- **ALWAYS** run `git-crypt status` on new/moved sensitive files before push; un-encrypted entries must be zero
- **NEVER** let CI/CD pipelines (buildspec.yml 등) commit back generated `application*.yml` without a prior git-crypt check
- **ALWAYS** prefer soft delete over physical deletion, including S3 objects
- **NEVER** call real external APIs from tests; use Fake implementations

## 코딩 규칙
coderabbit_rules.md 참조 (프로젝트 루트)

## 상세 문서
- `.claude/guides/harness-guideline.md` — Claude Code 하네스 설계 기준 (SSOT)
- `.claude/docs/architecture.md` — 아키텍처 상세
- `.claude/docs/domains.md` — 도메인 모델 설명
- `.claude/docs/testing.md` — 테스트 전략
- `.claude/guides/admin-ui-guide.md` — 어드민 UI 컨벤션
- `.claude/guides/agents-guide.md` · `skills-guide.md` · `hooks-guide.md` — 하네스 작성 가이드
