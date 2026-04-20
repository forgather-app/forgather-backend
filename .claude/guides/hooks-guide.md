# Claude Code Hooks 가이드

Hook은 하네스의 **Feedback Layer**에 해당한다. 에이전트 산출물을 자동으로 검증하고, 위반을 구조적으로 차단한다.
상세 원리는 `.claude/guides/harness-guideline.md` §6 참조.

## Hook이란?

Claude Code의 특정 이벤트 발생 시 자동으로 실행되는 쉘 스크립트.

## 이벤트 종류

| 이벤트 | 시점 | 용도 | 권장 timeout |
|--------|------|------|-------------|
| `PreToolUse` | 도구 실행 **전** | 입력 검증, 위험 명령 차단 | 5~10 |
| `PostToolUse` | 도구 실행 **후** | 산출물 품질 검사 (lint, type check) | 30 |
| `Stop` | Claude 응답 완료 시 | 배치 포맷팅, 세션 저장, 사후 검증 | 5~10 |
| `SessionStart` | 세션 시작 시 | 컨텍스트 로드, 환경 감지 | 10 |
| `SessionEnd` | 세션 종료 시 | 상태 저장, 정리 | 5 |
| `UserPromptSubmit` | 사용자 프롬프트 제출 시 | 키워드 탐지, 메타 명령 처리 | 5 |

> `timeout` 단위는 **초(seconds)**. Bash 도구의 timeout(milliseconds)과 혼동 주의.

---

## 설정 파일

| 파일 | 용도 | Git |
|-----|------|-----|
| `.claude/settings.json` | 팀 공유 설정 | ✅ 커밋 |
| `.claude/settings.local.json` | 개인 설정 | ❌ 제외 |

**우선순위**: `settings.local.json` > `settings.json` > 전역 설정

### 구조 예시

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          { "type": "command", "command": "bash .claude/hooks/scripts/foo.sh", "timeout": 5 }
        ]
      }
    ]
  }
}
```

---

## 현재 프로젝트 활성 Hook

| 이벤트 | 스크립트 | 카테고리 | 역할 |
|---|---|---|---|
| `UserPromptSubmit` | `hooks/scripts/doubt-detector.sh` | 세션 관리 | 프롬프트의 `!rv` 키워드 감지 → Doubt Mode 활성화 |
| `Stop` | `hooks/scripts/doubt-validator.sh` | 품질 게이트 | Doubt Mode에서 Claude 응답 완료 시 재검증 유도 |

상태 파일 위치: `.claude/.hook-state/doubt-mode-{session_id}`

### Doubt Mode (`!rv`)

Claude 응답을 의심하고 재검증을 요청하는 기능.

```
이 코드의 버그를 찾아줘 !rv
```

동작 흐름: `!rv` 감지 → 상태 파일 생성 → Claude 응답 → Stop 훅이 재검증 요청 주입.

---

## 설계 원칙

### 성공은 조용히, 실패는 시끄럽게

검사가 통과하면 출력 없이 종료(`exit 0`), 실패할 때만 명확한 메시지. 노이즈를 줄이고 신호에 집중.

### standard vs strict 모드

| 모드 | 동작 | 사용 |
|---|---|---|
| **standard** | 경고만 출력, 진행 허용 | HIGH/MEDIUM 규칙 |
| **strict** | 위반 시 `exit 2`로 차단 | CRITICAL 규칙 (시크릿, 위험 명령) |

### Timeout 원칙

- **단위는 초(seconds)** — Bash 도구 timeout(ms)과 다르므로 주의
- 빠른 검사(키워드, 패턴 매칭): 5~10
- 무거운 검사(테스트 실행, 전체 lint): 30~300
- timeout 초과 시 hook은 무시되고 작업은 진행됨

---

## 카테고리별 설계 가이드

| 카테고리 | 이벤트 | 예시 |
|---|---|---|
| **품질 게이트** | PostToolUse | spotless, test 자동 실행 |
| **디자인 체크** | PostToolUse | 아키텍처 규칙 위반 감지 |
| **거버넌스** | PreToolUse | 시크릿 탐지, 위험 명령 차단 (`rm -rf`, `git push --force`) |
| **지속 학습** | PostToolUse | 패턴 추출, 관찰 기록 |
| **세션 관리** | SessionStart/End, UserPromptSubmit | 컨텍스트 로드/저장, 메타 명령 |

---

## 안티패턴

| 패턴 | 증상 | 해결 |
|---|---|---|
| **Chatty Hook** | 성공 시에도 stdout 출력 | 성공은 `exit 0`만, 출력 없음 |
| **Leaky Abstraction** | 특정 MCP/agent 내부 상태에 의존 | 도구의 입출력만 관찰 |
| **Silent Fail** | 실패를 `exit 0`으로 숨김 | strict 규칙은 `exit 2`로 차단 |
| **Missing Timeout** | settings.json에 timeout 미지정 | 모든 hook에 timeout 필수 |

---

## 참고

- [Claude Code Hooks 공식 문서](https://docs.anthropic.com/en/docs/claude-code/hooks)
- 하네스 설계 원리: `.claude/guides/harness-guideline.md` §6
- Agents/Skills 가이드와 연계: `.claude/guides/agents-guide.md`, `skills-guide.md`
