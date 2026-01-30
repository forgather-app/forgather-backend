# Task: analyze-admin Agent 생성

## 목표
`.claude/agents/analyze-admin.md` 파일을 생성합니다.

## 실행 단계

### 1. agents 디렉토리 생성
```bash
mkdir -p .claude/agents
```

### 2. 아래 내용을 `.claude/agents/analyze-admin.md`에 그대로 작성

```markdown
---
name: analyze-admin
description: Admin UI 패턴 분석. 새 어드민 페이지 생성 전, 기존 패턴 파악이 필요할 때 자동 호출.
tools: Read, Grep, Glob
model: haiku
---

# Admin UI 패턴 분석 Agent

프로젝트의 기존 어드민 UI 패턴을 분석하여 컨벤션 문서를 생성합니다.

## 분석 프로세스

### 1단계: CSS 변수 추출

**대상**: `src/main/resources/static/css/admin/common.css`

추출 항목:
- 색상 변수 (`--primary-color`, `--text-color` 등)
- 간격 변수 (`--spacing-*`)
- 그림자, 보더 반경 등

### 2단계: 전역 JS 객체 분석

**대상 파일**:
- `src/main/resources/static/js/admin/api.js` → API 객체
- `src/main/resources/static/js/admin/auth.js` → Auth 객체
- `src/main/resources/static/js/admin/pagination.js` → PaginationUtil 객체

**추출 항목**:
- 객체명, 메서드 시그니처, 용도

### 3단계: Thymeleaf Fragment 분석

**탐색 경로**:
- `src/main/resources/templates/admin/layout.html` - 공통 레이아웃
- `src/main/resources/templates/admin/fragments/` - 재사용 fragment
- `src/main/resources/templates/admin/{entity}/` - 도메인별 템플릿

**추출 항목**:
- Fragment 이름 (`th:fragment`)
- 파라미터
- 사용 위치

### 4단계: 파일 네이밍 규칙 도출

기존 파일명 패턴 분석:
- HTML: `list.html`, `form.html`, `detail.html`
- CSS: `{entity}.css`
- JS: `{entity}.js`

## 출력 형식

분석 완료 후 아래 형식으로 결과를 반환합니다:

```markdown
# Admin UI 패턴 분석 결과

**분석 일시**: {timestamp}
**분석 범위**: templates/admin/, static/css/admin/, static/js/admin/

---

## 1. CSS Variables

| Variable | Value | Category | Usage Example |
|----------|-------|----------|---------------|
| `--primary-color` | #4A90D9 | Color | 버튼, 링크 |
| `--spacing-md` | 16px | Spacing | 카드 패딩 |

---

## 2. Global JS Objects

### API
| Method | Parameters | Return | Description |
|--------|------------|--------|-------------|
| `get(url)` | string | Promise | GET 요청 |
| `post(url, data)` | string, object | Promise | POST 요청 |

### Auth
| Method | Parameters | Return | Description |
|--------|------------|--------|-------------|

### PaginationUtil
| Method | Parameters | Return | Description |
|--------|------------|--------|-------------|

---

## 3. Thymeleaf Fragments

| Fragment | Location | Parameters | Usage |
|----------|----------|------------|-------|
| `navbar` | `fragments/navbar.html` | none | 모든 페이지 상단 |
| `pagination` | `fragments/pagination.html` | `page`, `totalPages` | 목록 페이지 하단 |

---

## 4. File Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| Controller | `Admin{Entity}Controller.java` | `AdminSpaceController.java` |
| Template (list) | `templates/admin/{entity}/list.html` | `templates/admin/space/list.html` |
| CSS | `static/css/admin/{entity}.css` | `static/css/admin/space.css` |
| JS | `static/js/admin/{entity}.js` | `static/js/admin/space.js` |

---

## 5. 권장사항

- 새 페이지 생성 시 위 패턴 준수
- CSS 변수 우선 사용 (하드코딩 금지)
- 공통 Fragment 재사용
```

## 주의사항

- 이 Agent는 **읽기 전용**입니다. 파일 수정/생성을 하지 않습니다.
- 분석 결과는 메인 컨텍스트로 요약하여 반환합니다.
- 대량의 파일 내용을 그대로 반환하지 말고, 패턴을 추출하여 정리합니다.
```

### 3. 기존 skills/analyze-admin 삭제 (존재하는 경우)
```bash
rm -rf .claude/skills/analyze-admin
```

## 완료 조건
- `.claude/agents/analyze-admin.md` 파일이 생성됨
- 파일 내용이 위 명세와 일치함
- 기존 skills/analyze-admin 디렉토리가 없음

## 검증
```bash
cat .claude/agents/analyze-admin.md
```
