# Implementation Plan: Forgather Admin UI/UX Improvement with Tailwind CSS

## Overview

Forgather Admin 페이지의 UI/UX를 개선하기 위한 단계별 구현 계획입니다. 기존 API 연동 코드(fetch, form action, th:href, JavaScript 로직)를 절대 변경하지 않으면서 Tailwind CSS 기반으로 스타일만 전환합니다.

---

## Requirements

1. **스타일 시스템 전환**: 커스텀 CSS에서 Tailwind CSS로 마이그레이션
2. **API 연동 보존**: 모든 fetch 호출, form action, th:href 등 기능 코드 유지
3. **점진적 개선**: 한 번에 하나의 파일/컴포넌트만 수정
4. **테스트 가능성**: 각 단계 완료 후 독립적으로 기능 테스트 가능
5. **모바일 대응**: 반응형 디자인 유지/개선

---

## Current Architecture Analysis

### 파일 구조

```
src/main/resources/
├── templates/admin/
│   ├── layout/
│   │   ├── base.html           # 기본 레이아웃 (Thymeleaf layout dialect)
│   │   └── fragments.html      # 공통 fragments (header, footer, pagination)
│   ├── login.html              # 로그인 페이지 (독립형)
│   ├── spaces/list.html        # 스페이스 목록 + 상세 모달
│   └── hosts/list.html         # 호스트 목록
├── static/css/admin/
│   ├── common.css              # CSS 변수, 공통 스타일 (567 lines)
│   ├── login.css               # 로그인 전용 스타일 (110 lines)
│   ├── spaces.css              # 스페이스 전용 스타일 (758 lines)
│   └── hosts.css               # 호스트 전용 스타일 (185 lines)
└── static/js/admin/
    ├── auth.js                 # Auth 전역 객체 (logout, redirectToLogin)
    ├── api.js                  # API 전역 객체 (HTTP 요청 유틸리티)
    ├── pagination.js           # PaginationUtil 전역 객체
    ├── login.js                # 로그인 페이지 로직
    ├── spaces.js               # 스페이스 페이지 로직 (867 lines)
    └── hosts.js                # 호스트 페이지 로직 (443 lines)
```

### API 연동 분석

#### 1. login.html + login.js
| 연동 유형 | 코드 | 용도 |
|----------|------|------|
| fetch | `API.login(username, password)` | POST /admin/login |
| redirect | `window.location.href = '/view/admin/spaces'` | 로그인 성공 후 이동 |

#### 2. spaces/list.html + spaces.js
| 연동 유형 | 코드 | 용도 |
|----------|------|------|
| th:href | `@{/view/admin/spaces}`, `@{/view/admin/hosts}` | 네비게이션 링크 |
| fetch | `API.getSpaces(page, size)` | GET /admin/spaces |
| fetch | `API.getSpacesByFilters(page, size, filters)` | GET /admin/spaces/search |
| fetch | `API.searchSpacesByName(name, page, size)` | GET /admin/spaces/search/by-name |
| fetch | `API.getSpaceDetail(spaceCode)` | GET /admin/spaces/{spaceCode} |
| fetch | `Auth.logout()` | POST /admin/logout |
| window.open | `getSpacePageUrl(spaceCode)` | 게스트 페이지 새 탭 열기 |

#### 3. hosts/list.html + hosts.js
| 연동 유형 | 코드 | 용도 |
|----------|------|------|
| th:href | `@{/view/admin/spaces}`, `@{/view/admin/hosts}` | 네비게이션 링크 |
| fetch | `API.getHosts(page, size)` | GET /admin/hosts |
| fetch | `Auth.logout()` | POST /admin/logout |

#### 4. fragments.html
| 연동 유형 | 코드 | 용도 |
|----------|------|------|
| th:href | `@{/view/admin/spaces}` | 로고 클릭 시 이동 |

### 보존해야 할 JavaScript 코드 패턴

