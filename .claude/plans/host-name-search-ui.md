# 호스트 이름 검색 UI 구현 계획

## 개요

호스트 관리 페이지(`hosts/list.html`)에 이름 검색 UI를 추가하여 백엔드 API 엔드포인트 `GET /admin/hosts/search/by-name`과 연동한다.
스페이스 페이지(`spaces/list.html` + `spaces.js`)에 이미 구현된 이름 검색 패턴을 동일하게 따른다.

## 사용 API

- **엔드포인트**: `GET /admin/hosts/search/by-name`
- **컨트롤러**: `AdminHostController.getHostsByName()`
- **요청 파라미터**:
  - `name` (String, optional, default `""`) - 검색할 호스트 이름 (부분 일치)
  - `page` (int) - 페이지 번호
  - `size` (int, default 15) - 페이지 크기
  - `sort` (String, default `createdAt,DESC`) - 정렬
- **응답 DTO**: `AdminHostResponse` (전체 목록 조회와 동일한 응답 구조)
  ```json
  {
    "hosts": [
      {
        "id": 1,
        "name": "홍길동",
        "createdAt": "2025-11-14T10:30:00",
        "spaceCount": 3
      }
    ],
    "currentPage": 1,
    "pageSize": 15,
    "totalCount": 42,
    "totalPages": 3
  }
  ```

## 참조 패턴: 스페이스 이름 검색 (spaces 페이지)

스페이스 페이지에 이미 구현된 이름 검색 패턴을 그대로 따른다.

### HTML 패턴 (spaces/list.html)
- 테이블 상단에 총 개수 + 검색 input + 검색 버튼 + 초기화 버튼 배치
- `nameSearchInput`: `<input type="text">`, placeholder "스페이스 이름 검색..."
- `nameSearchBtn`: 돋보기 SVG 아이콘 버튼 (input 내부 우측 절대 위치)
- `resetSearchBtn`: "초기화" 텍스트 버튼, 검색 활성화 시에만 표시 (`display: none` 기본)

### JavaScript 패턴 (spaces.js)
- `searchState` 전역 객체: `{ searchName: null }`
- `searchByName()`: 검색어 읽기 → 빈 값이면 `resetNameSearch()` → `searchState` 업데이트 → `loadSpaces()`
- `resetNameSearch()`: input 비우기 → `searchState` 초기화 → `loadSpaces()`
- `hasActiveNameSearch()`: `searchState.searchName !== null && searchState.searchName.trim() !== ''`
- `updateNameSearchUI()`: 초기화 버튼 표시/숨김
- `loadSpaces()` 내 분기: `hasActiveNameSearch()` 이면 `API.searchSpacesByName()` 호출

### API 패턴 (api.js)
- `searchSpacesByName(name, page, size, sort)`: `this.get('/spaces/search/by-name', { name, page, size, sort })`

## 수정 파일 목록

| # | 파일 | 변경 유형 | 설명 |
|---|------|-----------|------|
| 1 | `static/js/admin/api.js` | 메서드 추가 | `searchHostsByName()` |
| 2 | `templates/admin/hosts/list.html` | HTML 추가 | 검색 input + 버튼 영역 |
| 3 | `static/js/admin/hosts.js` | 상태+함수+이벤트 추가 | 검색 로직 전체 |

### 변경하지 않는 파일

| 파일 | 이유 |
|------|------|
| `hosts.css` | 현재 비어있고, 검색 UI는 Tailwind 인라인 클래스로 처리 |
| `common.css` | 추가 공통 스타일 불필요 |
| `pagination.js` | 기존 PaginationUtil 그대로 사용 |
| `AdminHostController.java` | 이미 구현 완료 |
| `AdminHostService.java` | 이미 구현 완료 |

## 구현 단계

### Step 1: api.js에 searchHostsByName() 메서드 추가

**파일**: `src/main/resources/static/js/admin/api.js`
**위치**: `getHostSpaces()` 메서드 아래

```javascript
async searchHostsByName(name, page = 1, size = 15, sort = 'createdAt,desc') {
    return this.get('/hosts/search/by-name', { name, page, size, sort });
}
```

