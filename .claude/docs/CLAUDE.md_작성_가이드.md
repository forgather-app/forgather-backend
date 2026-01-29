# CLAUDE.md 작성 완전 가이드

> Claude Code에게 좋은 CLAUDE.md 파일을 만들게 하기 위한 참고 자료
>
> **출처**: HumanLayer 블로그, Anthropic 공식 블로그, Claude Code 공식 문서

---

## 📋 TL;DR (핵심 요약)

| 원칙 | 설명 |
|------|------|
| **Less is More** | 지시사항은 최소화. 300줄 미만, 가능하면 60줄 이하 |
| **WHAT-WHY-HOW** | 구조, 목적, 작업 방법을 명확히 정의 |
| **보편적 적용** | 모든 세션에 적용되는 정보만 포함 |
| **Progressive Disclosure** | 세부 지침은 별도 파일로 분리 |
| **자동 생성 금지** | `/init` 결과물은 시작점일 뿐, 반드시 수동 검토 |
| **린터 역할 금지** | 코드 스타일은 린터에 맡기고, 훅으로 자동화 |

---

## 1️⃣ CLAUDE.md의 본질

### LLM의 무상태성 이해

```
핵심 사실:
- LLM은 세션 간 학습/기억을 유지하지 않음
- 매 세션 시작 시 코드베이스에 대해 아무것도 모름
- CLAUDE.md는 모든 대화에 자동 포함되는 유일한 파일
```

**Claude Code 내부 동작**:
```
<system-reminder>
  IMPORTANT: this context may or may not be relevant to your tasks.
  You should not respond to this context unless it is highly relevant to your task.
</system-reminder>
```

⚠️ **중요**: Claude는 관련성이 낮다고 판단하면 CLAUDE.md 내용을 **무시**할 수 있음

### CLAUDE.md의 역할

| 역할 | 설명 |
|------|------|
| **온보딩 문서** | 새로운 팀원(Claude)에게 프로젝트를 소개 |
| **시스템 프롬프트 확장** | Claude의 기본 동작을 프로젝트에 맞게 커스터마이즈 |
| **컨텍스트 주입** | 매 세션마다 자동으로 핵심 정보 제공 |

---

## 2️⃣ 좋은 CLAUDE.md의 구조: WHAT-WHY-HOW

### WHAT (무엇인가)
프로젝트의 **구조**와 **기술 스택**을 설명

```markdown
## 프로젝트 개요
- 기술 스택: Java 21, Spring Boot 3.3, MySQL 8.0
- 아키텍처: 모놀리식, 레이어드 아키텍처
- 주요 모듈: API 서버, 배치 처리, 관리자 페이지

## 디렉토리 구조
src/main/java/com/forgather/
├── api/          # REST 컨트롤러
├── application/  # 서비스 계층
├── domain/       # 엔티티, 리포지토리
└── infrastructure/  # 외부 연동
```

### WHY (왜 그런가)
각 구성 요소의 **목적**과 **설계 의도** 설명

```markdown
## 설계 결정 사항
- Presigned URL 사용 이유: 서버 부하 감소, 대용량 파일 직접 업로드
- Soft Delete 적용: 데이터 복구 가능성 확보, 히스토리 추적
- 비동기 처리: 이미지 리사이징은 @Async로 처리하여 응답 속도 개선
```

### HOW (어떻게 작업하는가)
**빌드, 테스트, 배포** 등 실제 작업 방법 명시

```markdown
## 명령어
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 로컬 실행: `./gradlew bootRun`
- 린트: `./gradlew spotlessApply`

## 작업 검증
- 코드 변경 후 반드시 `./gradlew test` 실행
- PR 전 `./gradlew spotlessCheck` 통과 확인
```

---

## 3️⃣ 작성 원칙 (공식 가이드 + 커뮤니티 베스트 프랙티스)

### 원칙 1: Less (Instructions) is More

**연구 결과** (HumanLayer 인용):
- Frontier LLM은 약 **150~200개의 지시사항**을 안정적으로 따를 수 있음
- Claude Code 시스템 프롬프트에 이미 **~50개 지시사항** 포함
- 지시 수 증가 → **전체 지시 수행 품질 균등 저하**

```
❌ 나쁜 예: 모든 가능한 명령어와 규칙을 나열
✅ 좋은 예: 보편적이고 필수적인 지시만 최소한으로 포함
```

**권장 길이**:
| 출처 | 권장 |
|------|------|
| 일반 합의 | 300줄 미만 |
| HumanLayer | 60줄 미만 |
| Anthropic | "concise and human-readable" |