1. **전역 상태 변수**: `currentPage`, `currentPageSize`, `totalPages`, `totalCount`, `filterState`, `searchState`
2. **DOM ID 참조**: `spacesTableBody`, `hostsTableBody`, `pagination`, `loadingSpinner`, `errorMessage` 등
3. **이벤트 리스너**: `addEventListener('click', ...)`, `addEventListener('change', ...)` 등
4. **data 속성**: `data-space-code` (이벤트 위임 패턴)
5. **모달 제어**: `openModal()`, `closeModal()`, `.show` 클래스 토글

---

## Implementation Steps

### Phase 1: Tailwind CSS 환경 구축 및 공통 인프라

#### Step 1.1: Tailwind CSS CDN 설정 및 커스텀 설정
**File**: `src/main/resources/templates/admin/layout/base.html`

**Action**:
- Tailwind CSS CDN 스크립트 추가 (`<script src="https://cdn.tailwindcss.com">`)
- `tailwind.config` 인라인 설정 추가 (기존 CSS 변수와 매핑되는 색상/간격 정의)
- 기존 common.css 링크는 유지 (점진적 전환을 위해)

**Dependencies**: 없음

**Risk**: Low - CDN 추가만으로 기존 코드에 영향 없음

**테스트 방법**:
- 로컬 서버 실행 후 모든 페이지 정상 렌더링 확인
- 브라우저 개발자 도구에서 Tailwind 클래스 적용 가능 여부 확인

---

#### Step 1.2: Tailwind 커스텀 테마 설정
**File**: `src/main/resources/templates/admin/layout/base.html` (인라인 config)

**Action**:
- 기존 CSS 변수와 동일한 값으로 Tailwind 테마 확장
```javascript
tailwind.config = {
  theme: {
    extend: {
      colors: {
        primary: { DEFAULT: '#2563eb', hover: '#1d4ed8' },
        secondary: { DEFAULT: '#64748b', hover: '#475569' },
        success: '#10b981',
        danger: '#ef4444',
        warning: '#f59e0b',
        info: '#3b82f6',
        surface: '#ffffff',
        'bg-color': '#f8fafc',
        'border-color': '#e2e8f0',
        'text-primary': '#1e293b',
        'text-secondary': '#64748b',
        'text-muted': '#94a3b8',
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['SF Mono', 'Monaco', 'Cascadia Code', 'monospace'],
      },
    },
  },
}
```

**Dependencies**: Step 1.1

**Risk**: Low - 설정만 추가, 기존 CSS와 공존

**테스트 방법**:
- 임시로 `<div class="bg-primary text-white p-4">Test</div>` 추가하여 색상 적용 확인

---

### Phase 2: 공통 컴포넌트 마이그레이션

#### Step 2.1: Header Fragment Tailwind 전환
**File**: `src/main/resources/templates/admin/layout/fragments.html`

**Action**:
- `<header th:fragment="header">` 내부 HTML의 class 속성을 Tailwind 클래스로 교체
- 기존 CSS 클래스: `.header`, `.header-container`, `.header-left`, `.logo`, `.header-nav`, `.nav-list`, `.nav-item`, `.nav-link`, `.btn`, `.btn-secondary`
- **절대 변경 금지**: `th:href="@{/view/admin/spaces}"`, `id="logoutBtn"`, `th:classappend="${currentPage == 'spaces'} ? 'active' : ''"`

**Tailwind 클래스 매핑 예시**:
```html
<header class="bg-white border-b border-gray-200 shadow-sm sticky top-0 z-50">
  <div class="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
    ...
  </div>
</header>
```

**Dependencies**: Step 1.2

**Risk**: Medium - 여러 페이지에서 사용하는 fragment이므로 신중하게 진행

**테스트 방법**:
- `/view/admin/spaces`, `/view/admin/hosts` 모두 방문하여 헤더 렌더링 확인
- 네비게이션 링크 클릭하여 페이지 이동 확인
- Logout 버튼 클릭하여 로그아웃 동작 확인