참고: `searchSpacesByName()`과 동일한 패턴이며, 엔드포인트만 `/hosts/search/by-name`으로 변경.

---

### Step 2: hosts/list.html에 검색 UI HTML 추가

**파일**: `src/main/resources/templates/admin/hosts/list.html`
**위치**: Controls 영역과 테이블 사이에 삽입

#### 변경 내용

spaces/list.html의 검색 바 패턴을 그대로 따르되:
1. 기존 Controls에서 `totalCountInfo` div 제거 → 새로운 검색 바 영역의 좌측에 통합
2. Controls과 테이블 사이에 새로운 "총 개수 + 검색" 행 삽입

```html
<div class="flex flex-wrap items-center justify-between gap-md mb-md">
    <div id="totalCountInfo" class="text-sm text-text-secondary font-medium"></div>
    <div class="flex items-center gap-sm">
        <div class="relative">
            <input type="text"
                   id="nameSearchInput"
                   class="w-64 pl-md pr-12 py-2.5 min-h-[44px] text-sm border border-border-color rounded-lg bg-surface text-text-primary placeholder-text-muted
                          focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary/20
                          transition-all duration-200"
                   placeholder="호스트 이름 검색..."
                   autocomplete="off">
            <button type="button" id="nameSearchBtn"
                    class="absolute right-0 top-1/2 -translate-y-1/2 p-2.5 min-w-[44px] min-h-[44px] flex items-center justify-center text-text-muted hover:text-primary rounded transition-colors duration-150 cursor-pointer"
                    aria-label="검색">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                </svg>
            </button>
        </div>
        <button type="button" id="resetSearchBtn"
                class="px-md py-2.5 min-h-[44px] text-sm font-medium text-text-secondary bg-surface border border-border-color rounded-lg whitespace-nowrap
                       hover:bg-bg-color hover:text-text-primary active:scale-[0.98]
                       transition-all duration-200 cursor-pointer"
                style="display: none;">
            초기화
        </button>
    </div>
</div>
```

### 디자인 레이아웃

```
┌──────────────────────────────────────────────────────────────────┐
│ Total: 42개                         [호스트 이름 검색... 🔍] [초기화] │
└──────────────────────────────────────────────────────────────────┘
```

---

### Step 3: hosts.js에 검색 상태 및 로직 추가

**파일**: `src/main/resources/static/js/admin/hosts.js`

#### 3-1. 전역 검색 상태 추가 (파일 상단, sortState 아래)

```javascript
const searchState = {
    searchName: null
};
```

#### 3-2. 검색 헬퍼 함수 추가

spaces.js의 패턴을 그대로 따른다:

```javascript
function hasActiveNameSearch() {
    return searchState.searchName !== null && searchState.searchName.trim() !== '';
}

function searchByName() {
    const nameInput = document.getElementById('nameSearchInput');
    const searchName = nameInput.value.trim();

    if (!searchName) {
        resetNameSearch();
        return;
    }

    searchState.searchName = searchName;
    updateNameSearchUI();
    currentPage = 1;
    loadHosts();
}

function resetNameSearch() {
    const nameInput = document.getElementById('nameSearchInput');
    nameInput.value = '';
    searchState.searchName = null;
    updateNameSearchUI();
    currentPage = 1;
    loadHosts();
}

function updateNameSearchUI() {
    const resetBtn = document.getElementById('resetSearchBtn');
    if (resetBtn) {
        resetBtn.style.display = hasActiveNameSearch() ? 'inline-flex' : 'none';
    }
}
```

#### 3-3. loadHosts() 함수 수정

API 호출 부분을 검색 상태에 따라 분기:

```javascript
let response;
const sortParam = `${sortState.field},${sortState.direction}`;

if (hasActiveNameSearch()) {
    response = await API.searchHostsByName(searchState.searchName, currentPage, currentPageSize, sortParam);
} else {
    response = await API.getHosts(currentPage, currentPageSize, sortParam);
}
```

#### 3-4. Empty State 변경 (renderHostsTable 함수)

검색 중 결과가 없을 때 "검색 초기화" 버튼을 추가 (spaces.js 패턴 참고):

