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
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 로컬 개발 환경
- MySQL 8.0 설치 필요 (DB: `forgather_v2`, user: `root`, pw: `0000`)
- 또는 Docker: `docker run -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=0000 -e MYSQL_DATABASE=forgather_v2 mysql:8.0`

## 브랜치 전략
- `v2/develop`: 개발 브랜치 (PR 타겟)
- `v2/main`: 운영 브랜치
- 기능 브랜치: `v2/{type}/#{issue-number}-{description}`

## 핵심 규칙
- **Soft Delete**: 삭제 시 `SoftDeleteEntity` 상속, `delete()` 호출
- **DTO 변환**: 엔티티 직접 반환 금지, Response DTO로 변환 필수
- **읽기 전용**: 조회 메서드는 `@Transactional(readOnly=true)` 적용
- **Repository 패턴**: `findBy...()`는 Optional, `getBy...()`는 예외 던짐

## 주의사항
- **git-crypt 사용**: `application*.yml`은 git-crypt로 자동 암호화됨
- 새 민감 파일 추가 시 `.gitattributes`에 git-crypt 설정 필요
- S3 파일 삭제는 soft delete 우선
- 테스트에서 실제 외부 API 호출 금지 (Fake 구현 사용)

## 코딩 규칙
@../coderabbit_rules.md 참조

## 상세 문서
- @docs/architecture.md - 아키텍처 상세
- @docs/domains.md - 도메인 모델 설명
- @docs/testing.md - 테스트 전략