---

#### Step 2.2: Footer Fragment Tailwind 전환
**File**: `src/main/resources/templates/admin/layout/fragments.html`

**Action**:
- `<footer th:fragment="footer">` 내부 HTML의 class 속성을 Tailwind 클래스로 교체
- 기존 CSS 클래스: `.footer`, `.footer-container`

**Tailwind 클래스 매핑 예시**:
```html
<footer class="bg-white border-t border-gray-200 mt-auto">
  <div class="max-w-7xl mx-auto px-6 py-6 text-center">
    <p class="text-gray-400 text-sm">&copy; 2025 Forgather. All rights reserved.</p>
  </div>
</footer>
```

**Dependencies**: Step 2.1

**Risk**: Low - 단순한 컴포넌트

**테스트 방법**: 모든 페이지에서 푸터 표시 확인

---

#### Step 2.3: Loading Spinner Fragment Tailwind 전환
**File**: `src/main/resources/templates/admin/layout/fragments.html`

**Action**:
- `<div th:fragment="loading">` 내부 스타일을 Tailwind로 전환
- 기존 CSS 클래스: `.loading-spinner`, `.spinner`
- **절대 변경 금지**: `id="loadingSpinner"`, `style="display: none;"`

**Dependencies**: Step 2.2

**Risk**: Low

**테스트 방법**:
- 스페이스/호스트 페이지에서 데이터 로딩 시 스피너 표시 확인
- 개발자 도구에서 `display` 스타일이 JS에 의해 `flex`로 변경되는지 확인

---

#### Step 2.4: Toast Notification Fragment Tailwind 전환
**File**: `src/main/resources/templates/admin/layout/fragments.html`

**Action**:
- `<div th:fragment="toast">` 내부 스타일을 Tailwind로 전환
- 기존 CSS 클래스: `.toast`, `.toast.show`, `.toast.success`, `.toast.error`, `.toast.warning`
- **절대 변경 금지**: `id="toast"`, JS에서 추가/제거하는 `show`, `success`, `error`, `warning` 클래스명

**중요**: JavaScript에서 `toast.className = 'toast ${type}'`와 `toast.classList.add('show')` 패턴을 사용하므로, 이 클래스명들은 유지하되 Tailwind 유틸리티와 조합

**Dependencies**: Step 2.3

**Risk**: Medium - JS와 CSS 클래스 연동 주의

**테스트 방법**:
- 로그아웃 시도 후 취소하여 toast 동작 확인 (showToast 함수 사용 시)
- 에러 발생 시나리오 테스트

---

### Phase 3: 로그인 페이지 마이그레이션

#### Step 3.1: Login Page HTML Tailwind 전환
**File**: `src/main/resources/templates/admin/login.html`

**Action**:
- 전체 페이지 HTML 구조를 Tailwind 클래스로 전환
- 기존 CSS 클래스: `.login-page`, `.login-container`, `.login-box`, `.login-header`, `.login-form`, `.form-group`, `.form-control`, `.error-message`, `.btn`, `.btn-primary`, `.btn-block`, `.login-footer`, `.loading-spinner`
- **절대 변경 금지**:
  - `id="loginForm"`, `id="username"`, `id="password"`, `id="loginBtn"`, `id="errorMessage"`, `id="loadingSpinner"`
  - `th:href="@{/css/admin/common.css}"`, `th:src="@{/js/admin/...}"`
  - `type="text"`, `type="password"`, `type="submit"`, `name="username"`, `name="password"`
  - `required`, `autofocus` 속성

**Dependencies**: Step 1.2

**Risk**: Medium - 독립 페이지라 다른 페이지에 영향 없음, 로그인 기능 테스트 필수

