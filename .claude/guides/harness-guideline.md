---
title: Harness 엔지니어링 실전 가이드
aliases:
  - 하네스 가이드라인
  - Harness Guideline
  - Harness 실전 가이드
tags:
  - software-3-0
  - llm
  - agent
  - harness
  - context-engineering
  - guideline
created: 2026-04-16
sources:
  - https://github.com/affaan-m/everything-claude-code
  - https://www.anthropic.com/engineering/harness-design-long-running-apps
  - https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents
  - 하네스엔지니어링 (Team Attention 이호연, 2026.04.07)
---

> [!abstract] 이 문서의 위치
> [[What is Harness?]]가 "Harness란 무엇이고, 왜 필요한가"를 다룬다면, 이 문서는 **"각 계층을 어떻게 만드는가"**를 다룬다. 이론의 실전 동반자.

---

## 문서 구조와 Harness 계층 매핑

| 섹션 | Harness 계층 | ECC 참조 디렉토리 | 산출물 |
|---|---|---|---|
| 1 | Config | `examples/`, `contexts/` | `CLAUDE.md` |
| 2 | Controller (Feedforward) | `rules/` | Rule 파일 |
| 3 | Service | `agents/` | Agent 정의 |
| 4 | Domain | `skills/` | Skill 정의 |
| 5 | Infrastructure | `mcp-configs/` | MCP 설정 |
| 6 | Feedback | `hooks/` | Hook 정의 |
| 7 | 계획 (Planning) | — | Plan 파일, 스펙 |
| 8 | 실행 (Orchestration) | — | 오케스트레이션 패턴 |
| 9 | 검증 (Verification) | — | 검증 기준, 안전장치 |
| 10 | 횡단 관심사 | 여러 디렉토리 | 토큰/GC/보안/개선 |

