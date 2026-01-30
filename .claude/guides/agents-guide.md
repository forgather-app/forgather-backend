# Agents 활용 가이드

## Agents란?

Claude Code의 서브 에이전트로, 특정 작업에 특화된 분석/처리를 수행합니다. Skills와 달리 **읽기 전용 분석**에 특화되어 있으며, Claude가 작업 컨텍스트에 따라 자동으로 호출합니다.

## Skills vs Agents

| 구분 | Skills | Agents |
|-----|--------|--------|
| **용도** | 코드 생성/수정 | 분석/조사 |
| **호출** | `/skill-name` 명시적 호출 | 자동 호출 또는 명시적 요청 |
| **도구** | Read, Write, Edit 등 | 주로 Read, Grep, Glob |
| **예시** | `/admin-ui`, `/test-writer` | `jpa-analyzer`, `analyze-admin` |

## 호출 방법

| 방법 | 예시 |
|-----|------|
| **자동** | "N+1 문제 있는지 확인해줘" → `jpa-analyzer` 자동 호출 |
| **명시적** | "jpa-analyzer로 ProductService 분석해줘" |

## 프로젝트 Agents

### `jpa-analyzer`
JPA 코드 분석 및 N+1 문제 감지

**자동 호출 트리거**:
- "N+1 문제 확인", "쿼리 최적화", "JPA 분석" 등

**분석 범위**:
```
jpa-analyzer로 전체 분석해줘           # 프로젝트 전체
jpa-analyzer로 product 도메인 분석해줘  # 특정 도메인
jpa-analyzer로 SpaceService 분석해줘   # 특정 서비스
```

**출력**: 심각도별 N+1 문제 목록 + 해결책 제시
- 🔴 HIGH: 실제 N+1 발생 (루프 내 Repository 호출)
- 🟡 MEDIUM: 잠재적 N+1 (중첩 서비스 호출, Lazy 컬렉션)
- 🟢 LOW: 최적화 기회 (Fetch Join, DTO 프로젝션)

### `analyze-admin`
어드민 UI 패턴 분석

**자동 호출 트리거**:
- 새 어드민 페이지 생성 요청 시 사전 분석
- "어드민 패턴 분석", "CSS 변수 확인" 등

**분석 항목**:
- CSS 변수 (`common.css`)
- 전역 JS 객체 (API, Auth, PaginationUtil)
- Thymeleaf Fragment 패턴
- 파일 네이밍 규칙

**출력**: 컨벤션 요약 문서 (CSS 변수 목록, JS 메서드, Fragment 사용법)

## Agent 파일 위치

```
.claude/agents/
├── jpa-analyzer.md      # JPA N+1 분석
└── analyze-admin.md     # Admin UI 패턴 분석
```

## 참고

- [Agents 공식 문서](https://docs.anthropic.com/en/docs/claude-code/sub-agents)
- Skills 활용법: `.claude/guides/skills-guide.md`
