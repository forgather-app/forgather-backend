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

### `/analyze-admin`
어드민 UI 패턴 분석

```
/analyze-admin              # 전체 분석
/analyze-admin css          # CSS 패턴만
/analyze-admin js           # JavaScript 패턴만
```

## 참고

- [Skills 공식 문서](https://code.claude.com/docs/en/skills)
- Skill 작성법: `.claude/docs/CLAUDE.md_작성_가이드.md`
