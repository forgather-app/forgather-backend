# Forgather Admin UI Guide

이 문서는 Forgather 프로젝트의 어드민 UI 컨벤션을 정의합니다.
`/admin-ui` skill과 `analyze-admin` agent가 이 가이드를 참조하여 일관된 코드를 생성합니다.

## 디렉토리 구조

```
src/main/resources/
├── templates/admin/
│   ├── layout/
│   │   ├── base.html           # 기본 레이아웃
│   │   └── fragments.html      # 공통 fragment
│   ├── spaces/
│   │   └── list.html           # 스페이스 목록
│   ├── hosts/
│   │   └── list.html           # 호스트 목록
│   └── login.html              # 로그인 페이지
├── static/css/admin/
│   ├── common.css              # 공통 스타일, CSS 변수 정의
│   ├── login.css
│   ├── spaces.css
│   └── hosts.css
└── static/js/admin/
    ├── auth.js                 # Auth 전역 객체
    ├── api.js                  # API 전역 객체
    ├── pagination.js           # PaginationUtil 전역 객체
    ├── login.js
    ├── spaces.js
    └── hosts.js
```

## CSS 변수 목록

`common.css`의 `:root`에 정의된 CSS 변수:

### 색상

| 변수 | 값 | 용도 |
|-----|-----|------|
| `--primary-color` | `#2563eb` | 주요 액션, 링크 |
| `--primary-hover` | `#1d4ed8` | 주요 색상 hover |
| `--secondary-color` | `#64748b` | 보조 요소 |
| `--success-color` | `#10b981` | 성공, 활성 상태 |
| `--danger-color` | `#ef4444` | 에러, 삭제 |
| `--warning-color` | `#f59e0b` | 경고 |
| `--info-color` | `#3b82f6` | 정보 |

### 배경/테두리

| 변수 | 값 | 용도 |
|-----|-----|------|
| `--bg-color` | `#f8fafc` | 페이지 배경 |
| `--surface-color` | `#ffffff` | 카드, 모달 배경 |
| `--border-color` | `#e2e8f0` | 테두리 |

### 텍스트

| 변수 | 값 | 용도 |
|-----|-----|------|
| `--text-primary` | `#1e293b` | 기본 텍스트 |
| `--text-secondary` | `#64748b` | 보조 텍스트 |
| `--text-muted` | `#94a3b8` | 비활성 텍스트 |

### 간격

| 변수 | 값 |
|-----|-----|
| `--spacing-xs` | `0.5rem` (8px) |
| `--spacing-sm` | `0.75rem` (12px) |
| `--spacing-md` | `1rem` (16px) |
| `--spacing-lg` | `1.5rem` (24px) |
| `--spacing-xl` | `2rem` (32px) |
| `--spacing-2xl` | `3rem` (48px) |

### 기타

| 변수 | 값 |
|-----|-----|
| `--radius-sm` | `0.25rem` |
| `--radius-md` | `0.5rem` |
| `--radius-lg` | `0.75rem` |
| `--shadow-sm` | `0 1px 2px 0 rgba(0, 0, 0, 0.05)` |
| `--shadow-md` | `0 4px 6px -1px rgba(0, 0, 0, 0.1), ...` |
| `--shadow-lg` | `0 10px 15px -3px rgba(0, 0, 0, 0.1), ...` |

## 전역 JavaScript 객체

### API 객체 (`api.js`)

HTTP 요청 유틸리티. 세션 기반 인증 자동 처리.

```javascript
// GET 요청
const data = await API.get('/endpoint', { param1: 'value1' });

// POST 요청
const result = await API.post('/endpoint', { data: 'value' });

// PUT 요청
await API.put('/endpoint', { data: 'value' });

// DELETE 요청
await API.delete('/endpoint');

// 기존 API 메서드
await API.getSpaces(page, size);
await API.getSpacesByFilters(page, size, filters);
await API.getSpaceDetail(spaceCode);
await API.getHosts(page, size);
```

### Auth 객체 (`auth.js`)

인증 유틸리티.

```javascript
// 로그아웃 (서버 API 호출 후 로그인 페이지로 리다이렉트)
Auth.logout();

// 로그인 페이지로 리다이렉트
Auth.redirectToLogin();
```

### PaginationUtil 객체 (`pagination.js`)

페이지네이션 UI 렌더링.

```javascript
PaginationUtil.render(
    container,      // HTMLElement - 페이지네이션을 렌더링할 DOM 요소
    currentPage,    // number - 현재 페이지 (1-based)
    totalPages,     // number - 전체 페이지 수
    totalCount,     // number - 전체 아이템 개수
    onPageClick,    // function - 페이지 클릭 콜백 (pageNumber를 인자로 받음)
    options         // object (optional) - { maxPageButtons: 5 }
);
```

