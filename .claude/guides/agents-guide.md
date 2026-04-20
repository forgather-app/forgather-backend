# Agents 활용 · 작성 가이드

서브 에이전트(Sub-agent)는 하네스의 **Service Layer**에 해당한다. 여러 Skill을 조합해 워크플로우를 오케스트레이션한다.
상세 원리는 `.claude/guides/harness-guideline.md` §3 참조.

## Agents란?

Claude Code의 서브 에이전트로, 특정 작업에 특화된 분석/처리를 수행한다. Skills와 달리 **읽기 전용 분석**에 특화되어 있으며, Claude가 작업 컨텍스트에 따라 자동으로 호출한다.

## Skills vs Agents

| 구분 | Skills | Agents |
|-----|--------|--------|
| **용도** | 코드 생성/수정 | 분석/조사·오케스트레이션 |
| **호출** | `/skill-name` 명시적 호출 | 자동 또는 명시적 요청 |
| **도구** | Read, Write, Edit 등 | 주로 Read, Grep, Glob |
| **예시** | `/admin-ui`, `/test-writer` | `jpa-analyzer`, `analyze-admin`, `tech-lead` |

## 호출 방법

| 방법 | 예시 |
|-----|------|
| **자동** | "N+1 문제 있는지 확인해줘" → `jpa-analyzer` 자동 호출 |
| **명시적** | "jpa-analyzer로 ProductService 분석해줘" |

---

## Agent 작성 규격

### 프런트매터 (표준 4필드)

```yaml
---
name: kebab-case-identifier
description: 1-2문장 목적 + 활성화 조건
tools: ["Read", "Grep", "Glob"]
model: opus | sonnet | haiku
---
```

> `allowed-tools`, `user-invocable`, `context`, `agent` 등 비표준 필드는 사용하지 않는다.

### 본문 6섹션 (필수)

| 섹션 | 역할 |
|---|---|
| `## Scope` | 다루는 영역과 경계 (1~2문장) |
| `## Responsibilities` | 구체적 책임 리스트 |
| `## Process` | 단계별 워크플로우 (번호 매김) |
| `## Output Format` | 산출물 규격 (표/템플릿) |
| `## Success Criteria` | 측정 가능한 완료 조건 (체크박스) |
| `## Red Flags — When NOT to Use` | 사용 금지 상황과 대체 agent |

### Model 라우팅

| 복잡도 | 모델 | 용도 |
|---|---|---|
| 빠른 탐색, 단순 조회 | `haiku` | 파일 검색, 단순 질의 |
| 표준 작업·실행 | `sonnet` | debugger, test-engineer, security-reviewer |
| 깊은 분석·아키텍처 | `opus` | architect, planner, code-reviewer, tech-lead |

---

## 프로젝트 Agents

### 분석 Agent
- **`jpa-analyzer`** (opus) — JPA 코드 분석 및 N+1 감지. HIGH/MEDIUM/LOW 심각도별 보고
- **`analyze-admin`** (opus) — 어드민 UI 패턴 분석 (CSS 변수, 전역 JS 객체, Fragment)

### 리뷰 Agent
- **`tech-lead`** (opus) — 7명 전문 리뷰어 오케스트레이션. `docs/review/`에 결과 문서화
- **`code-reviewer`** (opus) — git diff 기반 변경 코드 리뷰
- **`planner`** (opus) — 복잡 기능/리팩토링 계획 수립

### 전문 리뷰어 (tech-lead가 위임)
`review/` 하위 — security / architecture / performance / code-quality / infra / api-design / db-schema 7종

---

## 안티패턴

| 패턴 | 증상 | 해결 |
|---|---|---|
| **God Agent** | 하나의 agent가 10+ skill 순차 호출 | Sub-agent로 분리, Process ≤ 5단계 |
| **Long Method** | Process 섹션이 10단계 초과 | 단계를 sub-agent로 추출 |
| **Leaky Abstraction** | MCP/하위 agent 내부 구현에 의존 | 입출력 계약만 사용 |
| **비표준 프런트매터** | `allowed-tools`, `user-invocable` 등 혼용 | `name/description/tools/model`만 사용 |
| **오버엔지니어링 제안** | 소규모 프로젝트에 Kafka·Redis Cluster 권장 | "Further Consideration" 섹션으로 분리 |

---

## 자기 진단 체크리스트

새 agent를 추가하거나 기존 agent를 수정할 때:

- [ ] 프런트매터가 `name / description / tools / model` 4필드만 쓰는가?
- [ ] 6섹션(Scope/Responsibilities/Process/Output Format/Success Criteria/Red Flags)을 모두 갖추는가?
- [ ] Process 단계가 10개 이하인가?
- [ ] Success Criteria가 측정 가능한 형태(체크박스, 숫자)인가?
- [ ] Red Flags에 "대신 쓰는 agent"가 명시되어 있는가?
- [ ] 총 분량이 200줄 이하인가? (참조 내용은 `references/`로 분리)

---

## 참고

- [Agents 공식 문서](https://docs.anthropic.com/en/docs/claude-code/sub-agents)
- 하네스 설계 원리: `.claude/guides/harness-guideline.md` §3
- Skills 가이드: `.claude/guides/skills-guide.md`
