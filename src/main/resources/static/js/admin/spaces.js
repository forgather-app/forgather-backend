/**
 * Spaces Management Page Script
 * Space 목록 조회 및 페이지네이션 처리
 */

// 전역 상태
let currentPage = 1;
let currentPageSize = 15;
let totalPages = 1;
let totalCount = 0;

/**
 * 정렬 상태를 관리하는 객체
 *
 * field: 정렬 기준 필드 (id, name, createdAt, updatedAt)
 * direction: 정렬 방향 (asc, desc)
 *
 * 기본값: 생성시간 내림차순 (최신순)
 */
const sortState = {
    field: 'createdAt',
    direction: 'desc'
};

/**
 * 현재 선택된 필터 상태를 관리하는 객체
 *
 * 필터 조건 확장 가이드:
 * - 새로운 필터를 추가할 때는 이 객체에 프로퍼티를 추가한다.
 * - 예: { hasProduct: true, isPublic: true, hostName: '홍길동' }
 * - 각 프로퍼티의 초기값은 null 또는 적절한 기본값으로 설정
 *
 * hasProduct 필터 값:
 * - null: 전체 (필터 없음)
 * - true: 작품 소개 등록함
 * - false: 작품 소개 등록하지 않음
 */
const filterState = {
    hasProduct: null
};

/**
 * 이름 검색 상태를 관리하는 객체
 *
 * 동작 규칙:
 * - 이름 검색과 필터는 상호 배타적
 * - 이름 검색 활성화 시 필터 초기화
 * - 필터 사용 시 이름 검색 초기화
 *
 * searchName 값:
 * - null 또는 '': 검색 비활성화
 * - 문자열: 해당 이름으로 검색 활성화
 */
const searchState = {
    searchName: null
};