### 원칙 2: 보편적 적용성

CLAUDE.md는 **모든 세션**에 포함되므로:

```
❌ 포함하지 말 것:
- 특정 작업에만 해당하는 지침 (예: "새 DB 스키마 구조화 방법")
- 일회성 태스크 관련 내용
- 특정 기능에만 적용되는 규칙

✅ 포함할 것:
- 모든 작업에 적용되는 빌드/테스트 명령어
- 프로젝트 전반의 아키텍처 개요
- 보편적인 코딩 컨벤션
```

### 원칙 3: Progressive Disclosure (점진적 공개)

**구조**:
```
project/
├── CLAUDE.md                 # 핵심 정보만 (목차 + 간략 설명)
└── agent_docs/               # 세부 지침 분리
    ├── building.md
    ├── testing.md
    ├── code_conventions.md
    ├── database_schema.md
    └── deployment.md
```

**CLAUDE.md 예시**:
```markdown
## 상세 문서
필요시 아래 문서를 참조하세요:
- agent_docs/building.md - 빌드 및 배포 가이드
- agent_docs/testing.md - 테스트 전략 및 실행 방법
- agent_docs/code_conventions.md - 코딩 컨벤션
- agent_docs/database_schema.md - DB 스키마 설명

작업 전 관련 문서를 먼저 읽고 진행하세요.
```

**참고**: 파일 참조는 자동 로드가 아닙니다. Claude가 필요할 때 해당 경로의 파일을 직접 읽도록 안내하는 포인터 역할만 합니다.

**포인터 vs 복사**:
```
❌ 코드 스니펫을 직접 포함 (금방 outdated됨)
✅ file:line 참조 사용 (항상 최신 유지)

예: "인증 로직은 AuthService.java:45-78 참조"
```

### 원칙 4: Claude는 린터가 아니다

**왜 코드 스타일을 CLAUDE.md에 넣으면 안 되는가**:
- LLM은 **비용이 높고, 느리고, 비결정적**
- 린터는 **빠르고, 저렴하고, 결정적**
- Claude는 **기존 코드 패턴에서 자연스럽게 학습**함

**권장 접근법**:
```markdown
## 코드 스타일
- 포맷팅은 `./gradlew spotlessApply`로 자동 수정
- 코드 작성 후 반드시 위 명령어 실행

(구체적인 스타일 규칙은 .editorconfig 및 spotless 설정 참조)
```

**자동화 (Hooks 활용)**:
```json
// .claude/settings.json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Write(*.java)",
        "hooks": [
          {
            "type": "command",
            "command": "./gradlew spotlessApply"
          }
        ]
      }
    ]
  }
}
```

### 원칙 5: 자동 생성 금지 (신중한 수작업)

**HumanLayer의 레버리지 피라미드**:
```
CLAUDE.md (가장 높은 레버리지)
    ↓ 영향
리서치 문서
    ↓ 영향
구현 계획
    ↓ 영향
코드 (가장 낮은 레버리지)
```

```
❌ /init 결과를 그대로 사용
✅ /init은 시작점으로만 사용, 모든 문장을 신중히 검토
```

**이유**:
- CLAUDE.md의 한 줄이 **모든 세션, 모든 산출물**에 영향
- 잘못된 지시 → 계획 오류 → 대량의 나쁜 코드

---

## 4️⃣ Anthropic 공식 권장 사항

### 공식 블로그 권장 내용

**출처**: https://www.anthropic.com/engineering/claude-code-best-practices

```markdown
## CLAUDE.md에 포함할 내용
- Common bash commands (자주 쓰는 명령어)
- Core files and utility functions (핵심 파일)
- Code style guidelines (간략한 스타일 가이드)
- Testing instructions (테스트 방법)
- Repository etiquette (브랜치 명명, merge vs rebase)
- Developer environment setup (환경 설정)
- Project-specific warnings (주의사항)
```

**공식 예시**:
```markdown
# Bash commands
- npm run build: Build the project
- npm run typecheck: Run the typechecker

# Code style
- Use ES modules (import/export) syntax, not CommonJS (require)
- Destructure imports when possible (eg. import { foo } from 'bar')

# Workflow
- Be sure to typecheck when you're done making a series of code changes
- Prefer running single tests, and not the whole test suite, for performance
```

### 공식 문서 권장 사항

**출처**: https://claude.com/blog/using-claude-md-files