**테스트 방법**:
1. 로그인 페이지 접속 (`/view/admin/login`)
2. 빈 폼 제출 시 에러 메시지 표시 확인
3. 잘못된 자격증명으로 로그인 시도 후 에러 메시지 확인
4. 올바른 자격증명으로 로그인 성공 후 `/view/admin/spaces`로 리다이렉트 확인
5. 로딩 스피너 표시/숨김 동작 확인

---

#### Step 3.2: Login CSS 파일 정리 (선택적 삭제 또는 최소화)
**File**: `src/main/resources/static/css/admin/login.css`

**Action**:
- Step 3.1 완료 후 더 이상 사용되지 않는 CSS 규칙 삭제
- 또는 파일 전체를 빈 파일로 유지 (점진적 전환 완료 시 삭제 예정)

**Dependencies**: Step 3.1

**Risk**: Low - 사용되지 않는 코드 제거

---

### Phase 4: Hosts 페이지 마이그레이션

#### Step 4.1: Hosts Page Header/Footer Fragment 사용으로 전환
**File**: `src/main/resources/templates/admin/hosts/list.html`

**Action**:
- 인라인 헤더/푸터를 fragment 참조로 교체 (현재 인라인으로 중복 작성되어 있음)
- `<header th:replace="~{admin/layout/fragments :: header}"></header>`
- `<footer th:replace="~{admin/layout/fragments :: footer}"></footer>`
- **절대 변경 금지**: `id="logoutBtn"` (fragment에 있음)

**주의**: 현재 hosts/list.html은 base.html 레이아웃을 사용하지 않고 독립형으로 작성됨. fragments 사용 시 `currentPage` 변수 전달 필요

**Dependencies**: Step 2.4

**Risk**: Medium - Thymeleaf fragment 참조 구조 변경

**테스트 방법**:
- `/view/admin/hosts` 접속
- 헤더 네비게이션 동작 확인 (Spaces 링크, Hosts 링크 active 상태)
- 로그아웃 버튼 동작 확인

---

#### Step 4.2: Hosts Page Main Content Tailwind 전환
**File**: `src/main/resources/templates/admin/hosts/list.html`

**Action**:
- `.main-content`, `.container`, `.page-header`, `.controls`, `.page-size-selector`, `.loading-spinner`, `.error-message`, `.table-container`, `.data-table`, `.pagination`, `.toast` 등 Tailwind로 전환
- **절대 변경 금지**:
  - `id="pageSize"`, `id="loadingSpinner"`, `id="errorMessage"`, `id="hostsTable"`, `id="hostsTableBody"`, `id="pagination"`, `id="toast"`, `id="logoutBtn"`
  - `<option value="15">`, `<option value="30">`, `<option value="50">` (페이지 크기 옵션)

**Dependencies**: Step 4.1

**Risk**: Medium

**테스트 방법**:
1. 페이지 로드 시 호스트 목록 표시 확인
2. 페이지 크기 변경 (15/30/50) 후 테이블 재렌더링 확인
3. 페이지네이션 버튼 클릭하여 페이지 이동 확인
4. 빈 데이터 시 Empty State 표시 확인
5. 로딩 스피너 동작 확인
6. 키보드 화살표로 페이지 이동 확인

---

#### Step 4.3: Hosts CSS 파일 정리
**File**: `src/main/resources/static/css/admin/hosts.css`

**Action**: Step 4.2 완료 후 사용되지 않는 CSS 규칙 정리

**Dependencies**: Step 4.2

**Risk**: Low

---

### Phase 5: Spaces 페이지 마이그레이션

#### Step 5.1: Spaces Page Header/Footer Fragment 사용으로 전환
**File**: `src/main/resources/templates/admin/spaces/list.html`

**Action**: hosts 페이지와 동일하게 fragment 참조로 변경

**Dependencies**: Step 4.3

**Risk**: Medium

**테스트 방법**: 헤더/푸터 렌더링 및 네비게이션 동작 확인

---

