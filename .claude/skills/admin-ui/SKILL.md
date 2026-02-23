---
name: admin-ui
description: 어드민 UI 코드 생성. Thymeleaf 템플릿, CSS, JS를 프로젝트 컨벤션에 맞춰 작성. Use when user says "어드민 페이지 만들어줘", "admin UI 생성", "관리자 화면 추가", "백오피스 페이지", or mentions Thymeleaf admin template, list/detail/form/crud page creation.
allowed-tools: Read, Grep, Glob, Write, Edit
user-invocable: true
context: fork
agent: general-purpose
metadata:
  author: Forgather
  version: 1.0.0
---

# Admin UI 생성 스킬

이 스킬은 어드민 UI 코드 생성 시 참조하는 전문 지식입니다.

## 이 스킬의 용도

- `/admin-ui` 커맨드 실행 시 참조
- 어드민 관련 코드 생성/수정 요청 시 컨텍스트로 활용

## 생성 가능한 타입

| Type | 생성 파일 |
|------|----------|
| list | `templates/admin/{entity}/list.html`, `static/css/admin/{entity}.css`, `static/js/admin/{entity}.js` |
| detail | 기존 list.html에 상세 모달 추가 |
| form | `templates/admin/{entity}/form.html` 또는 모달로 추가 |
| crud | 위 세 가지 모두 생성 |

## 생성 프로세스

### 0단계: 기존 어드민 패턴 분석

**반드시 먼저 수행** - 기존 코드베이스의 패턴 파악:

1. `templates/admin/` - Thymeleaf 템플릿 구조 분석
2. `static/css/admin/common.css` - CSS 변수 목록 파악
3. `static/js/admin/` - 전역 JS 객체 메서드 확인 (API, Auth, PaginationUtil)
4. 기존 페이지 하나 선택해서 전체 흐름 파악

**분석 결과 정리 (내부용):**
- CSS 변수: `--primary-color`, `--shadow-sm` 등
- JS 메서드: `API.get()`, `API.post()`, `Auth.logout()`, `PaginationUtil.render()`
- Fragment 패턴: `layout/admin-layout.html`

### 1~5단계: 실제 코드 생성

1. `.claude/guides/admin-ui-guide.md` 컨벤션 참조
2. `templates/admin/` 기존 템플릿 구조 파악
3. `static/css/admin/common.css` CSS 변수 확인
4. `static/js/admin/` 전역 객체 패턴 확인 (API, Auth, PaginationUtil)
5. 대상 Entity 필드/연관관계 분석
6. 이 스킬의 `assets/` 하위 템플릿 참조하여 코드 생성

## 템플릿 파일

- `assets/list.html.template` - 목록 페이지 기본 구조
- `assets/page.css.template` - 페이지별 CSS 스타일
- `assets/page.js.template` - 페이지별 JavaScript
- `assets/modal.html.template` - 모달 HTML 구조
- `assets/modal.js.template` - 모달 JavaScript

## Forgather 컨벤션 요약

### CSS
- 색상/간격/그림자는 CSS 변수 사용 (`var(--primary-color)`)
- 클래스: `.page-header`, `.filter-section`, `.data-table`, `.modal-*`

### JavaScript
- 전역 객체: `API.get()`, `API.post()`, `Auth.logout()`, `PaginationUtil.render()`
- 이벤트 위임 패턴, `escapeHtml()` 필수

### HTML
- 스크립트 순서: `auth.js` → `api.js` → `pagination.js` → `{page}.js`
- 시맨틱 HTML5 + 접근성 속성 (aria-label, role)

## Examples

### Example 1: 호스트 관리 목록 페이지
User says: "호스트 관리 어드민 페이지 만들어줘"
Actions:
1. Host 엔티티 필드/연관관계 분석
2. `assets/list.html.template` 참조하여 list.html 생성
3. `assets/page.css.template` 참조하여 host.css 생성
4. `assets/page.js.template` 참조하여 host.js 생성
5. `scripts/convention-check.sh` 실행하여 컨벤션 검증

Result: `/admin/hosts` 목록 페이지 완성

### Example 2: 기존 페이지에 상세 모달 추가
User says: "스페이스 목록에서 상세보기 모달 추가해줘"
Actions:
1. 기존 `space/list.html` 분석
2. `assets/modal.html.template` 참조하여 모달 HTML 추가
3. `assets/modal.js.template` 참조하여 모달 JS 로직 추가
4. 기존 CSS에 모달 스타일 추가

Result: 스페이스 목록에서 행 클릭 시 상세 모달 표시

## 상세 참조

- `references/troubleshooting.md` - 자주 발생하는 문제와 해결법
