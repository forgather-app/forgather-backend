# Skills 활용 · 작성 가이드

Skill은 하네스의 **Domain Layer**에 해당한다. **하나의 명확한 책임**을 가진 도메인 지식 캡슐.
상세 원리는 `.claude/guides/harness-guideline.md` §4 참조.

## Skills란?

Claude Code 기능을 확장하는 커스텀 명령어. `/skill-name` 형식으로 호출하거나, 관련 요청 시 Claude가 자동 활성화한다.

## 호출 방법

| 방법 | 예시 |
|-----|------|
| **명시적** | `/admin-ui list Product` |
| **자동** | "Product 어드민 페이지 만들어줘" → Claude가 판단하여 skill 활성화 |

**팁**: `/`를 입력하면 사용 가능한 skills 목록이 자동완성된다.

---

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

| 타입 | 대상 | 상속 클래스 |
|-----|------|------------|
| `unit` | 엔티티, 값 객체 | 없음 (순수 JUnit5) |
| `service` | Service 클래스 | `TestOnContainer` |
| `acceptance` | Controller (API) | `AcceptanceTest` |

---

## Skill 작성 규격

### 프런트매터

```yaml
---
name: skill-identifier
description: "[목적]. [활성화 트리거 조건]."
origin: custom
---
```

> 프로젝트에서 추가로 쓰는 `allowed-tools`, `user-invocable` 같은 필드는 실행 환경에서 필요하면 유지하되, 문서화 목적의 본문은 6섹션 구조를 따른다.

### 본문 6섹션 (필수)

| 섹션 | 역할 |
|---|---|
| `## When to Activate` | 트리거 조건 (파일 타입, 코드 패턴, 워크플로우 단계) |
| `## Core Concepts` | 핵심 도메인 지식 |
| `## Code Examples` | 언어·프레임워크별 구현 예시 |
| `## Best Practices` | 실천 가능한 체크리스트 |
| `## Checklist` | 생성·수정 후 검증 항목 |
| `## Red Flags` | skill이 잘못 사용되는 징후와 대응 |

### 크기 가이드라인

| 유형 | 줄 수 | 예시 |
|---|---|---|
| 단일 패턴 | 100~200 | 간단한 템플릿 skill |
| 표준 도메인 | 200~400 | tdd-workflow, admin-ui |
| 대형 도메인 | 400~800 | api-design |
| 800줄 초과 | 분할 필요 | sub-skill로 분리 |

### Progressive Disclosure

SKILL.md는 **진입점만** 담고, 상세는 하위 폴더로 분산한다:

```
my-skill/
  SKILL.md              ← 메인 진입점 (150~200줄)
  references/
    conventions.md      ← 세부 컨벤션
    troubleshooting.md  ← 트러블슈팅
  assets/               ← 코드 템플릿
  scripts/              ← 검증 스크립트
```

SKILL.md 본문에 "상세는 `references/...`" 링크만 남긴다.

---

## 안티패턴

| 패턴 | 증상 | 해결 |
|---|---|---|
| **God Skill** | 하나의 skill이 모든 기능 담당 | 도메인 단위로 분할 |
| **Feature Envy** | 다른 skill 데이터를 과도하게 참조 | 병합 또는 재구조화 |
| **Duplication** | 여러 skill에 동일 프롬프트 복붙 | 공통 references로 추출 |
| **Spaghetti SKILL.md** | 200줄+ 단일 파일에 모든 내용 | references/ 분리 |

---

## 자기 진단 체크리스트

새 skill 추가·수정 시:

- [ ] `/`로 트리거될 때 프로젝트 컨벤션을 따를 수 있는가?
- [ ] 6섹션(When to Activate / Core Concepts / Code Examples / Best Practices / Checklist / Red Flags) 모두 채워졌는가?
- [ ] SKILL.md 본문이 200줄 이내인가?
- [ ] 상세 내용이 `references/`, `assets/`, `scripts/`로 분리되어 있는가?
- [ ] Red Flags에 "이런 상황에선 다른 skill/agent 사용"이 명시되어 있는가?

---

## 참고

- [Skills 공식 문서](https://docs.anthropic.com/en/docs/claude-code/skills)
- 하네스 설계 원리: `.claude/guides/harness-guideline.md` §4
- Agents 가이드: `.claude/guides/agents-guide.md`
