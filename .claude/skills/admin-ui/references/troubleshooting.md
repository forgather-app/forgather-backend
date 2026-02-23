# Admin UI Troubleshooting

## CSS 변수 미사용 경고

**Cause:** 하드코딩된 색상값 사용 (예: `color: #333;`)
**Solution:** `common.css`의 CSS 변수로 교체

```css
/* Bad */
color: #333;
background: #f5f5f5;

/* Good */
color: var(--text-primary);
background: var(--bg-secondary);
```

## escapeHtml() 미사용 경고

**Cause:** `innerHTML`에 사용자 입력 직접 삽입
**Solution:** 각 페이지 JS 파일 내에 `escapeHtml()` 함수를 정의하고, 사용자 입력을 감싸서 XSS 방지
- 참고: `hosts.js`, `spaces.js` 등 기존 페이지에 동일 패턴으로 정의되어 있음

```javascript
// 각 페이지 JS 파일 내에 아래 함수 정의 필요
function escapeHtml(text) {
    const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
    return text.replace(/[&<>"']/g, m => map[m]);
}

// Bad
element.innerHTML = `<span>${userData.name}</span>`;

// Good
element.innerHTML = `<span>${escapeHtml(userData.name)}</span>`;
```

## 스크립트 로드 순서 오류

**Cause:** `auth.js`보다 `page.js`가 먼저 로드됨
**Solution:** 아래 순서를 반드시 준수

```html
<!-- 올바른 순서 -->
<script th:src="@{/js/admin/auth.js}"></script>
<script th:src="@{/js/admin/api.js}"></script>
<script th:src="@{/js/admin/pagination.js}"></script>
<script th:src="@{/js/admin/{page}.js}"></script>
```

## API 호출 실패 (401 Unauthorized)

**Cause:** JWT 토큰 만료 또는 미설정
**Solution:** `Auth` 전역 객체의 토큰 갱신 로직 확인

```javascript
// API 호출 시 Auth 객체가 자동으로 토큰 헤더 추가
API.get('/hosts');  // BASE_URL('/admin') + endpoint → '/admin/hosts'로 요청
```

## 페이지네이션 동작 안 함

**Cause:** `PaginationUtil.render()` 호출 시 콜백 미전달
**Solution:** 콜백 함수를 반드시 전달

```javascript
const paginationContainer = document.getElementById('pagination');
PaginationUtil.render(
    paginationContainer,           // container: HTMLElement
    data.currentPage,              // currentPage (0-based)
    data.totalPages,               // totalPages
    data.totalCount,               // totalCount
    (page) => loadData(page)       // onPageClick 콜백 (필수)
);
```