#### Step 5.2: Spaces Page Controls Section Tailwind 전환
**File**: `src/main/resources/templates/admin/spaces/list.html`

**Action**:
- `.controls`, `.name-search-section`, `.search-input-wrapper`, `.search-input`, `.btn-search`, `.btn-reset`, `.filter-section`, `.filter-group`, `.filter-label`, `.filter-radio-label`, `.btn-filter`, `.page-size-selector` 등 Tailwind로 전환
- **절대 변경 금지**:
  - `id="nameSearchInput"`, `id="nameSearchBtn"`, `id="nameSearchResetBtn"`, `id="applyFilterBtn"`, `id="pageSize"`
  - `name="hasProduct"`, `value="all"`, `value="true"`, `value="false"`, `checked` 속성
  - `type="radio"`, `type="text"`, `placeholder` 속성

**Dependencies**: Step 5.1

**Risk**: Medium - 필터/검색 기능 복잡

**테스트 방법**:
1. 이름 검색 입력 후 검색 버튼 클릭 → 필터링된 결과 표시
2. 이름 검색 후 초기화 버튼 클릭 → 전체 목록으로 복원
3. 필터 라디오 버튼 선택 후 검색 버튼 클릭 → 필터링된 결과 표시
4. Enter 키로 이름 검색 실행 확인
5. 페이지 크기 변경 동작 확인

---

#### Step 5.3: Spaces Page Table Section Tailwind 전환
**File**: `src/main/resources/templates/admin/spaces/list.html`

**Action**:
- `.table-container`, `.data-table`, `thead`, `tbody`, `th`, `td`, `.badge`, `.badge-success`, `.badge-danger`, `.empty-state` 등 Tailwind로 전환
- **절대 변경 금지**:
  - `id="spacesTable"`, `id="spacesTableBody"`
  - 테이블 헤더 컬럼 순서 및 텍스트

**Dependencies**: Step 5.2

**Risk**: Medium

**테스트 방법**:
1. 스페이스 목록 테이블 렌더링 확인
2. Public/Private 배지 스타일 확인
3. 스페이스 코드 클릭 시 모달 열림 확인
4. 빈 상태 Empty State 표시 확인
5. 테이블 행 hover 효과 확인

---

#### Step 5.4: Spaces Page Modal Section Tailwind 전환
**File**: `src/main/resources/templates/admin/spaces/list.html`

**Action**:
- `.modal-overlay`, `.modal-container`, `.modal-header`, `.modal-title`, `.modal-close-btn`, `.modal-body`, `.modal-loading`, `.spinner-small`, `.modal-error`, `.modal-content`, `.modal-info-group`, `.modal-info-label`, `.modal-info-value`, `.modal-code`, `.modal-footer`, `body.modal-open` 등 Tailwind로 전환
- **절대 변경 금지**:
  - `id="spaceDetailModal"`, `id="closeModalBtn"`, `id="modalLoading"`, `id="modalError"`, `id="modalContent"`, `id="visitSpaceBtn"`, `id="modalTitle"`
  - `id="modalSpaceId"`, `id="modalSpaceCode"`, `id="modalSpaceName"`, `id="modalSpacePublic"`, `id="modalHasProduct"`, `id="modalGuestBookCount"`
  - `role="dialog"`, `aria-labelledby="modalTitle"`, `aria-modal="true"`, `aria-label` 속성
  - `style="display: none;"` (초기 숨김 상태)
  - `.show` 클래스 (JS에서 토글)

**주의**: JavaScript에서 `.show` 클래스 추가/제거로 모달 표시를 제어하므로, Tailwind 전환 시에도 `.show` 클래스 관련 스타일 유지 필요

**Dependencies**: Step 5.3

**Risk**: High - 모달 동작 복잡, JS 연동 주의