document.addEventListener('DOMContentLoaded', function () {
    // 인증 확인은 서버 인터셉터가 처리 (미인증 시 로그인 페이지로 리다이렉트)

    // DOM 요소 참조
    const spacesTableBody = document.getElementById('spacesTableBody');
    const loadingSpinner = document.getElementById('loadingSpinner');
    const errorMessage = document.getElementById('errorMessage');
    const pageSizeSelect = document.getElementById('pageSize');
    const paginationContainer = document.getElementById('pagination');
    const logoutBtn = document.getElementById('logoutBtn');

    // 테이블 스크롤 래퍼 참조
    const tableScrollWrapper = document.getElementById('tableScrollWrapper');

    /**
     * 테이블 스크롤 상태 업데이트
     * - 스크롤이 가능하면 'has-scroll' 클래스 추가
     * - 끝까지 스크롤하면 'scrolled-end' 클래스 추가
     */
    function updateTableScrollState() {
        if (!tableScrollWrapper) return;

        const { scrollWidth, clientWidth, scrollLeft } = tableScrollWrapper;
        const hasScroll = scrollWidth > clientWidth;
        const scrolledEnd = scrollLeft + clientWidth >= scrollWidth - 5; // 5px 여유

        tableScrollWrapper.classList.toggle('has-scroll', hasScroll && !scrolledEnd);
        tableScrollWrapper.classList.toggle('scrolled-end', scrolledEnd);
    }

    // 스크롤 및 리사이즈 이벤트 리스너 등록
    if (tableScrollWrapper) {
        tableScrollWrapper.addEventListener('scroll', updateTableScrollState);
        window.addEventListener('resize', updateTableScrollState);
    }

    /**
     * 로딩 상태 표시
     */
    function showLoading() {
        loadingSpinner.style.display = 'flex';
        spacesTableBody.innerHTML = '';
        paginationContainer.style.display = 'none';
    }

    /**
     * 로딩 상태 해제
     */
    function hideLoading() {
        loadingSpinner.style.display = 'none';
    }

    /**
     * 에러 메시지 표시
     * @param {string} message - 에러 메시지
     */
    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
        spacesTableBody.innerHTML = '';
        paginationContainer.style.display = 'none';
    }

    /**
     * 에러 메시지 숨김
     */
    function hideError() {
        errorMessage.textContent = '';
        errorMessage.style.display = 'none';
    }

    /**
     * Toast 알림 표시
     * @param {string} message - 알림 메시지
     * @param {string} type - 알림 타입 (success, error, warning)
     */
    function showToast(message, type = 'info') {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.className = `toast ${type}`;
        toast.classList.add('show');

        setTimeout(() => {
            toast.classList.remove('show');
        }, 3000);
    }

    /**
     * 날짜/시간 포맷팅 함수
     *
     * @param {string} dateTimeString - ISO 8601 형식의 날짜/시간 문자열 (예: "2025-01-15T10:30:45")
     * @returns {string} 포맷된 날짜/시간 문자열 (예: "2025.01.15 10:30")
     *
     * 동작:
     * - LocalDateTime을 yyyy.MM.dd HH:mm 형식으로 변환
     * - 입력값이 없거나 유효하지 않으면 "-" 반환 (fallback)
     * - 서버 타임존 그대로 표시 (별도 변환 없음)
     *
     * 사용 예시:
     * formatDateTime("2025-01-15T10:30:45") → "2025.01.15 10:30"
     * formatDateTime(null) → "-"
     */
    function formatDateTime(dateTimeString) {
        if (!dateTimeString) {
            return '-';
        }

        try {
            // LocalDateTime 형식: "2025-01-15T10:30:45"를 파싱
            const date = new Date(dateTimeString);

            // 유효한 날짜인지 확인
            if (isNaN(date.getTime())) {
                return '-';
            }

            // yyyy.MM.dd HH:mm 형식으로 변환
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');

            return `${year}.${month}.${day} ${hours}:${minutes}`;
        } catch (error) {
            console.error('Date formatting error:', error);
            return '-';
        }
    }

    /**
     * Space 테이블 렌더링
     *
     * @param {Array} spaces - Space 목록 (SimpleSpaceResponse[] 형태)
     *
     * SimpleSpaceResponse 구조:
     * - id: 스페이스 ID (숫자)
     * - code: 스페이스 코드 (문자열, 예: "e3f6b97f19")
     * - name: 스페이스 이름 (문자열)
     * - isPublic: 공개 여부 (boolean)
     * - createdAt: 생성 시간 (ISO 8601 문자열)
     * - updatedAt: 수정 시간 (ISO 8601 문자열)
     *
     * 동작:
     * - 데이터가 없으면 Empty State 표시
     * - 각 행을 동적으로 생성하여 tbody에 삽입
     * - XSS 방지를 위해 escapeHtml 적용
     * - 날짜/시간은 formatDateTime 함수로 포맷팅
     */
    function renderSpacesTable(spaces) {
        if (!spaces || spaces.length === 0) {
            spacesTableBody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center py-12">
                        <div class="flex flex-col items-center justify-center text-text-secondary">
                            <svg class="w-16 h-16 mb-4 text-text-muted opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
                            </svg>
                            <h3 class="text-lg font-semibold text-text-primary mb-2">스페이스를 찾을 수 없습니다</h3>
                            <p class="text-sm">표시할 스페이스가 없습니다.</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        const rows = spaces.map(space => {
            // 공개/비공개 상태를 Badge로 표시
            const publicBadge = space.isPublic
                ? '<span class="badge badge-success">공개</span>'
                : '<span class="badge badge-danger">비공개</span>';

            /**
             * data-space-code 속성 추가
             * - 클릭 시 이벤트 위임 패턴으로 spaceCode를 읽어옴
             * - HTML 속성으로 저장하므로 XSS 방지를 위해 escapeHtml 적용
             * - role="button", tabindex="0"으로 키보드 접근성 확보
             * - aria-label로 스크린 리더 지원
             */
            return `
                <tr class="hover:bg-bg-color transition-colors duration-150">
                    <td class="px-lg py-md text-sm text-text-primary">${space.id}</td>
                    <td class="px-lg py-md text-sm clickable-cell" data-space-code="${escapeHtml(space.code)}">
                        <span class="inline-flex items-center gap-2 font-mono text-primary cursor-pointer
                                     px-2 py-1 -mx-2 rounded-md
                                     bg-neutral-50 border border-neutral-200
                                     hover:bg-neutral-100 hover:border-neutral-300
                                     active:bg-neutral-200 active:scale-[0.98]
                                     transition-all duration-150
                                     focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-1"
                              role="button"
                              tabindex="0"
                              aria-label="${escapeHtml(space.code)} 스페이스 상세 보기">
                            <span class="underline decoration-dashed decoration-1 underline-offset-2">
                                ${escapeHtml(space.code)}
                            </span>
                            <svg class="w-4 h-4 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                            </svg>
                        </span>
                    </td>
                    <td class="px-lg py-md text-sm text-text-primary truncate" title="${escapeHtml(space.name)}">${escapeHtml(space.name)}</td>
                    <td class="px-lg py-md text-sm text-center">${publicBadge}</td>
                    <td class="px-lg py-md text-sm text-text-secondary font-mono">${formatDateTime(space.createdAt)}</td>
                    <td class="px-lg py-md text-sm text-text-secondary font-mono">${formatDateTime(space.updatedAt)}</td>
                </tr>
            `;
        }).join('');

        spacesTableBody.innerHTML = rows;

        // 테이블 렌더링 후 스크롤 상태 업데이트
        requestAnimationFrame(updateTableScrollState);
    }

    /**
     * 페이지네이션 UI 업데이트
     *
     * PaginationUtil을 사용하여 숫자 페이지 네비게이션을 렌더링합니다.
     * - 현재 페이지, 전체 페이지, 전체 아이템 개수를 표시
     * - 페이지 클릭 시 goToPage 함수 호출
     */
    function updatePagination() {
        PaginationUtil.render(
            paginationContainer,
            currentPage,
            totalPages,
            totalCount,
            goToPage
        );
    }

    /**
     * 필터 조건 유무를 확인한다
     *
     * @returns {boolean} 필터 조건이 하나라도 있으면 true, 모두 없으면 false
     *
     * 동작:
     * - filterState 객체의 모든 프로퍼티를 확인
     * - null이 아닌 값이 하나라도 있으면 필터가 설정된 것으로 판단
     * - 모든 값이 null이면 필터 없음 (전체 조회)
     *
     * 확장 포인트:
     * - 새로운 필터를 추가해도 이 함수는 수정할 필요 없음
     * - filterState 객체만 업데이트하면 자동으로 처리됨
     */
    function hasActiveFilters() {
        return Object.values(filterState).some(value => value !== null);
    }

    /**
     * Space 목록 로드
     *
     * 호출 시점:
     * - 페이지 최초 로드 (DOMContentLoaded)
     * - 통합 검색 버튼 클릭 (handleSearch)
     * - 페이지 크기 변경 (handlePageSizeChange)
     * - 페이지네이션 버튼 클릭 (goToPage)
     * - 정렬 헤더 클릭 (toggleSort)
     *
     * API 엔드포인트 분기 규칙 (우선순위):
     * 1. 이름 검색 활성화 → /admin/spaces/search/by-name
     * 2. 필터 조건 있음 → /admin/spaces/search
     * 3. 전체 조회 → /admin/spaces
     *
     * 정렬 파라미터:
     * - Spring Data Pageable의 sort 파라미터 사용
     * - 형식: ?sort=field,direction (예: ?sort=createdAt,desc)
     */
    async function loadSpaces() {
        hideError();
        showLoading();

        try {
            let response;
            const sortParam = `${sortState.field},${sortState.direction}`;

            // API 호출 분기: 이름 검색 > 필터 > 전체
            if (hasActiveNameSearch()) {
                // 이름 검색 API 호출 (/admin/spaces/search/by-name)
                response = await API.searchSpacesByName(searchState.searchName, currentPage, currentPageSize, sortParam);
            } else if (hasActiveFilters()) {
                // 필터링 API 호출 (/admin/spaces/search)
                response = await API.getSpacesByFilters(currentPage, currentPageSize, filterState, sortParam);
            } else {
                // 전체 조회 API 호출 (/admin/spaces)
                response = await API.getSpaces(currentPage, currentPageSize, sortParam);
            }

            // 상태 업데이트
            currentPage = response.currentPage;
            currentPageSize = response.pageSize;
            totalPages = response.totalPages;
            totalCount = response.totalCount;

            // 테이블 렌더링
            renderSpacesTable(response.spaces);

            // 페이지네이션 업데이트
            updatePagination();

            // 총 개수 정보 업데이트
            updateTotalCountInfo();

            // 정렬 아이콘 업데이트
            updateSortIcons();

        } catch (error) {
            console.error('Failed to load spaces:', error);
            showError(error.message || '스페이스 목록을 불러오는데 실패했습니다. 다시 시도해주세요.');
        } finally {
            hideLoading();
        }
    }

    /**
     * 총 개수 정보 업데이트
     */
    function updateTotalCountInfo() {
        const totalCountInfo = document.getElementById('totalCountInfo');
        if (totalCountInfo) {
            totalCountInfo.textContent = `Total: ${totalCount.toLocaleString()}개`;
        }
    }

    /**
     * 정렬 아이콘 업데이트
     * - 현재 정렬 필드에 맞는 아이콘(▲/▼) 표시
     * - 비활성 필드는 hover 시 ⇅ 아이콘 표시 (정렬 가능 힌트)
     */
    function updateSortIcons() {
        const sortableHeaders = document.querySelectorAll('th[data-sort]');
        sortableHeaders.forEach(header => {
            const field = header.getAttribute('data-sort');
            const iconSpan = header.querySelector('.sort-icon');
            if (iconSpan) {
                if (field === sortState.field) {
                    // 활성 정렬 컬럼: 방향 아이콘 항상 표시
                    iconSpan.textContent = sortState.direction === 'asc' ? '▲' : '▼';
                    iconSpan.classList.remove('text-text-muted', 'opacity-0', 'group-hover:opacity-100');
                    iconSpan.classList.add('text-primary');
                } else {
                    // 비활성 컬럼: hover 시에만 정렬 가능 힌트 표시
                    iconSpan.textContent = '⇅';
                    iconSpan.classList.remove('text-primary');
                    iconSpan.classList.add('text-text-muted', 'opacity-0', 'group-hover:opacity-100');
                }
            }
        });
    }

    /**
     * 정렬 토글
     * - 같은 필드 클릭 시 방향 토글 (asc ↔ desc)
     * - 다른 필드 클릭 시 해당 필드로 변경, 기본 방향은 asc
     *
     * @param {string} field - 정렬 기준 필드
     */
    function toggleSort(field) {
        if (sortState.field === field) {
            // 같은 필드: 방향 토글
            sortState.direction = sortState.direction === 'asc' ? 'desc' : 'asc';
        } else {
            // 다른 필드: 새 필드로 변경, 기본 오름차순
            sortState.field = field;
            sortState.direction = 'asc';
        }
        currentPage = 1; // 정렬 변경 시 첫 페이지로
        loadSpaces();
    }

    /**
     * 이름 검색 활성화 여부 확인
     *
     * @returns {boolean} 이름 검색이 활성화되어 있으면 true
     */
    function hasActiveNameSearch() {
        return searchState.searchName !== null && searchState.searchName.trim() !== '';
    }

    /**
     * 페이지 이동
     *
     * @param {number} page - 이동할 페이지 번호
     *
     * 동작:
     * - 유효한 페이지 범위인지 확인 (1 ~ totalPages)
     * - 유효하면 currentPage를 업데이트하고 데이터 재로드
     * - PaginationUtil에서 페이지 버튼 클릭 시 이 함수가 호출됨
     */
    function goToPage(page) {
        if (page < 1 || page > totalPages) {
            return;
        }
        currentPage = page;
        loadSpaces();
    }

    /**
     * HTML 이스케이프 (XSS 방지)
     * @param {string} text - 이스케이프할 텍스트
     * @returns {string} 이스케이프된 텍스트
     */
    function escapeHtml(text) {
        const map = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        };
        return text.toString().replace(/[&<>"']/g, m => map[m]);
    }

    /**
     * 페이지 크기 변경 핸들러
     */
    function handlePageSizeChange() {
        currentPageSize = parseInt(pageSizeSelect.value);
        currentPage = 1; // 첫 페이지로 리셋
        loadSpaces();
    }

    /**
     * 로그아웃 핸들러
     */
    function handleLogout() {
        if (confirm('로그아웃 하시겠습니까?')) {
            Auth.logout();
        }
    }

    /**
     * 필터 적용 함수
     *
     * 호출 시점:
     * - 사용자가 필터를 선택하고 [검색] 버튼을 클릭했을 때
     *
     * 동작 흐름:
     * 1. 이름 검색 상태 초기화 (필터와 이름 검색은 상호 배타적)
     * 2. HTML에서 선택된 radio 버튼의 value를 읽어온다
     * 3. value를 적절한 타입으로 변환 ('all' → null, 'true' → true, 'false' → false)
     * 4. filterState 객체에 저장
     * 5. 페이지를 1페이지로 초기화 (필터 변경 시 항상 첫 페이지부터 시작)
     * 6. loadSpaces() 호출하여 목록 갱신
     *
     * 확장 포인트:
     * - 새로운 필터를 추가할 때 이 함수에서 해당 필터 값을 읽어서 filterState에 추가
     * - 예시:
     *   ```javascript
     *   const isPublic = document.querySelector('input[name="isPublic"]:checked').value;
     *   filterState.isPublic = isPublic === 'all' ? null : isPublic === 'true';
     *   ```
     */
    function applyFilter() {
        // 필터와 이름 검색은 상호 배타적 - 이름 검색 초기화
        clearNameSearchState();

        // Optional Chaining (?.)으로 안전하게 접근 후, Nullish Coalescing (??)으로 기본값 설정
        const hasProductValue = document.querySelector('input[name="hasProduct"]:checked')?.value ?? 'all';

        // 매핑 로직 개선 (객체 리터럴 활용으로 if-else 제거)
        const filterMap = {
            'all': null,
            'true': true,
            'false': false
        };
        filterState.hasProduct = filterMap[hasProductValue];

        // 필터 변경 시 페이지를 1페이지로 초기화
        currentPage = 1;

        // 목록 갱신
        loadSpaces();
    }

    /**
     * 이름 검색 실행
     *
     * 호출 시점:
     * - 검색 버튼 클릭 또는 Enter 키 입력
     *
     * 동작 흐름:
     * 1. 검색어 읽기
     * 2. 필터 상태 초기화 (이름 검색과 필터는 상호 배타적)
     * 3. 검색 상태 업데이트
     * 4. UI 업데이트 (초기화 버튼 표시)
     * 5. 목록 갱신
     */
    function searchByName() {
        const nameInput = document.getElementById('nameSearchInput');
        const searchName = nameInput.value.trim();

        // 빈 검색어일 경우 전체 조회로 전환
        if (!searchName) {
            resetNameSearch();
            return;
        }

        // 필터와 이름 검색은 상호 배타적 - 필터 초기화
        clearFilterState();

        // 검색 상태 업데이트
        searchState.searchName = searchName;

        // UI 업데이트
        updateNameSearchUI();

        // 페이지 초기화 및 목록 갱신
        currentPage = 1;
        loadSpaces();
    }

    /**
     * 이름 검색 초기화
     *
     * 호출 시점:
     * - 초기화 버튼 클릭
     *
     * 동작:
     * - 검색어 입력 필드 비우기
     * - 검색 상태 초기화
     * - 전체 목록 조회
     */
    function resetNameSearch() {
        const nameInput = document.getElementById('nameSearchInput');
        nameInput.value = '';

        searchState.searchName = null;

        // UI 업데이트
        updateNameSearchUI();

        // 페이지 초기화 및 목록 갱신
        currentPage = 1;
        loadSpaces();
    }

    /**
     * 이름 검색 상태만 초기화 (UI 갱신 포함)
     * - 필터 적용 시 호출됨
     */
    function clearNameSearchState() {
        const nameInput = document.getElementById('nameSearchInput');
        if (nameInput) {
            nameInput.value = '';
        }
        searchState.searchName = null;
        updateNameSearchUI();
    }

    /**
     * 필터 상태만 초기화 (UI 갱신 포함)
     * - 이름 검색 시 호출됨
     */
    function clearFilterState() {
        // 필터 상태 초기화
        filterState.hasProduct = null;

        // 라디오 버튼 UI 초기화 ('전체' 선택)
        const allRadio = document.querySelector('input[name="hasProduct"][value="all"]');
        if (allRadio) {
            allRadio.checked = true;
        }
    }

    /**
     * 이름 검색 UI 업데이트
     * - 검색 중: 초기화 버튼 표시
     * - 검색 없음: 초기화 버튼 숨김
     */
    function updateNameSearchUI() {
        const resetBtn = document.getElementById('resetSearchBtn');
        if (resetBtn) {
            resetBtn.style.display = hasActiveNameSearch() ? 'inline-flex' : 'none';
        }
    }

    // 이벤트 리스너 등록
    pageSizeSelect.addEventListener('change', handlePageSizeChange);
    logoutBtn.addEventListener('click', handleLogout);

    /**
     * 필터 적용 버튼 클릭 이벤트 리스너
     * - 이름 검색 초기화 후 필터 적용
     */
    const applyFilterBtn = document.getElementById('applyFilterBtn');
    if (applyFilterBtn) {
        applyFilterBtn.addEventListener('click', applyFilter);
    }

    /**
     * 이름 검색 버튼 클릭 이벤트 리스너
     */
    const nameSearchBtn = document.getElementById('nameSearchBtn');
    if (nameSearchBtn) {
        nameSearchBtn.addEventListener('click', searchByName);
    }

    /**
     * 이름 검색 초기화 버튼 클릭 이벤트 리스너
     */
    const resetSearchBtn = document.getElementById('resetSearchBtn');
    if (resetSearchBtn) {
        resetSearchBtn.addEventListener('click', resetNameSearch);
    }

    /**
     * 이름 검색 입력 필드 Enter 키 이벤트 리스너
     * - Enter 키 입력 시 검색 실행
     */
    const nameSearchInput = document.getElementById('nameSearchInput');
    if (nameSearchInput) {
        nameSearchInput.addEventListener('keydown', function(event) {
            if (event.key === 'Enter') {
                event.preventDefault();
                searchByName();
            }
        });
    }

    /**
     * 테이블 헤더 정렬 클릭 이벤트 리스너 (이벤트 위임)
     */
    const spacesTable = document.getElementById('spacesTable');
    if (spacesTable) {
        const thead = spacesTable.querySelector('thead');
        if (thead) {
            thead.addEventListener('click', function(event) {
                const th = event.target.closest('th[data-sort]');
                if (th) {
                    const field = th.getAttribute('data-sort');
                    if (field) {
                        toggleSort(field);
                    }
                }
            });
        }
    }

    /**
     * 키보드 네비게이션
     * - 좌측 화살표: 이전 페이지
     * - 우측 화살표: 다음 페이지
     *
     * 접근성 향상을 위한 기능
     */
    document.addEventListener('keydown', function (event) {
        // 좌측 화살표: 이전 페이지
        if (event.key === 'ArrowLeft' && currentPage > 1) {
            event.preventDefault();
            goToPage(currentPage - 1);
        }
        // 우측 화살표: 다음 페이지
        else if (event.key === 'ArrowRight' && currentPage < totalPages) {
            event.preventDefault();
            goToPage(currentPage + 1);
        }
    });

    // ==========================================================================
    // Modal Logic - Space Detail
    // ==========================================================================

    /**
     * 모달 관련 DOM 요소 참조
     * - 한 번만 querySelector로 찾아서 재사용 (성능 최적화)
     */
    const modal = document.getElementById('spaceDetailModal');
    const modalLoading = document.getElementById('modalLoading');
    const modalError = document.getElementById('modalError');
    const modalContent = document.getElementById('modalContent');
    const closeModalBtn = document.getElementById('closeModalBtn');
    const visitSpaceBtn = document.getElementById('visitSpaceBtn');

    // 모달 내 데이터 표시 요소들
    const modalSpaceId = document.getElementById('modalSpaceId');
    const modalSpaceCode = document.getElementById('modalSpaceCode');
    const modalSpaceName = document.getElementById('modalSpaceName');
    const modalSpacePublic = document.getElementById('modalSpacePublic');
    const modalHasProduct = document.getElementById('modalHasProduct');
    const modalGuestBookCount = document.getElementById('modalGuestBookCount');

    /**
     * 현재 열려있는 스페이스 코드를 저장
     * - Visit Space Page 버튼 클릭 시 사용
     * - null이면 모달이 닫혀있음을 의미
     */
    let currentOpenSpaceCode = null;

    /**
     * 모달 열기
     * - CSS의 .show 클래스를 추가하여 페이드 인 애니메이션 실행
     * - body에 modal-open 클래스를 추가하여 배경 스크롤 방지
     */
    function openModal() {
        modal.style.display = 'flex';
        // 다음 프레임에서 show 클래스 추가 (CSS 애니메이션을 위함)
        requestAnimationFrame(() => {
            modal.classList.add('show');
        });
        document.body.classList.add('modal-open');
    }

    /**
     * 모달 닫기
     * - CSS의 .show 클래스를 제거하여 페이드 아웃 애니메이션 실행
     * - 애니메이션 완료 후 display: none 처리
     * - body의 modal-open 클래스 제거하여 배경 스크롤 복원
     */
    function closeModal() {
        modal.classList.remove('show');
        document.body.classList.remove('modal-open');
        currentOpenSpaceCode = null;

        // 애니메이션 완료 후 display: none (300ms는 CSS transition과 동일)
        setTimeout(() => {
            modal.style.display = 'none';
            // 모달 내용 초기화
            resetModalContent();
        }, 300);
    }

    /**
     * 모달 내용 초기화
     * - 다음에 모달을 열 때 이전 데이터가 보이지 않도록 숨김 처리
     */
    function resetModalContent() {
        modalLoading.style.display = 'none';
        modalError.style.display = 'none';
        modalContent.style.display = 'none';
        visitSpaceBtn.style.display = 'none';
    }

    /**
     * 모달에 로딩 상태 표시
     */
    function showModalLoading() {
        resetModalContent();
        modalLoading.style.display = 'flex';
    }

    /**
     * 모달에 에러 메시지 표시
     * @param {string} message - 표시할 에러 메시지
     */
    function showModalError(message) {
        resetModalContent();
        modalError.textContent = message;
        modalError.style.display = 'block';
    }

    /**
     * 모달에 스페이스 상세 정보 표시
     *
     * API: GET /admin/spaces/{spaceCode}
     * @param {object} data - API 응답 데이터 (SpaceDetailResponse)
     * @param {object} data.space - 스페이스 기본 정보 (SimpleSpaceResponse)
     * @param {number} data.productCount - 등록한 작품 소개 개수
     * @param {number} data.guestBookCount - 방명록 개수
     */
    function showModalContent(data) {
        resetModalContent();

        // 스페이스 기본 정보 표시
        modalSpaceId.textContent = data.space.id;
        modalSpaceCode.textContent = data.space.code;
        modalSpaceName.textContent = data.space.name;

        // 공개/비공개 상태를 Badge로 표시
        const publicBadge = data.space.isPublic
            ? '<span class="badge badge-success">공개</span>'
            : '<span class="badge badge-danger">비공개</span>';
        modalSpacePublic.innerHTML = publicBadge;

        /**
         * 작품 소개 등록 상태 표시
         * - productCount가 0: "작품 소개를 등록하지 않음" (회색 텍스트)
         * - productCount가 1 이상: "등록한 작품 소개 N개" (녹색 텍스트, 숫자 포맷팅 적용)
         */
        const productStatusText = data.productCount === 0
            ? '<span style="color: var(--text-muted);">작품 소개를 등록하지 않음</span>'
            : `<span style="color: var(--success-color); font-weight: 600;">${data.productCount.toLocaleString()}개</span>`;
        modalHasProduct.innerHTML = productStatusText;

        // 방명록 개수 표시 (숫자 포맷팅)
        modalGuestBookCount.textContent = data.guestBookCount.toLocaleString();

        // 콘텐츠와 버튼 표시
        modalContent.style.display = 'block';
        visitSpaceBtn.style.display = 'block';
    }

    /**
     * 스페이스 상세 정보 로드
     *
     * @param {string} spaceCode - 조회할 스페이스 코드
     *
     * 동작 흐름:
     * 1. 모달 열기 및 로딩 표시
     * 2. API 호출
     * 3. 성공 시 데이터 표시, 실패 시 에러 표시
     *
     * 주의:
     * - 비동기 함수이므로 await 필요
     * - API 에러는 try-catch로 처리하여 사용자에게 친절한 메시지 표시
     */
    async function loadSpaceDetail(spaceCode) {
        currentOpenSpaceCode = spaceCode;
        openModal();
        showModalLoading();

        try {
            const data = await API.getSpaceDetail(spaceCode);
            showModalContent(data);
        } catch (error) {
            console.error('Failed to load space detail:', error);
            showModalError(error.message || '스페이스 정보를 불러오는데 실패했습니다. 다시 시도해주세요.');
        }
    }

    /**
     * 게스트 페이지 방문 URL 생성
     *
     * @param {string} spaceCode - 스페이스 코드
     * @returns {string} 게스트 페이지 URL
     *
     * 환경별 도메인 매핑:
     * - API 호출 도메인 (현재 페이지 hostname 기준)
     *   - localhost:8080 → 로컬 환경
     *   - api.dev.forgather.app → 개발 환경
     *   - api.forgather.app → 운영 환경
     *
     * - 게스트 페이지 도메인 (api. 접두사 제거)
     *   - 로컬 환경 → dev.forgather.app (개발 환경과 동일)
     *   - 개발 환경 → dev.forgather.app
     *   - 운영 환경 → forgather.app
     *
     * URL 형식: https://{guestDomain}/guest/{spaceCode}/home
     *
     * 주의:
     * - 백오피스는 항상 api. 서브도메인에서 실행됨
     * - 게스트 페이지는 api. 없이 메인 도메인에서 실행됨
     * - 로컬 개발 시에는 개발 환경으로 리다이렉트
     */
    function getSpacePageUrl(spaceCode) {
        const hostname = window.location.hostname;

        let guestDomain;

        // 환경별 게스트 페이지 도메인 결정
        if (hostname === 'api.forgather.app') {
            // 운영 환경: api.forgather.app → forgather.app
            guestDomain = 'forgather.app';
        } else if (hostname === 'api.dev.forgather.app') {
            // 개발 환경: api.dev.forgather.app → dev.forgather.app
            guestDomain = 'dev.forgather.app';
        } else {
            // 로컬 환경 (localhost 등): 개발 환경으로 고정
            guestDomain = 'dev.forgather.app';
        }

        return `https://${guestDomain}/guest/${spaceCode}/home`;
    }

    /**
     * Code 컬럼 클릭/키보드 이벤트 공통 핸들러
     *
     * @param {Event} event - 클릭 또는 키보드 이벤트
     *
     * 동작 과정:
     * 1. closest()로 data-space-code 속성을 가진 td를 찾음
     * 2. Code 컬럼이면 data-space-code 속성에서 spaceCode를 읽어옴
     * 3. loadSpaceDetail 함수를 호출하여 모달 표시
     */
    function handleSpaceCodeInteraction(event) {
        const td = event.target.closest('td[data-space-code]');
        if (!td) {
            return;
        }

        const spaceCode = td.getAttribute('data-space-code');
        if (spaceCode) {
            loadSpaceDetail(spaceCode);
        }
    }

    /**
     * Code 컬럼 클릭 이벤트 핸들러 (이벤트 위임 패턴)
     *
     * 이벤트 위임 패턴을 사용하는 이유:
     * - 테이블 행이 동적으로 생성되므로 각 행에 개별 이벤트 리스너를 달면 메모리 낭비
     * - tbody에 한 번만 이벤트 리스너를 등록하고, 클릭된 요소가 Code 컬럼인지 확인
     * - 페이징으로 DOM이 재생성되어도 이벤트 리스너를 다시 등록할 필요 없음
     */
    spacesTableBody.addEventListener('click', function (event) {
        handleSpaceCodeInteraction(event);
    });

    /**
     * Code 컬럼 키보드 이벤트 핸들러 (접근성)
     *
     * 키보드 접근성 향상:
     * - Enter 또는 Space 키로 클릭과 동일한 동작 실행
     * - role="button"과 tabindex="0"이 설정된 요소에서 동작
     * - 스크린 리더 사용자도 키보드로 모달을 열 수 있음
     */
    spacesTableBody.addEventListener('keydown', function (event) {
        if (event.key === 'Enter' || event.key === ' ') {
            const td = event.target.closest('td[data-space-code]');
            if (td) {
                event.preventDefault(); // Space 키의 스크롤 방지
                handleSpaceCodeInteraction(event);
            }
        }
    });

    /**
     * 게스트 페이지로 이동 버튼 클릭 핸들러
     *
     * 동작:
     * - 현재 모달에 표시된 스페이스의 게스트 페이지를 새 탭에서 열기
     * - window.open의 두 번째 인자 '_blank'로 새 탭 열기
     * - noopener, noreferrer 옵션으로 보안 강화
     *   (새 탭이 원본 페이지에 접근하지 못하도록 window.opener 차단)
     *
     * 참고:
     * - currentOpenSpaceCode는 모달을 열 때 설정됨 (loadSpaceDetail 함수)
     * - URL 생성은 getSpacePageUrl 함수가 담당 (환경별 도메인 처리)
     */
    visitSpaceBtn.addEventListener('click', function () {
        if (currentOpenSpaceCode) {
            const url = getSpacePageUrl(currentOpenSpaceCode);
            window.open(url, '_blank', 'noopener,noreferrer');
        }
    });

    /**
     * 모달 닫기 버튼 클릭 핸들러 (헤더 X 버튼)
     */
    closeModalBtn.addEventListener('click', function () {
        closeModal();
    });

    /**
     * 모달 닫기 버튼 클릭 핸들러 (푸터 닫기 버튼)
     */
    const closeModalFooterBtn = document.getElementById('closeModalFooterBtn');
    if (closeModalFooterBtn) {
        closeModalFooterBtn.addEventListener('click', function () {
            closeModal();
        });
    }

    /**
     * 모달 오버레이 클릭 시 닫기
     * - 모달 배경(검은색 반투명 영역)을 클릭하면 모달 닫기
     * - 모달 내용 영역(.modal-container)을 클릭하면 닫히지 않도록 처리
     *
     * event.target vs event.currentTarget:
     * - event.target: 실제 클릭된 요소 (모달 내용 또는 오버레이)
     * - event.currentTarget: 이벤트 리스너가 등록된 요소 (항상 modal)
     * - 둘이 같다는 것은 오버레이를 직접 클릭했다는 의미
     */
    modal.addEventListener('click', function (event) {
        if (event.target === modal) {
            closeModal();
        }
    });

    /**
     * ESC 키로 모달 닫기
     * - 키보드 접근성 향상
     * - 모달이 열려있을 때만 동작 (modal.classList.contains('show'))
     */
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && modal.classList.contains('show')) {
            closeModal();
        }
    });

    // 초기 로드
    loadSpaces();
});