## 공통 HTML 구조

### 페이지 기본 구조

```html
<!DOCTYPE html>
<html lang="ko" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Page Title - Forgather Admin</title>
    <link rel="stylesheet" th:href="@{/css/admin/common.css}">
    <link rel="stylesheet" th:href="@{/css/admin/{page}.css}">
</head>
<body>
    <header class="header">...</header>
    <main class="main-content">
        <div class="container">
            <div class="page-header">...</div>
            <div class="controls">...</div>
            <div id="loadingSpinner" class="loading-spinner" style="display: none;">...</div>
            <div id="errorMessage" class="error-message" style="display: none;"></div>
            <div class="table-container">...</div>
            <div class="pagination" id="pagination" style="display: none;"></div>
        </div>
    </main>
    <footer class="footer">...</footer>
    <div id="toast" class="toast"></div>

    <!-- Scripts (순서 중요) -->
    <script th:src="@{/js/admin/auth.js}"></script>
    <script th:src="@{/js/admin/api.js}"></script>
    <script th:src="@{/js/admin/pagination.js}"></script>
    <script th:src="@{/js/admin/{page}.js}"></script>
</body>
</html>
```

### 테이블 구조

```html
<div class="table-container">
    <table class="data-table" id="{entity}Table">
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <!-- ... -->
            </tr>
        </thead>
        <tbody id="{entity}TableBody">
            <!-- JavaScript로 동적 생성 -->
        </tbody>
    </table>
</div>
```

### 모달 구조

```html
<div id="{entity}DetailModal" class="modal-overlay" style="display: none;"
     role="dialog" aria-labelledby="modalTitle" aria-modal="true">
    <div class="modal-container">
        <div class="modal-header">
            <h3 id="modalTitle" class="modal-title">Title</h3>
            <button id="closeModalBtn" class="modal-close-btn" aria-label="Close modal">
                <span aria-hidden="true">&times;</span>
            </button>
        </div>
        <div class="modal-body">
            <div id="modalLoading" class="modal-loading" style="display: none;">...</div>
            <div id="modalError" class="modal-error" style="display: none;"></div>
            <div id="modalContent" class="modal-content" style="display: none;">
                <!-- 내용 -->
            </div>
        </div>
        <div class="modal-footer">
            <!-- 액션 버튼 -->
        </div>
    </div>
</div>
```

## JavaScript 패턴

### 기본 페이지 스크립트 구조

```javascript
// 전역 상태
let currentPage = 1;
let currentPageSize = 15;
let totalPages = 1;
let totalCount = 0;

const filterState = {
    // 필터 상태
};

document.addEventListener('DOMContentLoaded', function () {
    // DOM 요소 참조
    const tableBody = document.getElementById('{entity}TableBody');
    // ...

    // UI 함수
    function showLoading() { ... }
    function hideLoading() { ... }
    function showError(message) { ... }
    function hideError() { ... }
    function showToast(message, type) { ... }

    // 유틸리티 함수
    function formatDateTime(dateTimeString) { ... }
    function escapeHtml(text) { ... }

    // 렌더링 함수
    function renderTable(items) { ... }
    function updatePagination() { ... }

    // 데이터 로드
    async function loadData() { ... }
    function goToPage(page) { ... }

    // 이벤트 핸들러
    function handlePageSizeChange() { ... }
    function handleLogout() { ... }

    // 이벤트 리스너 등록
    // ...

    // 초기화
    loadData();
});
```

### XSS 방지

**반드시** 사용자 입력 또는 서버 데이터를 HTML에 삽입할 때 `escapeHtml()` 사용:

```javascript
function escapeHtml(text) {
    if (text === null || text === undefined) {
        return '';
    }
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.toString().replace(/[&<>"']/g, m => map[m]);
}

// 사용 예
const row = `<td>${escapeHtml(item.name)}</td>`;
```

### 이벤트 위임 패턴

동적으로 생성되는 요소의 이벤트 처리:

```javascript
tableBody.addEventListener('click', function (event) {
    const target = event.target;

    // 클릭 대상 검증
    if (target.tagName !== 'TD' || !target.hasAttribute('data-item-id')) {
        return;
    }

    const itemId = target.getAttribute('data-item-id');
    if (itemId) {
        loadDetail(itemId);
    }
});
```

## 참조 파일

새 페이지 생성 시 다음 파일을 참조:

- **HTML 구조**: `templates/admin/spaces/list.html`
- **CSS 스타일**: `static/css/admin/spaces.css`
- **JavaScript 로직**: `static/js/admin/spaces.js`
- **공통 스타일**: `static/css/admin/common.css`
- **전역 객체**: `static/js/admin/api.js`, `auth.js`, `pagination.js`