**테스트 방법**:
1. 스페이스 코드 클릭 → 모달 열림 + 로딩 스피너 표시
2. 데이터 로드 완료 → 스페이스 상세 정보 표시
3. "게스트 페이지로 이동" 버튼 클릭 → 새 탭에서 게스트 페이지 열림
4. X 버튼 클릭 → 모달 닫힘
5. 오버레이(배경) 클릭 → 모달 닫힘
6. ESC 키 → 모달 닫힘
7. 모달 열림 시 배경 스크롤 잠금 확인
8. 존재하지 않는 스페이스 코드로 에러 메시지 표시 확인

---

#### Step 5.5: Spaces CSS 파일 정리
**File**: `src/main/resources/static/css/admin/spaces.css`

**Action**: Step 5.4 완료 후 사용되지 않는 CSS 규칙 정리

**Dependencies**: Step 5.4

**Risk**: Low

---

### Phase 6: Common CSS 정리 및 최종 마이그레이션

#### Step 6.1: Common CSS에서 JS 연동 클래스만 유지
**File**: `src/main/resources/static/css/admin/common.css`

**Action**:
- JavaScript에서 동적으로 추가/제거/참조하는 클래스만 유지:
  - `.toast.show`, `.toast.success`, `.toast.error`, `.toast.warning`
  - `.modal-overlay.show`
  - `.pagination-btn.active`
  - `body.modal-open`
- 나머지 순수 스타일링 클래스는 삭제 (Tailwind로 대체됨)
- CSS 변수는 제거해도 되나, Tailwind config에서 동일 값 사용 중이므로 안전

**Dependencies**: Step 5.5

**Risk**: Medium - JS 연동 클래스 누락 시 기능 오류

**테스트 방법**: 전체 기능 회귀 테스트

---

### Phase 7: 인터랙션 및 애니메이션 개선

#### Step 7.1: 버튼 Hover/Active 효과 개선
**File**: 모든 HTML 파일

**Action**:
- Tailwind transition 클래스 추가: `transition-all duration-200`
- Hover 효과: `hover:bg-primary-hover`, `hover:scale-[0.98]` 등
- Focus 효과: `focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2`

**Dependencies**: Step 6.1

**Risk**: Low

---

#### Step 7.2: 테이블 행 Hover 효과 개선
**File**: spaces.js, hosts.js (테이블 렌더링 함수)

**Action**:
- 동적 생성되는 `<tr>` 태그에 Tailwind 클래스 추가
- **주의**: JavaScript 코드 내 템플릿 리터럴 수정 필요
- 기존: `<tr>` → 변경: `<tr class="hover:bg-gray-50 transition-colors">`

**Dependencies**: Step 7.1

**Risk**: Low - 스타일 클래스만 추가

---

#### Step 7.3: 모달 애니메이션 개선
**File**: common.css 또는 인라인 style

**Action**:
- 모달 열림/닫힘 시 fade + slide 애니메이션 개선
- Tailwind의 `@keyframes` 또는 `animate-*` 유틸리티 활용

**Dependencies**: Step 7.2

**Risk**: Low

---

#### Step 7.4: 페이지네이션 버튼 애니메이션 개선
**File**: pagination.js

**Action**:
- `_createButton` 함수에서 생성하는 버튼에 Tailwind transition 클래스 추가
- 기존: `button.className = 'pagination-btn'`
- 변경: `button.className = 'pagination-btn transition-all duration-200 hover:scale-105'`

**Dependencies**: Step 7.3

**Risk**: Low

---

## Testing Strategy

### Unit Tests (기존 테스트 유지)
- `src/test/java/com/forgather/back_office/` 하위 테스트 실행
- `./gradlew test --tests "com.forgather.back_office.*"`

### Integration Tests (수동 테스트)
각 Phase 완료 후 수행:

1. **인증 흐름**
   - 로그인 → 스페이스 페이지 → 로그아웃 → 로그인 페이지 리다이렉트
   - 세션 만료 후 API 호출 시 로그인 페이지 리다이렉트

