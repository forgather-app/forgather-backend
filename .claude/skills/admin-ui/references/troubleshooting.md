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
**Solution:** `escapeHtml()`로 감싸서 XSS 방지

```javascript
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
API.get('/api/admin/hosts');  // Auth.getToken() 자동 포함
```

## 페이지네이션 동작 안 함

**Cause:** `PaginationUtil.render()` 호출 시 콜백 미전달
**Solution:** 콜백 함수를 반드시 전달

```javascript
PaginationUtil.render({
    currentPage: data.currentPage,
    totalPages: data.totalPages,
    onPageChange: (page) => loadData(page)  // 콜백 필수
});
```