> [!note] 분석 기반
> [everything-claude-code](https://github.com/affaan-m/everything-claude-code)(ECC)의 47개 agent, 181개 skill, hooks, rules, MCP 설정, CLAUDE.md 템플릿을 분석하여 **공통 패턴**을 추출했다.

### Harness 6축 순환 구조 — 운영 관점의 보완 프레임워크

위 7-layer 아키텍처가 **"무엇을 만드는가"**를 다룬다면, 6축 순환 구조는 **"어떻게 운영하는가"**를 다룬다. 두 관점은 상호 보완적이다.

```
구조(Scaffolding) → 맥락(Context) → 계획(Planning)
       ↑                                    ↓
개선(Compounding) ← 검증(Verification) ← 실행(Execution)
```

| 축 | 핵심 질문 | 관련 계층 |
|---|---|---|
| **구조** | 뭘 깔아두는가 | Config, Infrastructure |
| **맥락** | AI가 뭘 아는가 | Config, Controller |
| **계획** | 뭘 할지 정하는가 | Service |
| **실행** | 어떻게 시키는가 | Service, Domain |
| **검증** | 잘 했는지 확인 | Feedback |
| **개선** | 어떻게 나아지는가 | 횡단 관심사 |

> 모델 교체로 5% 개선하는 것보다, **하네스 설계로 16% 개선하는 것이 현실적**이다.

---

## 1. CLAUDE.md 작성 가이드 (Config Layer)

> 참조: [[What is Harness?#(1) 컨텍스트 파일 — `CLAUDE.md`]]

### 역할

에이전트가 **매 세션 가장 먼저 읽는** 문서. Software 1.0의 `package.json`이자 `application.yml`에 해당한다. 세션 경계를 넘어 살아남는 유일한 맥락.

### 공통 패턴 (ECC 5개 템플릿 분석)

1. **60줄 이하** — 본문이 아니라 인덱스. 길어지면 모델이 핵심을 놓친다.
2. **명령형 어투** — "ALWAYS create new objects", "NEVER mutate state". 설명이 아니라 지시.
3. **구체적 임계값** — "함수 50줄 이하", "파일 800줄 이하". 모호한 "짧게 작성하라"는 무의미.
4. **빌드 명령은 복사 가능하게** — 모델이 바로 실행할 수 있는 형태.
5. **상세 내용은 링크로** — CLAUDE.md는 카탈로그, 본문은 별도 문서.

### 작성 템플릿

````markdown
# Project Name

## Overview
[프로젝트 목적 1-2문장]

## Tech Stack
- [언어/프레임워크/DB 나열]

## Directory Structure
```
src/
  domain/     - 비즈니스 로직
  api/        - 라우터/컨트롤러
  infra/      - DB, 외부 서비스 연동
tests/        - 테스트
```

## Build & Run
```bash
npm install          # 의존성 설치
npm run dev          # 개발 서버
npm run test         # 테스트 실행
npm run build        # 프로덕션 빌드
```

## Coding Conventions
- ALWAYS use immutable patterns
- NEVER mutate function arguments
- Functions under 50 lines
- Files under 800 lines
- Early return over deep nesting

## Testing
- TDD: RED → GREEN → REFACTOR
- Minimum 80% coverage
- Unit tests for utilities, integration tests for APIs

## Domain Knowledge
[이 프로젝트만의 특수한 비즈니스 규칙, 용어, 제약사항]

## Current Phase
[지금 하고 있는 작업과 하면 안 되는 것]
````

### Best Practice 예시: SaaS Next.js 프로젝트

```markdown
# SaaS Dashboard

## Overview
Next.js 15 + Supabase + Stripe 기반 SaaS 대시보드.

## Coding Conventions
- ALWAYS enforce Row-Level Security on all Supabase queries
- NEVER use SELECT * — explicit column selection only
- ALWAYS validate server actions with Zod schemas
- NEVER expose Stripe secret key to client components

## Domain Knowledge
- Free tier: 100 API calls/day, 1GB storage
- Webhook은 반드시 idempotent 처리
- 구독 상태 변경은 Stripe webhook이 SSOT
```

### 설정 파일 상속 구조 — 하위가 상위를 덮어쓴다

CLAUDE.md는 단일 파일이 아니라 **계층적 상속 구조**를 갖는다. 아래로 갈수록 범위가 좁고, 우선순위가 높다.

```
~/.claude/CLAUDE.md          ← USER: 나만 적용 · 모든 프로젝트 (내 습관, 스타일)
  └─ my-app/CLAUDE.md        ← PROJECT: 팀 공유 · Git 커밋 (스택, 컨벤션)
      └─ src/auth/CLAUDE.md  ← FOLDER: 이 폴더 작업 시에만 자동 로드 (특수 규칙)
```

| 레벨 | 담는 내용 | 예시 |
|---|---|---|
| **User** | 내 작업 습관, 선호하는 코딩 스타일, 공통 규칙 | "camelCase 사용" → 모든 프로젝트에 적용 |
| **Project** | 이 프로젝트의 기술 스택, 컨벤션, 중요한 제약 | "snake_case 사용" → Project가 User를 오버라이드 |
| **Folder** | 특정 모듈의 특수한 규칙 | "JWT만 사용" → auth 폴더 작업 시만 적용 |

> [!tip] 최대 200줄 정도를 유지하며 계속 업데이트하라
> 너무 길어지면 AI 성능이 급격히 저하된다. CLAUDE.md는 인덱스이지 백과사전이 아니다.

### Progressive Disclosure — 필요한 것만 필요할 때

CLAUDE.md에 모든 내용을 넣는 대신, **"이런 상황에서는 이걸 참고해"**라고 안내하여 필요한 것만 동적으로 읽게 한다.

```
my-skill/
  SKILL.md              ← 메인 진입점
  references/
    code-style.md       ← 코드 작업 시 참고
    testing-guide.md    ← 테스트 작업 시 참고
    api-convention.md   ← API 작업 시 참고
    deploy-checklist.md ← 배포 시 참고
```

**핵심 아이디어**: 프롬프트에 "이 상황에서는 이 문서를 읽어"라고 가이드하면 AI가 동적으로 필요한 문서만 참조한다. 한꺼번에 다 주면 AI도 헷갈린다.

### Context 파일 패턴 — 동적 주입

`CLAUDE.md`가 **항상 적용되는 보편 규칙**이라면, Context 파일은 **모드별로 동적 주입**되는 규칙이다.

```
contexts/
  dev.md       ← 개발 모드: 빠른 피드백, 실험 허용
  review.md    ← 리뷰 모드: 보안·성능·품질 엄격 적용
  research.md  ← 리서치 모드: 탐색 위주, 코드 수정 금지
```

**원칙**: 에이전트가 쓰는 "모자"(hat)마다 하나의 context 파일. CLAUDE.md에 모든 모드를 섞지 않는다.

> [!warning] 안티패턴: Spaghetti CLAUDE.md
> - **증상**: 200줄 이상, 개발·리뷰·배포 규칙이 뒤섞임, 규칙끼리 모순
> - **탐지**: `wc -l CLAUDE.md`로 줄 수 확인
> - **해결**: 보편 규칙만 CLAUDE.md에 남기고, 나머지는 `contexts/`, `rules/`, `skills/`로 분리

### 구조(Scaffolding) 심화

#### 사람의 문서 vs AI의 문서 — 분리해서 관리

분리하지 않으면, 사람이 관리를 멈춘 순간 AI도 엉뚱한 맥락으로 일한다.

| 구분 | 폴더 | 내용 |
|---|---|---|
| **사람이 관리** · 비즈니스의 진실 | `docs/` | 비즈니스 룰, 도메인 정의, 체크리스트, 온보딩 가이드, ADR, API 스펙, 외부 연동 규격 |
| **AI가 남기는** 기록 · 작업의 흔적 | `.dev/` | learnings, troubleshooting 기록, 작업 로그, 디버깅 히스토리, 실험 결과, 스크래치패드 |

> 위(docs/)는 콘텐츠(사람+AI), 아래(.dev/)는 설정(AI 행동 규칙). **docs/는 사람의 책임**이다.

#### Plugins — 도구 묶음의 배포/공유

Skills + Hooks + Agents + MCP를 하나의 **패키지로 묶어서** 배포하고 공유하는 단위. 플러그인 설치 또는 Claude Code에 git 주소 넣고 "분석해줘"로 바로 사용 가능하다.

#### 경계 설정 — AI의 자유도 조절

| 질문 | 수단 | 설정 위치 |
|---|---|---|
| **뭘 알려줄까** | 프로젝트 규칙, 코딩 스타일, 금기사항 작성 | `CLAUDE.md` · `rules/` |
| **어디까지 허용할까** | Permission Mode로 자동 허용 범위 설정 (plan / auto / bypass) | `settings.json` |
| **뭘 막을까** | Hook으로 위험한 명령을 실행 전에 자동 차단 | `.claude/hooks/` |

> **이중 안전장치 예시**: CLAUDE.md에 "직접 main push 금지" 작성 + Hook으로 `git push --force` 차단 → 부탁 + 강제의 조합

---

## 2. Rules 작성 가이드 (Controller / Feedforward Layer)

> 참조: [[What is Harness?#피드포워드 (가이드) — 실행 전 방향 설정]]

### 역할

에이전트가 **첫 시도에서 좋은 결과를 낼 확률**을 높이는 사전 가이드. 실행 전에 방향을 잡아준다.

### 공통 패턴 (ECC rules/ 분석)

1. **계층화 조직** — `common/`(전사) → `{language}/`(언어별) → 프로젝트별
2. **파일 하나에 관심사 하나** — `coding-style.md`, `security.md`, `testing.md` 분리 (SRP)
3. **심각도별 정리** — CRITICAL → HIGH → MEDIUM → LOW
4. **구체적 임계값** — 모호한 지침 대신 숫자로 표현
5. **체크리스트 형식** — 모델이 항목별로 검증 가능

### 계층 구조

```
rules/
├── common/                 ← 전사 공통 (항상 로드)
│   ├── coding-style.md
│   ├── security.md
│   ├── testing.md
│   ├── performance.md
│   └── git-workflow.md
├── typescript/             ← 언어별 (프로젝트에 따라 선택)
├── python/
├── golang/
└── ...
```

> [[What is Harness?#9. 계층화 전략: Global → Domain → Local]]의 실제 구현

### 작성 템플릿

````markdown
# {관심사} Rules

## CRITICAL — 반드시 지켜야 한다 (위반 시 블로킹)
- [ ] 하드코딩된 시크릿 금지 (API 키, 비밀번호, 토큰)
- [ ] 모든 사용자 입력 검증 필수
- [ ] SQL 인젝션 방지 (파라미터화된 쿼리)

## HIGH — 강력히 권장 (위반 시 경고)
- [ ] 함수 50줄 이하
- [ ] 파일 800줄 이하
- [ ] 깊은 중첩 금지 (4단계 이상)

## MEDIUM — 권장
- [ ] 매직 넘버 대신 상수 사용
- [ ] 일관된 네이밍 컨벤션
- [ ] 의미 있는 커밋 메시지

## LOW — 선호
- [ ] TODO/FIXME 주석에 담당자 명시
- [ ] 불필요한 주석 제거
````

### Best Practice 예시: Security Rules

```markdown
# Security Rules

## CRITICAL
- [ ] 하드코딩된 시크릿 금지 — 환경 변수 또는 Secret Manager 사용
- [ ] 모든 사용자 입력을 스키마(Zod, Pydantic)로 검증
- [ ] SQL 인젝션 방지 — ORM 또는 파라미터화된 쿼리만 사용
- [ ] XSS 방지 — 사용자 입력 HTML 이스케이프
- [ ] CSRF 보호 활성화

## HIGH
- [ ] 에러 응답에 스택 트레이스 노출 금지
- [ ] 인증/인가 검사를 모든 보호된 엔드포인트에 적용
- [ ] 노출된 크리덴셜 발견 시 즉시 로테이션

## 시크릿 발견 시 대응
1. 즉시 작업 중단
2. security-reviewer agent에 에스컬레이션
3. 크리덴셜 로테이션
4. 코드베이스 전체에 유사 패턴 감사
```

### Best Practice 예시: Coding Style Rules

```markdown
# Coding Style Rules

## Core Principles
- **Immutability**: ALWAYS create new objects, NEVER mutate existing ones
- **KISS**: 단순한 해결책 우선
- **DRY**: 반복 로직 추출 (단, 조기 추상화 금지)
- **YAGNI**: 지금 필요한 것만 구현

## CRITICAL
- [ ] 불변 패턴 사용 (스프레드 연산자, Object.freeze, readonly)

## HIGH
- [ ] 함수 50줄 이하, 파라미터 5개 이하
- [ ] 파일 200-400줄 (최대 800줄)
- [ ] 깊은 중첩 대신 early return
- [ ] camelCase(변수/함수), PascalCase(타입/컴포넌트), UPPER_SNAKE_CASE(상수)

## MEDIUM
- [ ] 의미 접두사: is/has(불리언), use(훅), on/handle(이벤트)
- [ ] 매직 넘버 대신 명명된 상수
```

---

## 3. Agent 작성 가이드 (Service Layer)

> 참조: [[What is Harness?#Sub-agent]]

### 역할

여러 Skill을 조합해 **워크플로우를 오케스트레이션**하는 계층. Software 1.0의 Service Layer에 해당한다.

### 공통 패턴 (ECC 47개 agent 분석)

**Frontmatter 구조:**

```yaml
---
name: kebab-case-identifier
description: 1-2문장 목적 + 활성화 조건
tools: ["Read", "Grep", "Glob", "Bash", "Edit", "Write"]
model: opus|sonnet|haiku
---
```

**본문 구조 (6개 필수 섹션):**

| 섹션 | 역할 | 설명 |
|---|---|---|
| **Scope** | 경계 정의 | 이 agent가 다루는 것과 다루지 않는 것 |
| **Responsibilities** | 책임 목록 | 구체적 행동 리스트 |
| **Process** | 워크플로우 | 단계별 절차 (번호 매김) |
| **Output Format** | 산출물 규격 | 구조화된 출력 명세 |
| **Success Criteria** | 완료 조건 | 측정 가능한 기준 |
| **Red Flags** | 사용 금지 상황 | "When NOT to use" |

### Model 라우팅

| 복잡도 | 모델 | 용도 |
|---|---|---|
| 빠른 탐색, 단순 조회 | `haiku` | explore, writer, docs-lookup |
| 표준 작업, 실행 | `sonnet` | executor, debugger, test-engineer, security-reviewer |
| 깊은 분석, 아키텍처 | `opus` | architect, planner, code-reviewer, code-simplifier |

### 작성 템플릿

````markdown
---
name: my-agent
description: "[목적] agent. [언제 활성화되는지]."
tools: ["Read", "Grep", "Glob", "Bash"]
model: sonnet
---

## Scope
[이 agent가 다루는 영역. 명확한 경계.]

## Responsibilities
- [책임 1]
- [책임 2]
- [책임 3]

## Process
1. **분석**: [첫 번째 단계]
2. **실행**: [두 번째 단계]
3. **검증**: [세 번째 단계]
4. **보고**: [결과 정리]

## Output Format
```
### Summary
[한줄 요약]

### Findings
| 항목 | 심각도 | 설명 | 해결 방안 |
|---|---|---|---|
| ... | CRITICAL/HIGH/MEDIUM/LOW | ... | ... |

### Verdict
[APPROVE / WARNING / BLOCK]
```

## Success Criteria
- [ ] [측정 가능한 조건 1]
- [ ] [측정 가능한 조건 2]

## Red Flags — When NOT to Use
- [이런 상황에서는 사용하지 않는다]
- [대신 이 agent를 사용한다]
````

### Best Practice 예시: Code Reviewer

```yaml
---
name: code-reviewer
description: "코드 품질·보안·유지보수성 리뷰 agent. PR 생성 전 또는 코드 변경 후 활성화."
tools: ["Read", "Grep", "Glob", "Bash"]
model: opus
---
```

**핵심 설계 원칙:**

1. **신뢰도 필터링** — 80% 이상 확신할 때만 보고. 노이즈 제거.
2. **4단계 심각도** — CRITICAL(시크릿 노출, 인젝션) → HIGH(큰 함수, 테스트 누락) → MEDIUM(성능) → LOW(스타일)
3. **구조화된 판정** — 3가지 중 하나: Approve / Warning / Block
4. **파일 참조 필수** — 모든 발견에 `파일:라인` 포함

### Best Practice 예시: Planner

```yaml
---
name: planner
description: "복잡한 기능 기획·리팩토링·아키텍처 변경 계획 agent."
tools: ["Read", "Grep", "Glob", "Bash"]
model: opus
---
```

**핵심 설계 원칙:**

1. **4단계 프로세스** — Requirements Analysis → Architecture Review → Step Breakdown → Implementation Order
2. **Worked Example 포함** — 실제 사례(예: Stripe 구독 결제 구현)를 템플릿에 내장하여 모델이 기대 수준을 이해
3. **Red Flags 체크리스트** — 큰 함수, 깊은 중첩, 중복 코드, 누락된 에러 핸들링, 하드코딩 값, 테스트 부재

### Best Practice 예시: Loop Operator

```yaml
---
name: loop-operator
description: "자율 에이전트 루프 관리 agent. 안전 가드레일 포함."
tools: ["Read", "Grep", "Glob", "Bash", "Edit", "Write"]
model: sonnet
---
```

**핵심 설계 원칙:**

1. **명시적 정지 조건** — 무한 루프 방지
2. **에스컬레이션 트리거** — 연속 2회 체크포인트 무진전, 동일 스택 트레이스 반복, 비용 초과
3. **사전 필수 검사** — 품질 게이트 활성, 평가 기준선 존재, 롤백 경로 확보, 브랜치 격리 확인

> [!warning] 안티패턴: God Skill / Long Method
> - **증상**: 하나의 agent가 10개 이상 skill을 순차 호출
> - **탐지**: Process 섹션의 단계가 10개 초과
> - **해결**: sub-agent로 분리, 각 agent는 3-5개 skill 호출로 제한

---

## 4. Skill 작성 가이드 (Domain Layer)

> 참조: [[What is Harness?#Skills]]

### 역할

**하나의 명확한 책임**을 가진 도메인 지식 캡슐. Software 1.0의 Domain Object(SRP)에 해당한다.

### 공통 패턴 (ECC 181개 skill 분석)

**디렉토리 구조:**
```
skills/
  api-design/
    SKILL.md
  tdd-workflow/
    SKILL.md
  backend-patterns/
    SKILL.md
```

**Frontmatter:**
```yaml
---
name: skill-identifier
description: "[목적]. [활성화 트리거 조건]."
origin: custom
---
```

**본문 구조 (6개 필수 섹션):**

| 섹션 | 역할 |
|---|---|
| **When to Activate** | 트리거 조건 (파일 타입, 코드 패턴, 워크플로우 단계) |
| **Core Concepts** | 핵심 도메인 지식 |
| **Code Examples** | 멀티 언어 구현 예시 |
| **Best Practices** | 실천 가능한 체크리스트 |
| **Checklists** | 검증 항목 |
| **Red Flags** | 이 skill이 잘못 사용되는 징후 |

### 크기 가이드라인

| 유형 | 줄 수 | 예시 |
|---|---|---|
| 단일 패턴 | 100-200 | strategic-compact |
| 표준 도메인 | 200-400 | tdd-workflow, frontend-patterns |
| 대형 도메인 | 400-800 | api-design, backend-patterns |
| 800줄 초과 | 분리 필요 | sub-skill로 분할 |

### 작성 템플릿

````markdown
---
name: my-skill
description: "[도메인] 패턴과 best practice. [트리거 조건] 시 활성화."
origin: custom
---

# {Skill 이름}

## When to Activate
- [파일 타입/패턴 1]에서 작업할 때
- [워크플로우 단계]에 진입했을 때
- [특정 키워드/명령]이 호출되었을 때

## Core Concepts

### {개념 1}
[설명 + 왜 중요한지]

### {개념 2}
[설명 + 왜 중요한지]

## Code Examples

### TypeScript
```typescript
// Good
[좋은 예시 코드]

// Bad — [이유]
[나쁜 예시 코드]
```

### Python
```python
# Good
[좋은 예시 코드]
```

## Best Practices
- [실천 항목 1]
- [실천 항목 2]

## Checklist
- [ ] [검증 항목 1]
- [ ] [검증 항목 2]

## Red Flags
- [이 skill이 잘못 사용되는 징후 1]
- [해결: ...]
````

### Best Practice 예시: TDD Workflow

```yaml
---
name: tdd-workflow
description: "테스트 주도 개발 워크플로우. 새 기능 구현 또는 버그 수정 시 활성화."
origin: ECC
---
```

**핵심 구조:**

```
RED (실패하는 테스트 작성)
  ↓
GREEN (테스트 통과하는 최소 구현)
  ↓
REFACTOR (테스트 유지하며 코드 개선)
  ↓
COMMIT (체크포인트 커밋)
```

**강제 규칙:**
- 최소 80% 커버리지
- Unit(함수) + Integration(API/서비스) + E2E(사용자 흐름) 3계층
- Arrange-Act-Assert 패턴
- 테스트 간 독립성 보장 (공유 상태 금지)

### Best Practice 예시: API Design

```yaml
---
name: api-design
description: "REST API 설계 패턴. 새 엔드포인트 생성 또는 API 리팩토링 시 활성화."
origin: ECC
---
```

**11점 출시 전 체크리스트:**
1. RESTful 리소스 URL (kebab-case, 복수형, 동사 금지)
2. 올바른 HTTP 메서드 (GET=조회, POST=생성, PUT=전체수정, PATCH=부분수정, DELETE=삭제)
3. 적절한 상태 코드 (200/201/204/400/401/403/404/409/422/500)
4. 일관된 에러 응답 형식 (code, message, details)
5. 페이지네이션 (offset 또는 cursor 기반)
6. 필터링·정렬·검색 쿼리 파라미터
7. 인증·인가 적용
8. Rate limiting 헤더 (X-RateLimit-*)
9. 버전 관리 (/api/v1)
10. 폐기 정책 (6개월 사전 통보, Sunset 헤더)
11. OpenAPI/Swagger 문서

> [!warning] 안티패턴: Feature Envy / Duplication
> - **Feature Envy**: Skill이 다른 Skill의 데이터를 과도하게 참조 → 병합 또는 구조 변경
> - **Duplication**: 비슷한 프롬프트가 여러 Skill에 복사 → 공통 Skill로 추출

---

## 5. MCP 설정 가이드 (Infrastructure Layer)

> 참조: [[What is Harness?#MCP]]

### 역할

외부 시스템(DB, API, 파일, 브라우저)에 대한 **접근 추상화**. Software 1.0의 Infrastructure/Adapter Layer에 해당한다.

### 공통 패턴 (ECC 30개 서버 분석)

**설정 구조:**

```json
{
  "mcpServers": {
    "server-name": {
      "command": "npx",
      "args": ["-y", "@package/mcp-server"],
      "env": {
        "API_KEY": "${API_KEY}"
      }
    }
  }
}
```

### 핵심 제약

> [!important] 활성 MCP는 10개 이하
> 각 MCP 서버는 ~200 토큰의 오버헤드를 소비한다. 10개 = ~2,000 토큰이 **항상** 컨텍스트에서 차감. [[What is Harness?#(3) 스킬 폭증 (Skill Bloat)]] 참조.

### 카테고리별 정리

| 카테고리 | 예시 | 활성화 시점 |
|---|---|---|
| **이슈 관리** | GitHub, Jira | 기능/버그 작업 시 |
| **데이터베이스** | Supabase, PostgreSQL | 데이터 관련 작업 시 |
| **메모리** | Knowledge graph | 장기 실행 세션 |
| **웹/브라우저** | Playwright, Firecrawl | UI 테스트, 크롤링 |
| **검색** | Exa, Context7 | 리서치 작업 시 |
| **배포** | Vercel, Railway | DevOps 작업 시 |
| **개발 도구** | Filesystem, Docker | 일반 개발 |

### 프로젝트 유형별 권장 조합

| 프로젝트 유형 | 필수 MCP | 선택 MCP |
|---|---|---|
| **웹 풀스택** | GitHub, Supabase, Playwright | Vercel, Exa |
| **API 서버** | GitHub, DB(PostgreSQL) | Docker, Monitoring |
| **데이터 분석** | Filesystem, DB | Exa, Context7 |
| **오픈소스** | GitHub, Filesystem | Exa, Context7 |

### 작성 템플릿

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_TOKEN": "${GITHUB_TOKEN}"
      }
    },
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/project"]
    }
  }
}
```

> [!warning] 안티패턴: MCP 없는 하드코딩
> - **증상**: 프롬프트에 `curl` 명령이나 API URL을 직접 기술
> - **해결**: 외부 시스템 접근은 반드시 MCP 서버로 추상화

---

## 6. Hooks 작성 가이드 (Feedback Layer)

> 참조: [[What is Harness?#(2) 자동 강제 시스템 — 린터·Hooks·CI]]

### 역할

에이전트의 산출물을 **자동으로 검증하고, 위반을 구조적으로 차단**하는 피드백 센서. [[What is Harness?#강제 vs 부탁 — Harness의 핵심 철학]]의 "강제"를 구현하는 계층.

### 공통 패턴 (ECC hooks.json 분석)

**이벤트 타입:**

| 이벤트 | 시점 | 용도 |
|---|---|---|
| `PreToolUse` | 도구 실행 **전** | 입력 검증, 위험 작업 차단 |
| `PostToolUse` | 도구 실행 **후** | 산출물 품질 검사 |
| `Stop` | 응답 완료 시 | 배치 포맷팅, 세션 저장 |
| `SessionStart` | 세션 시작 시 | 컨텍스트 로드, 환경 감지 |
| `SessionEnd` | 세션 종료 시 | 상태 저장, 정리 |

**Hook 구조:**

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "command": "node scripts/hooks/preflight.js",
        "timeout": 10000
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "command": "node scripts/hooks/quality-gate.js",
        "timeout": 30000
      }
    ],
    "Stop": [
      {
        "matcher": "",
        "command": "node scripts/hooks/session-persist.js",
        "timeout": 5000
      }
    ]
  }
}
```

### 카테고리별 설계

| 카테고리 | 이벤트 | 예시 |
|---|---|---|
| **품질 게이트** | PostToolUse | lint, type check, test 자동 실행 |
| **디자인 체크** | PostToolUse | 아키텍처 규칙 위반 감지 |
| **거버넌스** | PreToolUse | 시크릿 탐지, 위험 명령 차단 |
| **지속 학습** | PostToolUse | 패턴 추출, 관찰 기록 |
| **세션 관리** | SessionStart/End | 컨텍스트 로드/저장 |

### 설계 원칙

> [!tip] 성공은 조용히, 실패는 시끄럽게
> 테스트가 통과하면 조용히 넘어가고, 실패할 때만 크게 알린다. 노이즈를 줄이고 신호에만 집중. — [[What is Harness?#(2) 자동 강제 시스템 — 린터·Hooks·CI]]

- **standard 모드**: 경고만 출력, 진행은 허용
- **strict 모드**: 위반 시 진행 차단 (CRITICAL 규칙용)
- **timeout 설정**: 빠른 검사 5-10초, 무거운 검사 30-300초

### 작성 템플릿

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "command": "node scripts/hooks/dangerous-command-check.js",
        "timeout": 5000
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "command": "node scripts/hooks/lint-and-typecheck.js",
        "timeout": 30000
      }
    ],
    "Stop": [
      {
        "matcher": "",
        "command": "node scripts/hooks/session-save.js",
        "timeout": 5000
      }
    ],
    "SessionStart": [
      {
        "matcher": "",
        "command": "node scripts/hooks/context-load.js",
        "timeout": 10000
      }
    ]
  }
}
```

> [!warning] 안티패턴: Leaky Abstraction
> - **증상**: Hook이 특정 MCP 서버의 내부 구현에 의존
> - **해결**: Hook은 도구의 입력/출력만 관찰, 내부 상태에 의존하지 않음

---

## 7. 계획(Planning) 가이드

### 역할

AI에게 일을 시키기 **전에**, 무엇을 할지 함께 정하는 단계. **한 번에 완벽하게**가 아니라 **반복해서 수렴**하는 구조.

```
계획(Plan) → 실행(Execute) → 검증(Verify)
    ↑                              │
    └──── 미달 시 피드백 ──────────┘
```

### "해줘"의 함정

| 안티패턴: "해줘" | 패턴: "같이 계획부터" |
|---|---|
| "이거 만들어줘" | "~~할 건데 같이 계획 세워보자" |
| ↓ AI가 알아서 만듦 | ↓ AI가 계획 작성 |
| ↓ 검수 → "아닌데..." | ↓ 사람이 검토 → 수정 → 승인 |
| ↓ 다시 시킴 → 검수 → 다시... | ↓ 승인된 계획대로 실행 |
| **시간만 날림** | **높은 성공률** |

> 계획과 실행을 **분리**하면 검수 횟수가 줄고, 결과의 예측 가능성이 올라간다.

### AskUserQuestion — AI가 맥락을 스스로 채우게 하기

"해줘"가 아니라 **"물어봐"**. 커스텀 Plan 스킬에서 AskUserQuestion을 사전에 넣어 모호한 지점을 질문으로 끌어낸다.

**예시**: "결제 시스템을 리팩토링하고 싶어" → AI가 질문:
- "멀티 PG 연동은 단일업무? 다중?" 
- "가격 정책도 포함하나?"  
- "기존 DB 스키마는 건드려도 되나?"

→ 모호한 점이 없으니 정확한 계획이 나온다.

### 커스텀 Plan 스킬 — 기본 Plan Mode를 넘어서

기본 Plan Mode는 계획과 실행을 분리하지만, 모든 걸 프롬프트에 담을 수 없다. **인터뷰 + 요구사항 도출 + 플랜 파일까지 자동화**하는 전용 스킬을 만든다.

```
1. 목표 확인 — 사용자의 의도를 미러링
   ↓
2. 인터뷰 — 모호한 지점을 질문으로 끌어내기
   ↓
3. 요구사항 + 태스크 도출
   ↓
4. 플랜 파일로 떨구기 → /execute로 실행
```

> implicit(머릿속) → explicit(플랜 파일). 계획 단계에서 **모호함을 줄이는 것**이 실행 품질을 결정한다.

### 관련 도구

| 도구 | 용도 |
|---|---|
| `/specify` | 스펙 파일을 더 잘 얻기 위한 스킬. 인터뷰 → 요구사항 도출 → 플랜 파일까지 자동화 |
| `/deep-interview` | 깊이 있는 인터뷰로 unknown-unknown을 줄인다. 미처 생각 못한 엣지 케이스까지 끌어낸다 |
| `/clarify` | 요구사항을 명확하게 하고 싶을 때. 모호한 지시를 구체적인 스펙으로 변환 |

---

## 8. 실행(Orchestration) 가이드

### 역할

계획이 확정된 후, **어떤 패턴으로 AI에게 일을 시킬 것인가**를 결정하는 단계. 상황에 맞는 실행 패턴을 선택하는 것이 핵심.

### 3가지 실행 패턴

| 패턴 | 구조 | 적합한 상황 | 비용 |
|---|---|---|---|
| **혼자 (Single)** | AI 1개 → 결과물 | 단순 작업, 대부분의 일상 | 1x |
| **부하 파견 (Subagent)** | 메인 → 조사/작업/검증 병렬 → 결과 종합 | 병렬/전문화 가능한 작업 | 2-3x |
| **팀 협업 (Team Mode)** | PM/개발/디자인/QA 등 에이전트 간 소통 | 다관점·복잡 작업 | ~7x |

> **90%는 단일/서브에이전트로 충분**하다. Team Mode는 비용 대비 효과를 따져서 사용.

### 상황별 오케스트레이션 패턴

**순차 파이프라인**: 순서가 중요한 작업

```
상황: 블로그 글을 쓰려는데 조사 → 초안 → 퇴고 → 발행 순서가 중요하다
"AI 에이전트 트렌드를 조사해서 → 초안 쓰고 → 퇴고까지 순서대로 진행해줘"
도구: TaskCreate로 제거식 순서 제어, 히스토리 적절히 유지
```

**병렬 Subagent**: 독립적인 작업을 동시에

```
상황: 경쟁사 3곳의 랜딩 페이지를 동시에 분석하고 싶다. 서로 독립적.
"사이트 A, B, C 각 랜딩 페이지를 각각 에이전트가 분석하여, 공시 비교로 들어와"
도구: Agent 3개 spawn, 병렬 분석 후 메인이 비교
```

**Team Mode**: 다관점 협업

```
상황: 새 기능을 설계하면서 동시에 구현하고 리뷰도 받아야 한다. 에이전트끼리 소통이 필요.
"Team 가서 설계, 구현자, 리뷰어 3명으로 나서서 구현자가 바로 시작"
도구: TeamCreate로 팀 생성, 에이전트 간 직접 소통
```

### Ralph Loop — 될 때까지 반복

**완료 기준을 먼저 정하고**, 충족할 때까지 AI가 알아서 돈다. "뭐가 되면 끝인지"만 정해주면 AI가 될 때까지 계속 돈다.

**예시**: "랜딩 페이지 만들어줘"

| 단계 | 결과 |
|---|---|
| 완료 기준 합의 | 모바일 반응형 + Lighthouse 90점 이상 + 카피 3번 이상 퇴고 |
| 1차 | 페이지 완성, Lighthouse 72점 |
| 2차 | 92점, 모바일 레이아웃 깨짐 |
| 3차 | 레이아웃 수정, 카피 퇴고 2회만 |
| **4차** | **전항목 충족 — PASS** |

> 사람이 한 일: 기준 정하기 1번. 나머진 AI가 알아서.

### Auto Research — 자율 실험 루프

Karpathy/autoresearch 패턴: 사람은 **방향(program.md)만 정하고**, AI가 밤새 실험을 돌린다.

```
코드 수정(train.py 변경) → 실행(5분간 학습) → 평가(성능 비교) → 판단(유지/폐기)
     └──────────────── 자동 반복 ────────────────────┘
```

- AI가 `train.py`만 수정 — 아키텍처, 하이퍼파라미터 변경
- 시간당 ~12개 실험 자율 수행, 밤새 무인 운영
- 성능 개선되면 유지, 아니면 폐기 → 다음 실험

> 핵심 패턴: **수정 → 실행 → 평가 → 반복** — 사람은 방향(program.md)만 잡아주면 된다.

### 관련 도구

| 도구 | 용도 |
|---|---|
| `/agent-orchestrate` | 현재 문제에 최적화된 오케스트레이션 패턴을 자동 적용. 단일/병렬/파이프라인 등 상황에 맞게 선택 |
| `ralph` | 실행 결과를 자동 검증하는 공식 플러그인. Claude Code에서 바로 사용 가능 |
| `autoresearch` | 사람은 방향만 정하고 AI가 밤새 실험. 수정→실행→평가→반복 루프를 자율적으로 돌린다 |

> 실행은 **패턴 선택 + 자율성 범위 설정**이 핵심이다.

---

## 9. 검증(Verification) 가이드

### 역할

결과물을 **어떻게 믿을 것인가**. 기준 없는 검증은 "대충 끝났다고 하기"와 같다.

### 검증 원칙

#### 01 — 기준이 있어야 검증이 가능하다 (Sprint Contract)

작업 전에 **"뭘 만들고 어떻게 검증할지" 합의**한다. 기준 없이 시키면 AI가 끝없이 하거나 대충 끝냈다고 한다.

1. **완료 조건을 먼저 정한다** — "이 3가지가 되면 끝" — 시작 전에 합의
2. **조건을 측정 가능하게 쓴다** — "잘 되게" 말고 "테스트 통과 + 빌드 성공"
3. **미달이면 다시 돌린다** — 기준이 있으니 자동 반복이 가능해진다

> Ralph Loop — 기준 달성까지 반복시킨다. 기준이 명확하면 자동화가 가능.

#### 02 — 컨텍스트를 나누고, 관점을 분리한다 (Generator / Evaluator)

**같은 컨텍스트에서 만들고 평가하면 안 된다.** 자기 작업을 평가하면 quality가 mediocre해도 자신있게 칭찬한다.

> "만든 AI와 확인하는 AI를 분리하는 것이 **가장 강력한 레버**" — Anthropic
> "Evaluator를 회의적으로 튜닝하는 게, Generator를 자기비판적으로 만드는 것보다 **훨씬 쉽다**"

#### 03 — 모델도 나누고, 역할도 나눈다

| 모델 | 역할 | 검증 대상 |
|---|---|---|
| **Codex** | 코드 리뷰 | 로직 오류, 보안 취약점, 테스트 누락 검출 |
| **Gemini** | 문서 리뷰 | 일관성, 정확성, 구조 검증 |
| **Opus / Sonnet** | 성능별 분업 | Opus: 복잡한 판단, 아키텍처 / Sonnet: 빠른 확인, 반복 검증 |

> "Out of the box, Claude is a poor QA agent." — 검증 에이전트도 튜닝이 필요하다. 기준을 구체적으로, 회의적으로 설정해야 쓸 수 있다.

#### 04 — 에이전트에게 눈을 달아주기 (시각 검증)

코드만 검증하는 게 아니라, **화면도 직접 보고 확인**할 수 있어야 한다.

| 도구 | 방식 | 용도 |
|---|---|---|
| **Browser Agent** (chrome-cdp, agent-browser) | 실제 Chrome 브라우저를 제어. DOM 탐색, 클릭, 스크린샷, 네비게이션 | 웹앱의 UX를 직접 검증 |
| **Computer Use** (built-in MCP) | 스크린샷 + 마우스/키보드로 모든 앱 제어 | 웹이 아닌 네이티브 앱, 디자인 도구도 검증 가능 |
| **시각 검증 루프** (패턴) | generate → screenshot → evaluate → 수정 | 사람이 눈으로 하는 것을 AI가 대신 |

> 검증 범위를 **코드에서 화면까지 확장**하면 사람이 끼어들 일이 줄어든다.

### 안전장치 — "실수해도 괜찮은 구조"를 만드는 것이 핵심

| 전략 | 설명 | 수단 |
|---|---|---|
| **되돌릴 수 있는 환경** | 브랜치/Worktree 격리에서 작업. 실수해도 메인은 안전 | `git worktree add` |
| **위험한 건 사람이 확인** | 삭제, 배포, 외부 발송 같은 작업은 승인 후 실행 | Runtime Gate |
| **Dry-run 먼저** | "이렇게 할 건데 맞나?" 미리보기 후 실행 | `--dry-run` |

### 관련 도구

| 도구 | 용도 |
|---|---|
| `/qa` | Browser Agent, Computer Use 도구를 활용해서 QA를 자동화하는 스킬. 화면을 직접 보고, 클릭하고, 검증한다 |
| `verify 레퍼런스` | 검증 스킬을 더 깊이 이해하고 싶다면 참고. 검증 전략, 패턴, 실제 적용 사례가 정리되어 있다 |

> 검증을 자동화하면 사람은 **판단에만 집중**할 수 있다.

---

## 10. 횡단 관심사

### 7.1 토큰 최적화

```bash
# 권장 설정
MAX_THINKING_TOKENS=10000                # thinking 비용 70% 절감
CLAUDE_AUTOCOMPACT_PCT_OVERRIDE=50       # 50%에서 조기 압축 → 품질 향상
```

**모델 라우팅으로 비용 제어:**
- 일상 작업: Sonnet (Opus 대비 60% 절감)
- 깊은 분석: Opus (아키텍처, 보안, 복잡한 리뷰)
- 단순 탐색: Haiku (파일 검색, 간단한 질의)

**MCP 토큰 관리:**
- 미사용 MCP 비활성화 (각 ~200 토큰)
- 활성 MCP 10개 이하 유지

### 7.2 세션 맥락 관리 — 쌓이면 비워라

컨텍스트는 채우는 것만큼 **비우는 것**도 중요하다.

| 사용량 | 상태 | 권장 행동 |
|---|---|---|
| ~20% | 쾌적 | 그대로 작업 |
| ~50% | 주의 | `/compact` — 오래된 대화를 요약·압축. 같은 주제를 이어갈 때 |
| ~80% | 위험 | `/clear` 또는 새 세션 — 컨텍스트 완전 초기화. 다른 주제로 전환할 때 |

**handoff**: 현재 세션의 맥락을 파일로 저장 → 새 세션에서 이어받기. 맥락 손실 없이 전환.

> [!tip] 개인 기준: 20~30%에서 새로 시작
> 아예 다른 맥락의 작업을 하게 되면 /clear. 이어가야 하면 handoff로 맥락을 넘긴다.

### 7.3 전략적 압축 (Strategic Compaction)

**언제 압축하는가:**
- 리서치 완료 **후**, 구현 시작 **전** (발견한 것을 정리)
- 마일스톤 달성 **후**, 다음 페이즈 **전** (상태 정리)

**언제 압축하면 안 되는가:**
- 구현 **중간** — 작업 중인 변수 스코프, 의사결정 맥락이 유실됨

```
리서치 → [압축] → 구현 → [마일스톤] → [압축] → 다음 구현 → ...
```

> [[What is Harness?#(1) 컨텍스트 부패 (Context Rot)]] 방지의 핵심 전략

### 7.3 지속 학습 (Continuous Learning)

ECC의 Instinct 시스템: 에이전트가 반복하는 패턴을 **자동 추출하고 Skill로 진화**시킨다.

```
실수 발생 → Harness 수정 → 패턴 추출 → 신뢰도 점수 부여 → Skill로 승격
```

> [!important] 같은 실수가 두 번 일어나면, 프롬프트가 아니라 Harness를 고친다
> — [[What is Harness?#강제 vs 부탁 — Harness의 핵심 철학]]

### 7.4 개선(Compound) 심화 — 관측하고 개선하기

#### 관측하기

- **세션 분석**: 프롬프트 패턴 + Skill/Agent 호출 빈도를 분석. 어디서 시간을 쓰는지, 어디서 실패하는지 파악
- **AI Slop 감지**: 작업하다 보면 불필요한 코드, 중복 설정, 안 쓰는 규칙이 쌓인다. 이게 AI slop

#### 개선하기 — 3번 규칙

| 패턴 | 행동 |
|---|---|
| **같은 작업을 3번 반복** | → Skill로 만들어서 재사용 |
| **같은 실수를 3번 반복** | → Rule 또는 CLAUDE.md에 명시 |
| **Skill이 잘 안 동작** | → 세션 분석 → 병목 발견 → Skill 개선 (스킬도 계속 다듬는다) |

```
작업 → 관측(세션+사용 패턴) → 패턴 발견 → Skill 또는 Rule → 더 나은 작업
```

#### 단순화하기

- **안 쓰는 건 치운다** — 필요 없어진 Skill, MCP, Rule은 바로 삭제. 쌓이면 AI slop
- **모델이 좋아지면 Harness를 재평가** — 예전에 필요했던 가드레일이 지금은 불필요할 수 있다
- **과설계 신호를 인식하기** — 설정이 너무 복잡하면 뭔가 잘못된 것. 점점 단순해져야 정상

> Anthropic — "Harness의 공간은 모델이 좋아져도 줄어들지 않는다. **이동할 뿐**이다."

#### 자가 진단 체크리스트

| 잘 가고 있다는 신호 | 실패하고 있다는 징후 |
|---|---|
| 같은 말을 두 번 하지 않는다 (맥락 전달 OK) | 검수에 시간이 더 오래 걸린다 (검증 자동화 필요) |
| 실수가 규칙이 된다 (개선 루프 동작) | 시켰는데 원하는 결과가 안 나온다 (맥락/계획 부족) |
| 차단 장치가 뭔가를 막고 있다 (사고 예방) | 스킬·에이전트가 많은데 잘 안 쓴다 (context pollution) |
| 불필요한 것이 줄어든다 (단순화 진행) | 가이드 파일이 길어지고 관리 안 된다 (분리/정리 필요) |

> **좋은 Harness는 점점 단순해진다.** 복잡해지고 있다면 뭔가 잘못된 것.

#### session-wrap 플러그인

Claude Code 세션이 끝난 뒤, 세션 내용을 분석해서 인사이트를 자동 추출하는 플러그인.

| 기능 | 설명 |
|---|---|
| **패턴 발견** | 반복 작업 패턴을 감지해서 Skill 후보로 제안 |
| **실수 추출** | 세션 중 실수를 정리해서 Rule 후보로 제안 |
| **문서 업데이트** | CLAUDE.md, docs 업데이트 필요 항목 감지 |

> 세션 분석 → 패턴 발견 → Skill/Rule 추가 — 이것이 개선 루프의 핵심

### 7.5 Garbage Collection

> 참조: [[What is Harness?#(3) Garbage Collection — 누적되는 부패 막기]]

**청소 대상과 도구:**

| 대상 | 탐지 도구 | 설명 |
|---|---|---|
| 미사용 파일/export | `knip` | 데드코드 감지 |
| 미사용 의존성 | `depcheck` | 불필요한 npm 패키지 |
| 미사용 TypeScript export | `ts-prune` | 사용되지 않는 타입/함수 |
| 문서 드리프트 | 수동 비교 | 문서와 실제 코드 불일치 |

**원칙**: 정기적으로 청소 에이전트를 실행하여 코드베이스 엔트로피를 관리한다.

---

## 11. 안티패턴 종합

> 참조: [[What is Harness?#10. 안티패턴]]

| 안티패턴 | 계층 | 증상 | 탐지법 | 해결책 |
|---|---|---|---|---|
| **Spaghetti CLAUDE.md** | Config | 200줄+, 규칙 충돌 | `wc -l` | contexts/, rules/로 분리 |
| **God Skill** | Domain | 하나의 Skill이 모든 기능 처리 | 책임 수 카운트 | SRP 적용, 분할 |
| **MCP Hardcoding** | Infra | 프롬프트에 API URL 직접 기술 | URL 패턴 grep | MCP 서버로 추상화 |
| **Leaky Abstraction** | Service/Infra | Agent가 MCP 내부 구현에 의존 | 결합도 분석 | 인터페이스 경계 강화 |
| **Circular Dependency** | Domain | Skill 간 순환 호출 | 의존성 그래프 | DAG 구조로 변환 |
| **Feature Envy** | Domain | Skill이 다른 Skill 데이터 과다 참조 | 교차 참조 수 | 병합 또는 재구조화 |
| **Long Method** | Service | Agent가 10+ skill 순차 호출 | Process 단계 수 | Sub-agent 분리 |
| **Skill Bloat** | 전체 | MCP/Skill 과다 → 모델 혼란 | 활성 수 카운트 | 10개 이하로 제한, 불필요 제거 |

> [!note] 공통 원인
> Software 1.0에서 배운 설계 원칙(SRP, 레이어 분리, 추상화)을 3.0에서 적용하지 않을 때 발생한다. **원칙은 같고, 적용 대상만 달라졌다.** — [[What is Harness?#10. 안티패턴]]

---

## 12. 프로젝트 부트스트랩 체크리스트

새 프로젝트에 Harness를 구축할 때의 최소 시작점:

### Phase 1: 기반 (Day 1)
- [ ] `CLAUDE.md` 작성 (60줄 이하: 프로젝트 개요, 구조, 빌드 명령, 핵심 컨벤션)
- [ ] `contexts/` 폴더 생성 (최소 `dev.md`, `review.md`)
- [ ] `rules/common/` 에 `security.md`, `testing.md` 배치

### Phase 2: 에이전트 (Week 1)
- [ ] 핵심 agent 3개 정의: `planner`, `executor`, `reviewer`
- [ ] 프로젝트 도메인 skill 최소 1개 작성
- [ ] `hooks.json`에 기본 품질 게이트 설정 (lint, test)

### Phase 3: 인프라 (Week 1-2)
- [ ] MCP 서버 구성 (10개 이하, 필수만)
- [ ] 세션 영속성 hook 설정 (SessionStart/End)

### Phase 4: 진화 (Ongoing)
- [ ] 첫 실패 후 → 해당 실패를 방지하는 rule 또는 hook 추가
- [ ] 반복되는 패턴 → skill로 추출
- [ ] 주기적 GC 실행 (문서 드리프트, 데드코드)

> [!tip] 완벽한 Harness로 시작할 필요 없다
> 가장 간단한 구성에서 시작하고, **실패가 가르치는 대로** 진화시킨다. 모든 Harness 구성요소는 가정을 인코딩한다 — 그 가정이 맞는지는 실행해봐야 안다.

---

## 13. References

- [[What is Harness?]] — Harness 이론적 프레임워크 (이 문서의 기반)
- [everything-claude-code](https://github.com/affaan-m/everything-claude-code) — 47 agents, 181 skills, hooks 참조 구현
- [Harness Design for Long-Running Apps — Anthropic](https://www.anthropic.com/engineering/harness-design-long-running-apps) — GAN 영감 다중 에이전트 패턴
- [Effective Harnesses for Long-Running Agents — Anthropic](https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents) — 장기 실행 에이전트 패턴
- [Harness Engineering — Martin Fowler](https://martinfowler.com/articles/harness-engineering.html) — 피드포워드/피드백 제어 이론
- 하네스엔지니어링 (Team Attention 이호연, 2026.04.07) — 6축 순환 구조, 계획/실행/검증/개선 실전 패턴
- [team-attention/harness](https://github.com/team-attention/harness) — 강의자료, harness-checklist.md, Harness 플러그인
- [team-attention/hoyeon](https://github.com/team-attention/hoyeon) — 개인 Harness 전체 구성 참조 레포
- [plugins-for-claude-natives](https://github.com/plugins-for-claude-natives) — Team Attention에서 자주 쓰는 실무 검증된 플러그인 모음
- [karpathy/autoresearch](https://github.com/karpathy/autoresearch) — 자율 실험 루프 (수정→실행→평가→반복)
- [garrytan/gstack](https://github.com/garrytan/gstack), [obra/superpowers](https://github.com/obra/superpowers), [oh-my-claudecode](https://github.com/Yeachan-Heo/oh-my-claudecode) — Harness 오픈소스 도구