**구조 권장**:
```markdown
# Project Context
프로젝트 설명 및 Claude가 알아야 할 행동 지침

## About This Project
기술 스택, 아키텍처 설명

## Key Directories
주요 디렉토리 설명

## Standards
코딩 표준 (간략히)

## Common Commands
자주 사용하는 명령어

## Notes
기타 중요 정보
```

**팁**:
1. `/init` 사용 후 반드시 검토 및 수정
2. `#` 키로 세션 중 CLAUDE.md에 내용 추가 가능
3. 팀과 공유하려면 git에 체크인
4. 개인용은 `CLAUDE.local.md`로 분리 (.gitignore)

---

## 5️⃣ 안티패턴 (피해야 할 것들)

### ❌ 지시사항 과잉
```markdown
# 나쁜 예
- 항상 함수명은 동사로 시작하세요
- 변수명은 camelCase를 사용하세요
- 한 줄은 80자를 넘기지 마세요
- import는 알파벳 순으로 정렬하세요
- 주석은 영어로 작성하세요
- ...50개 더
```

### ❌ 특정 작업 전용 지시
```markdown
# 나쁜 예
## 새 API 엔드포인트 추가 시
1. controller 패키지에 파일 생성
2. @RestController 어노테이션 추가
3. ...

(이런 내용은 별도 문서로 분리)
```

### ❌ 코드 스니펫 직접 포함
```markdown
# 나쁜 예
## 표준 응답 포맷
```java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    // ... 20줄
}
```

(대신 "ApiResponse.java 참조"로 대체)
```

### ❌ 관련 없는 정보
```markdown
# 나쁜 예
- 나를 "Mr. Tinkleberry"라고 불러줘
- 항상 이모지를 사용해서 대답해줘
- 한국어로만 대답해줘

(이런 개인 선호는 무시될 가능성 높음)
```

---

## 6️⃣ 실전 템플릿

### 기본 템플릿 (60줄 이하)

```markdown
# [프로젝트명]

## 개요
[한 문장 설명]

## 기술 스택
- Backend: [기술]
- Database: [기술]
- Infrastructure: [기술]

## 프로젝트 구조
```
src/
├── [디렉토리]/ - [설명]
└── [디렉토리]/ - [설명]
```

## 명령어
- 빌드: `[명령어]`
- 테스트: `[명령어]`
- 린트: `[명령어]`

## 핵심 규칙
- [규칙 1]
- [규칙 2]
- [규칙 3]

## 주의사항
- [주의 1]
- [주의 2]

## 상세 문서
- @[경로] - [설명]
```

### Spring Boot 프로젝트 예시

```markdown
# Forgather - 이벤트 사진 공유 서비스

## 개요
결혼식, 졸업전시회 등 이벤트의 사진을 QR코드로 쉽게 공유하는 서비스

## 기술 스택
- Backend: Java 21, Spring Boot 3.3, Spring Data JPA
- Database: MySQL 8.0
- Infrastructure: AWS (EC2, S3, RDS, CloudFront)
- Monitoring: Grafana, Prometheus

## 프로젝트 구조
```
src/main/java/com/forgather/
├── api/           # REST 컨트롤러, DTO
├── application/   # 서비스, 유즈케이스
├── domain/        # 엔티티, 리포지토리
└── infrastructure/ # 외부 연동 (S3, 알림 등)
```

## 명령어
- 빌드: `./gradlew build`
- 테스트: `./gradlew test`
- 단일 테스트: `./gradlew test --tests "TestClassName"`
- 린트: `./gradlew spotlessApply`
- 로컬 실행: `./gradlew bootRun`

## 핵심 규칙
- 서비스 계층에 @Transactional(readOnly=true) 기본 적용
- 엔티티 직접 반환 금지, 항상 DTO 변환
- N+1 쿼리 주의: fetch join 또는 @EntityGraph 사용

## 주의사항
- .env 파일 절대 커밋 금지
- S3 파일 삭제는 soft delete 우선 적용
- 테스트에서 실제 외부 API 호출 금지 (Mock 사용)

## 상세 문서
- @docs/api-spec.md - API 명세
- @docs/database-schema.md - DB 스키마
- @docs/deployment.md - 배포 가이드
```

---

## 7️⃣ 커뮤니티 의견 요약 (Hacker News)