2. **스페이스 관리**
   - 목록 조회 (페이지네이션, 페이지 크기 변경)
   - 이름 검색 (검색, 초기화)
   - 필터 검색 (전체/등록함/미등록)
   - 상세 모달 (열기, 닫기, 게스트 페이지 이동)

3. **호스트 관리**
   - 목록 조회 (페이지네이션, 페이지 크기 변경)
   - 빈 상태 표시

### E2E Tests (선택적)
- Selenium 또는 Playwright로 전체 사용자 시나리오 테스트

---

## Risks & Mitigations

### Risk 1: JavaScript 클래스 참조 누락
- **Description**: CSS 클래스명을 변경했으나 JavaScript에서 해당 클래스를 참조하는 경우
- **Mitigation**:
  - 모든 JS 파일에서 `classList`, `className`, `querySelector` 사용 부분 검색
  - CSS 클래스 삭제 전 전체 프로젝트 Grep 수행
  - `.show`, `.active`, `.error`, `.success` 등 상태 클래스는 반드시 유지

### Risk 2: Thymeleaf 동적 클래스 누락
- **Description**: `th:classappend` 등으로 동적 추가되는 클래스 누락
- **Mitigation**:
  - HTML 파일에서 `th:class`, `th:classappend` 검색
  - 동적 클래스(`active` 등)는 Tailwind와 병행 사용

### Risk 3: 반응형 디자인 회귀
- **Description**: Tailwind 전환 시 모바일 레이아웃 깨짐
- **Mitigation**:
  - 각 Step 완료 후 Chrome DevTools의 반응형 모드로 테스트
  - 768px 이하 breakpoint 집중 테스트

### Risk 4: 브라우저 호환성
- **Description**: Tailwind CDN 버전과 특정 브라우저 호환 문제
- **Mitigation**:
  - Tailwind CDN v3.x 사용 (IE 미지원, 모던 브라우저 타겟)
  - Chrome, Firefox, Safari, Edge 테스트

---

## Success Criteria

- [ ] 모든 페이지가 Tailwind CSS 기반으로 스타일링됨
- [ ] 기존 API 연동 코드(fetch, form action, th:href 등)가 100% 동일하게 동작
- [ ] JavaScript 로직(이벤트 핸들러, 상태 관리, DOM 조작)이 정상 동작
- [ ] 반응형 디자인이 768px 이하에서 정상 작동
- [ ] 로그인 → 스페이스 관리 → 호스트 관리 → 로그아웃 전체 흐름 정상 동작
- [ ] 기존 CSS 파일 크기 대비 50% 이상 감소 (불필요한 커스텀 CSS 제거)
- [ ] Lighthouse 접근성 점수 90점 이상 유지

---

## File Summary

| 파일 경로 | 작업 유형 | Phase |
|----------|----------|-------|
| `templates/admin/layout/base.html` | Tailwind CDN 추가, config 설정 | 1 |
| `templates/admin/layout/fragments.html` | Header/Footer/Loading/Toast Tailwind 전환 | 2 |
| `templates/admin/login.html` | 전체 페이지 Tailwind 전환 | 3 |
| `static/css/admin/login.css` | 정리/삭제 | 3 |
| `templates/admin/hosts/list.html` | Fragment 참조 + Tailwind 전환 | 4 |
| `static/css/admin/hosts.css` | 정리/삭제 | 4 |
| `templates/admin/spaces/list.html` | Fragment 참조 + Tailwind 전환 | 5 |
| `static/css/admin/spaces.css` | 정리/삭제 | 5 |
| `static/css/admin/common.css` | JS 연동 클래스만 유지 | 6 |
| `static/js/admin/spaces.js` | 테이블 행 클래스 추가 (선택적) | 7 |
| `static/js/admin/hosts.js` | 테이블 행 클래스 추가 (선택적) | 7 |
| `static/js/admin/pagination.js` | 버튼 클래스 추가 (선택적) | 7 |