```javascript
if (!hosts || hosts.length === 0) {
    const hasSearch = hasActiveNameSearch();
    // hasSearch이면 "검색 조건에 맞는 호스트가 없습니다." + 초기화 버튼
    // 아니면 기존 "표시할 호스트가 없습니다." 메시지
}
```

#### 3-5. Empty State 초기화 버튼 이벤트 처리

`hostsTableBody.addEventListener('click', ...)` 부분에서 `#resetEmptyStateBtn` 클릭 시 `resetNameSearch()` 호출 추가.

#### 3-6. 검색 이벤트 리스너 등록

```javascript
// 검색 버튼 클릭
nameSearchBtn.addEventListener('click', searchByName);

// 초기화 버튼 클릭
resetSearchBtn.addEventListener('click', resetNameSearch);

// Enter 키 입력
nameSearchInput.addEventListener('keydown', function(event) {
    if (event.key === 'Enter') {
        event.preventDefault();
        searchByName();
    }
});
```

#### 3-7. 키보드 네비게이션

기존 `isInputFocused` 체크가 이미 `INPUT` 태그를 포함하므로 추가 수정 불필요.

#### 3-8. 정렬 변경 시 검색 상태 유지

`toggleSort()`는 `loadHosts()`를 호출하므로 검색 상태가 자동으로 유지됨. 별도 수정 불필요.

---

## 검색 상태별 동작

| 상태 | API 엔드포인트 | 초기화 버튼 | totalCountInfo |
|------|----------------|-------------|----------------|
| 전체 조회 (기본) | `GET /admin/hosts` | 숨김 | `Total: 42개` |
| 이름 검색 중 | `GET /admin/hosts/search/by-name?name=xxx` | 표시 | `Total: 3개` |
| 검색 결과 없음 | `GET /admin/hosts/search/by-name?name=xxx` | 표시 (empty state 내) | `Total: 0개` |

## Spaces와의 차이점

- 호스트 페이지에는 `hasProduct` 같은 필터가 없으므로, 필터와 검색 간 상호 배타 로직(`clearFilterState`, `clearNameSearchState`)은 **생략**한다.
- 검색 상태만 단독으로 관리.

## 구현 시 주의사항

1. **spaces.js 패턴 동일하게 따를 것**
2. **XSS 방지**: API 쿼리 파라미터는 `URLSearchParams`에서 자동 인코딩됨
3. **기존 모달 기능 유지**: 검색 결과 테이블에서도 스페이스 개수 클릭 시 모달이 정상 동작해야 함
4. **ui-ux-pro-max 스킬 필수 사용**: 구현 시 Tailwind 기반 검색 UI의 접근성, 반응형, 인터랙션 품질 검증
5. **접근성**: `autocomplete="off"`, `aria-label="검색"` 필수
6. **44px 터치 타겟**: 모든 버튼 `min-h-[44px]` WCAG 2.1 준수

## 구현 순서

1. **Step 1**: `api.js`에 `searchHostsByName()` 추가 (독립적)
2. **Step 2**: `hosts/list.html`에 검색 UI HTML 추가 (Step 1과 독립적)
3. **Step 3**: `hosts.js`에 검색 로직 추가 (Step 1, 2에 의존)
4. **수동 테스트**: 아래 항목 수행

## 테스트 체크리스트

1. 검색어 입력 후 Enter 키로 검색 실행 확인
2. 검색어 입력 후 돋보기 버튼 클릭으로 검색 실행 확인
3. 검색 결과 테이블이 올바르게 갱신되는지 확인
4. 초기화 버튼 클릭 시 전체 목록 복귀 확인
5. 빈 검색어로 검색 시 전체 목록 표시 확인
6. 검색 상태에서 정렬 변경 시 검색 유지 확인
7. 검색 상태에서 페이지 크기 변경 시 검색 유지 + 1페이지 이동 확인
8. 검색 상태에서 페이지네이션 동작 확인
9. 검색 결과 없을 때 empty state + 초기화 버튼 표시 확인
10. 검색 결과에서 스페이스 개수 클릭 시 모달 정상 동작 확인
