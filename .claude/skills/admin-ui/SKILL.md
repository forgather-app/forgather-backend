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

## When to Activate

다음 요청이나 키워드가 나타날 때 활성화:

**명시 호출**
```
/admin-ui list {Entity}      # 목록 페이지
/admin-ui detail {Entity}    # 상세 모달
/admin-ui form {Entity}      # 생성/수정 폼
/admin-ui crud {Entity}      # 위 세 가지 모두
```

**자동 트리거**
- "어드민 페이지 만들어줘", "admin UI 생성"
- "관리자 화면 추가", "백오피스 페이지"
- Thymeleaf admin template, list/detail/form/crud 생성 언급
- `templates/admin/**/*.html` 파일 작성 컨텍스트

**새 페이지 생성 직전에는 `analyze-admin` agent가 먼저 호출되어 CSS 변수·JS 객체·Fragment 목록을 수집**할 수 있다.

## Core Concepts

### 생성 가능한 타입

| Type | 생성 파일 |
|------|----------|
| `list` | `templates/admin/{entity}/list.html` + `static/css/admin/{entity}.css` + `static/js/admin/{entity}.js` |
| `detail` | 기존 list.html에 상세 모달 추가 |
| `form` | `templates/admin/{entity}/form.html` 또는 모달 |
| `crud` | 위 세 가지 모두 |

### 공통 빌딩 블록
- **레이아웃**: `templates/admin/layout.html` — 공통 navbar/footer
- **Fragment**: `templates/admin/fragments/` — 재사용 조각 (navbar, pagination, search)
- **CSS 변수**: `static/css/admin/common.css` — 색상·간격·그림자·보더 반경
- **전역 JS 객체**: `API`, `Auth`, `PaginationUtil` — HTTP 요청·인증·페이지네이션

## Code Examples

### CSS — 변수 우선 사용

```css
/* ✅ Good — 변수 사용 */
.page-header {
    color: var(--primary-color);
    padding: var(--spacing-md);
    box-shadow: var(--shadow-sm);
}

/* ❌ Bad — 하드코딩 */
.page-header {
    color: #4A90D9;
    padding: 16px;
}
```

### JavaScript — 전역 객체 + 이벤트 위임

```javascript
// ✅ Good — 전역 API 객체 사용
const spaces = await API.get('/admin/spaces');

// ✅ Good — 이벤트 위임 + escapeHtml
document.querySelector('.data-table').addEventListener('click', (e) => {
    if (!e.target.matches('.row-action')) return;
    const id = e.target.dataset.id;
    openDetail(escapeHtml(id));
});
```

### HTML — 스크립트 순서

```html
<!-- ✅ 의존성 순서: auth → api → pagination → 페이지 스크립트 -->
<script src="/js/admin/auth.js"></script>
<script src="/js/admin/api.js"></script>
<script src="/js/admin/pagination.js"></script>
<script src="/js/admin/space.js"></script>
```

### 생성 프로세스

1. **0단계**: `analyze-admin` 결과 확인 (또는 `templates/admin/`, `common.css`, `api.js` 읽어 패턴 파악)
2. `.claude/guides/admin-ui-guide.md` 컨벤션 확인
3. 대상 Entity 필드·연관관계 분석
4. `assets/` 템플릿 참조하여 HTML/CSS/JS 생성
5. `scripts/convention-check.sh` 실행하여 컨벤션 검증

## Best Practices

- **CSS 변수 우선**: 색상·간격·그림자는 `var(--*)` 사용, 하드코딩 금지
- **클래스 네이밍**: `.page-header`, `.filter-section`, `.data-table`, `.modal-*` 사용
- **Fragment 재사용**: 공통 UI는 `th:fragment`/`th:replace`로 반복 제거
- **시맨틱 HTML5**: `<header>`, `<main>`, `<nav>` + 접근성 속성 (`aria-label`, `role`)
- **XSS 방지**: 사용자 입력 렌더 전 `escapeHtml()` 통과 필수
- **파일명 일관성**: `list.html` / `form.html` / `detail.html`, `{entity}.css`, `{entity}.js`

## Checklist

생성 후 확인:

- [ ] `convention-check.sh` 통과했는가? (하드코딩·네이밍·스크립트 순서 검증)
- [ ] CSS에 `var(--*)` 사용, 하드코딩 색상·간격 없음
- [ ] JS가 `API` / `Auth` / `PaginationUtil` 전역 객체를 사용 (fetch 직접 호출 금지)
- [ ] HTML 스크립트 순서가 auth → api → pagination → 페이지 순
- [ ] 공통 레이아웃(`admin/layout.html`) + 필요한 Fragment 재사용
- [ ] 파일 네이밍 규칙(`{entity}.css` / `{entity}.js`) 준수
- [ ] 사용자 입력 HTML에 `escapeHtml()` 적용
- [ ] Admin Controller에 `@LoginAdminUser` + 세션 인증 인터셉터 적용

## Red Flags

| 안티패턴 | 해결 |
|---|---|
| CSS 변수 무시하고 색상·간격 하드코딩 | `common.css`의 `var(--*)`로 교체 |
| Fragment 대신 공통 마크업 복붙 | `th:fragment` + `th:replace` |
| 페이지 JS가 `fetch()` 직접 호출 | `API.get/post/put/delete` 전역 객체 경유 |
| `escapeHtml()` 없이 사용자 입력 렌더 | 렌더 전 전부 escape |
| HTML 스크립트 순서 역전 (페이지 스크립트 먼저) | `auth → api → pagination → page` 순서 |
| Admin Controller에 `@LoginAdminUser` 누락 | 인증 리졸버 적용 + 세션 검증 |

## Examples

**Example 1: 호스트 관리 목록 페이지**

> User: "호스트 관리 어드민 페이지 만들어줘"

1. Host 엔티티 필드·연관관계 분석
2. `assets/list.html.template` 참조 → `list.html` 생성
3. `assets/page.css.template` 참조 → `host.css` 생성
4. `assets/page.js.template` 참조 → `host.js` 생성
5. `scripts/convention-check.sh` 실행 → 컨벤션 검증

→ `/admin/hosts` 목록 페이지 완성

**Example 2: 기존 페이지에 상세 모달 추가**

> User: "스페이스 목록에서 상세보기 모달 추가해줘"

1. 기존 `space/list.html` 분석
2. `assets/modal.html.template` 참조 → 모달 HTML 추가
3. `assets/modal.js.template` 참조 → 모달 JS 추가
4. 기존 CSS에 모달 스타일 추가

→ 행 클릭 시 상세 모달 표시

## 상세 참조

- `.claude/guides/admin-ui-guide.md` — 어드민 UI 컨벤션 전문
- `references/troubleshooting.md` — 자주 발생하는 문제와 해결법
- `scripts/convention-check.sh` — 생성 후 컨벤션 자동 검증

## 템플릿

- `assets/list.html.template` — 목록 페이지
- `assets/page.css.template` — 페이지별 CSS
- `assets/page.js.template` — 페이지별 JS
- `assets/modal.html.template` — 모달 HTML
- `assets/modal.js.template` — 모달 JS
