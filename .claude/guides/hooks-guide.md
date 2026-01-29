# Claude Code Hooks 가이드

Claude Code의 특정 이벤트 발생 시 자동으로 실행되는 쉘 스크립트.

## 훅 이벤트 종류

| 이벤트 | 발생 시점 |
|--------|----------|
| `UserPromptSubmit` | 사용자가 프롬프트 제출 시 |
| `Stop` | Claude 응답 완료 시 |
| `PreToolUse` | 도구 사용 전 |
| `PostToolUse` | 도구 사용 후 |

## 설정 파일

| 파일 | 용도 | Git |
|-----|------|-----|
| `.claude/settings.json` | 팀 공유 설정 | ✅ 커밋 |
| `.claude/settings.local.json` | 개인 설정 | ❌ 제외 |

**우선순위**: `settings.local.json` > `settings.json` > 전역 설정

## Doubt Mode (`!rv`)

Claude 응답을 의심하고 재검증을 요청하는 기능.

**사용법**: 프롬프트에 `!rv` 포함
```
이 코드의 버그를 찾아줘 !rv
```

**동작**: `!rv` 감지 → Claude 응답 → 차단 + 재검증 요청

**스크립트 위치**:
```
.claude/hooks/scripts/
├── doubt-detector.sh     # !rv 감지
└── doubt-validator.sh    # 재검증 요청
```

## 참고

- [Claude Code Hooks 문서](https://docs.anthropic.com/en/docs/claude-code/hooks)