| 의견 | 설명 |
|------|------|
| **Table-of-Contents 접근법** | CLAUDE.md에는 목차만, 세부 내용은 별도 파일로 |
| **context 과잉 주의** | 너무 많은 정보는 오히려 품질 저하 |
| **정보 밀도 중요** | attention은 유한함, 핵심만 집중 |
| **README와의 차이** | CLAUDE.md는 "모델의 프롬프트 커스터마이즈" 역할 |
| **재온보딩 비용 절감** | 인간은 기억하지만 Claude는 매번 잊음 |

---

## 📚 참고 자료

| 자료 | URL |
|------|-----|
| **HumanLayer 블로그** (원문) | https://www.humanlayer.dev/blog/writing-a-good-claude-md |
| **GeekNews 요약** | https://news.hada.io/topic?id=24744 |
| **Anthropic 공식 블로그** | https://claude.com/blog/using-claude-md-files |
| **Anthropic Engineering** | https://www.anthropic.com/engineering/claude-code-best-practices |
| **Claude Code 공식 문서** | https://code.claude.com/docs/en/memory |
| **Claude Code Settings** | https://code.claude.com/docs/en/settings |
| **Claude Code Skills** | https://code.claude.com/docs/en/skills |
| **Claude Code Hooks** | https://code.claude.com/docs/en/hooks |

---

## ✅ 체크리스트: CLAUDE.md 작성 시

- [ ] 300줄 미만인가? (이상적으로 60줄 이하)
- [ ] WHAT, WHY, HOW가 명확한가?
- [ ] 모든 세션에 보편적으로 적용되는 내용인가?
- [ ] 특정 작업 전용 지시가 섞여 있지 않은가?
- [ ] 코드 스니펫 대신 파일 참조를 사용했는가?
- [ ] 코드 스타일 규칙은 린터에 위임했는가?
- [ ] 세부 지침은 별도 파일로 분리했는가?
- [ ] /init 결과를 그대로 사용하지 않고 검토했는가?
- [ ] 민감 정보(API 키, 크레덴셜)가 없는가?

---

## 8️⃣ Skills (슬래시 명령어 확장)

### Commands vs Skills

| 항목 | Commands (레거시) | Skills (권장) |
|-----|------------------|---------------|
| 위치 | `.claude/commands/name.md` | `.claude/skills/name/SKILL.md` |
| 추가 파일 | 불가 | 가능 (templates, scripts, examples) |
| 호출 제어 | 불가 | `disable-model-invocation`, `user-invocable` |
| 서브에이전트 | 불가 | `context: fork` |
| 동작 | 동일 (둘 다 `/name` 으로 호출) | 동일 |

**같은 이름이면 skill이 command보다 우선합니다.**

### Skills 구조

```
.claude/skills/my-skill/
├── SKILL.md           # 메인 지침 (필수)
├── templates/         # Claude가 채울 템플릿
├── examples/          # 예시 출력물
└── scripts/           # Claude가 실행할 스크립트
```

### SKILL.md Frontmatter

```yaml
---
name: my-skill                    # 스킬명 (생략 시 디렉토리명)
description: "스킬 설명"           # Claude가 자동 호출 판단에 사용 (권장)
argument-hint: "[filename]"       # 자동완성 시 힌트
allowed-tools: Read, Grep, Glob   # 스킬 활성화 시 허용 도구
disable-model-invocation: true    # true면 사용자만 호출 가능
user-invocable: false             # false면 Claude만 호출 가능
context: fork                     # 서브에이전트에서 실행
agent: Explore                    # context: fork일 때 에이전트 타입
---

스킬 지침 내용...
```

### 변수 치환

| 변수 | 설명 |
|-----|------|
| `$ARGUMENTS` | 호출 시 전달된 모든 인자 |
| `$ARGUMENTS[N]` 또는 `$N` | N번째 인자 (0-based) |
| `${CLAUDE_SESSION_ID}` | 현재 세션 ID |

### 동적 컨텍스트

`` !`command` `` 문법으로 쉘 명령어 실행 결과를 주입:

```yaml
## PR 컨텍스트
- PR diff: !`gh pr diff`
- 변경 파일: !`gh pr diff --name-only`
```

### 호출 제어

| 설정 | 사용자 호출 | Claude 호출 |
|-----|-----------|------------|
| (기본값) | O | O |
| `disable-model-invocation: true` | O | X |
| `user-invocable: false` | X | O |

---

*작성일: 2026-01-28*
*업데이트: 2026-01-29 (Skills 섹션 추가)*
*참고: 이 문서는 Claude Code에게 CLAUDE.md 생성을 요청할 때 참고 자료로 제공하기 위해 작성됨*
