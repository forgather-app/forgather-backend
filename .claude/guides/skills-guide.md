# Skills 활용 가이드

## Skills란?

Claude Code의 기능을 확장하는 커스텀 명령어입니다. `/skill-name` 형식으로 호출하거나, 관련 요청 시 Claude가 자동으로 활성화합니다.

## 호출 방법

| 방법 | 예시 |
|-----|------|
| **명시적** | `/admin-ui list Product` |
| **자동** | "Product 어드민 페이지 만들어줘" → Claude가 판단하여 skill 활성화 |

**팁**: `/`를 입력하면 사용 가능한 skills 목록이 자동완성됩니다.

## 프로젝트 Skills

### `/admin-ui`
어드민 UI 코드 생성 (Thymeleaf 기반)

```
/admin-ui list Product       # 목록 페이지
/admin-ui detail Product     # 상세 모달
/admin-ui form Product       # 생성/수정 폼
/admin-ui crud Product       # 위 세 가지 모두
```

### `/test-writer`
테스트 코드 생성 (JUnit5 + Spring Boot)

```
/test-writer unit Space           # 단위 테스트 (엔티티)
/test-writer service SpaceService # 서비스 통합 테스트
/test-writer acceptance Space     # 인수 테스트 (API)
```

| 타입 | 대상 | 상속 클래스 |
|-----|------|------------|
| `unit` | 엔티티, 값 객체 | 없음 (순수 JUnit5) |
| `service` | Service 클래스 | `TestOnContainer` |
| `acceptance` | Controller (API) | `AcceptanceTest` |

## 참고

- [Skills 공식 문서](https://docs.anthropic.com/en/docs/claude-code/skills)
- Agents 활용법: `.claude/guides/agents-guide.md`
