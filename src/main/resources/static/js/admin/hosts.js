/**
 * Hosts Management Page Script
 * Host 목록 조회 및 페이지네이션 처리
 *
 * 주요 기능:
 * 1. 호스트 목록 API 호출 및 테이블 렌더링
 * 2. 페이지네이션 (이전/다음 버튼, 페이지 정보 표시)
 * 3. 페이지 크기 변경 (15/30/50개)
 * 4. 날짜 포맷팅 (ISO 8601 -> YYYY-MM-DD HH:mm)
 * 5. 로딩/에러 상태 관리
 */

// ==========================================================================
// 전역 상태 관리
// ==========================================================================

/**
 * 현재 페이지 번호 (1부터 시작)
 * - API 응답의 currentPage 값과 동기화
 */
let currentPage = 1;

/**
 * 현재 페이지 크기 (한 페이지에 표시할 아이템 개수)
 * - 사용자가 select box에서 변경 가능
 * - 기본값: 15
 */
let currentPageSize = 15;

/**
 * 전체 페이지 수
 * - API 응답의 totalPages 값으로 업데이트
 * - 다음 버튼 활성화/비활성화에 사용
 */
let totalPages = 1;

/**
 * 전체 아이템 개수
 * - API 응답의 totalCount 값으로 업데이트
 * - 사용자에게 전체 데이터 규모를 알림
 */
let totalCount = 0;

/**
 * 정렬 상태를 관리하는 객체
 *
 * field: 정렬 기준 필드 (id, name, createdAt)
 * direction: 정렬 방향 (asc, desc)
 *
 * 기본값: 생성시간 내림차순 (최신순)
 */
const sortState = {
    field: 'createdAt',
    direction: 'desc'
};

// ==========================================================================
// 페이지 초기화
// ==========================================================================

document.addEventListener('DOMContentLoaded', function() {
    // ======================================================================
    // 인증 확인
    // ======================================================================

    /**
     * 인증 확인은 서버 인터셉터가 처리합니다.
     * - 세션이 없거나 만료된 경우 로그인 페이지로 자동 리다이렉트
     * - httpOnly 쿠키는 JS에서 접근 불가하므로 서버에서 검증
     */

    // ======================================================================
    // DOM 요소 참조
    // ======================================================================

    /**
     * DOM 요소들을 변수에 저장하여 재사용합니다.
     * - getElementById는 매번 호출하면 성능 저하
     * - 한 번만 조회하여 변수에 캐싱
     */
    const hostsTableBody = document.getElementById('hostsTableBody');
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

    // ======================================================================
    // UI 상태 관리 함수
    // ======================================================================

    /**
     * 스켈레톤 로딩 UI 렌더링
     * - 실제 데이터 행과 유사한 형태로 표시
     * - CLS(Cumulative Layout Shift) 최소화
     *
     * @param {number} rowCount - 표시할 스켈레톤 행 개수 (기본값: 5)
     */
    function renderSkeletonLoading(rowCount = 5) {
        const skeletonRows = Array(rowCount).fill(null).map(() => `
            <tr class="animate-pulse">
                <td class="px-6 py-4">
                    <div class="h-4 bg-neutral-200 rounded w-8"></div>
                </td>
                <td class="px-6 py-4">
                    <div class="h-4 bg-neutral-200 rounded w-32"></div>
                </td>
                <td class="px-6 py-4 text-center">
                    <div class="h-4 bg-neutral-200 rounded w-8 mx-auto"></div>
                </td>
                <td class="px-6 py-4">
                    <div class="h-4 bg-neutral-200 rounded w-28"></div>
                </td>
            </tr>
        `).join('');

        hostsTableBody.innerHTML = skeletonRows;
    }

    /**
     * 로딩 상태 표시
     * - 스피너 대신 스켈레톤 UI로 데이터 구조 예측 가능
     * - CLS(Cumulative Layout Shift) 최소화
     * - API 호출 시작 시 호출됩니다.
     */
    function showLoading() {
        loadingSpinner.style.display = 'none';
        renderSkeletonLoading(currentPageSize > 15 ? 10 : 5);
        paginationContainer.style.display = 'none';
    }

    /**
     * 로딩 상태 해제
     * - 로딩 스피너를 숨깁니다.
     * - API 호출 완료 시 호출됩니다.
     */
    function hideLoading() {
        loadingSpinner.style.display = 'none';
    }

    /**
     * 에러 메시지 표시
     * @param {string} message - 표시할 에러 메시지
     *
     * - 사용자 친화적인 에러 메시지를 표시합니다.
     * - 테이블과 페이지네이션을 숨깁니다.
     * - 네트워크 에러, API 에러 등을 사용자에게 알립니다.
     */
    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
        hostsTableBody.innerHTML = '';
        paginationContainer.style.display = 'none';
    }

    /**
     * 에러 메시지 숨김
     * - 새로운 데이터 로드 시 이전 에러 메시지를 제거합니다.
     */
    function hideError() {
        errorMessage.textContent = '';
        errorMessage.style.display = 'none';
    }

    /**
     * Toast 알림 표시
     * @param {string} message - 알림 메시지
     * @param {string} type - 알림 타입 (success, error, warning, info)
     *
     * - 화면 우측 상단에 짧은 알림을 표시합니다.
     * - 3초 후 자동으로 사라집니다.
     * - 로그아웃 성공, 에러 발생 등의 피드백에 사용됩니다.
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

    // ======================================================================
    // 날짜 포맷팅 함수
    // ======================================================================

    /**
     * ISO 8601 날짜 문자열을 YYYY.MM.DD HH:mm 형식으로 변환
     * @param {string} dateString - ISO 8601 형식의 날짜 문자열 (예: "2025-11-14T10:30:00")
     * @returns {string} 포맷팅된 날짜 문자열 (예: "2025.11.14 10:30")
     *
     * 동작 과정:
     * 1. Date 객체로 파싱
     * 2. 각 필드를 2자리로 패딩 (padStart 사용)
     * 3. YYYY.MM.DD HH:mm 형식으로 조합
     *
     * 주의사항:
     * - 타임존 변환은 하지 않음 (서버와 클라이언트가 동일한 타임존 가정)
     * - 잘못된 날짜 문자열이 들어오면 "-" 반환
     */
    function formatDateTime(dateString) {
        if (!dateString) {
            return '-';
        }

        try {
            const date = new Date(dateString);

            // Invalid Date 체크
            if (isNaN(date.getTime())) {
                return '-';
            }

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

    // ======================================================================
    // 테이블 렌더링 함수
    // ======================================================================

    /**
     * Host 테이블 렌더링
     * @param {Array} hosts - Host 목록 배열
     *
     * 테이블 컬럼 구성:
     * 1. ID: host.id (숫자)
     * 2. 호스트명: host.name (문자열, XSS 방지를 위해 escapeHtml 적용)
     * 3. 스페이스 개수: host.spaceCount (서버에서 COUNT 쿼리로 계산된 값)
     * 4. 생성일: host.createdAt (formatDateTime으로 포맷팅)
     *
     * 빈 상태 처리:
     * - hosts가 비어있으면 "No Hosts Found" 메시지 표시
     * - 사용자가 데이터가 없음을 명확히 알 수 있도록 아이콘과 메시지 제공
     */
    function renderHostsTable(hosts) {
        // 데이터가 없는 경우
        if (!hosts || hosts.length === 0) {
            hostsTableBody.innerHTML = `
                <tr>
                    <td colspan="4" class="text-center py-12">
                        <div class="flex flex-col items-center justify-center text-text-secondary">
                            <svg class="w-16 h-16 mb-4 text-text-muted opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
                            </svg>
                            <h3 class="text-lg font-semibold text-text-primary mb-2">호스트를 찾을 수 없습니다</h3>
                            <p class="text-sm">표시할 호스트가 없습니다.</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        /**
         * 각 호스트를 테이블 행(tr)으로 변환
         * - map으로 배열을 순회하며 HTML 문자열 생성
         * - join('')으로 하나의 문자열로 합침
         * - innerHTML에 할당하여 DOM에 렌더링
         */
        const rows = hosts.map(host => {
            /**
             * 스페이스 개수
             * - 서버에서 COUNT 쿼리로 계산된 값을 직접 사용
             * - 방어적 코드: spaceCount가 없는 경우 0으로 표시
             */
            const spaceCount = host.spaceCount || 0;

            /**
             * HTML 템플릿 리터럴로 테이블 행 생성
             * - XSS 방지: escapeHtml 함수로 사용자 입력값 이스케이프
             * - 날짜 포맷팅: formatDateTime 함수 사용
             */
            /**
             * 스페이스 개수 셀 스타일 분기
             * - 0개: 회색 비활성 텍스트, 클릭 불가
             * - 1개 이상: primary 색상, 클릭 가능 (모달 열기)
             */
            const spaceCountCell = spaceCount > 0
                ? `<td class="px-6 py-4 text-sm text-primary font-semibold text-center cursor-pointer hover:underline transition-colors duration-150"
                       data-host-id="${host.id}" data-host-name="${escapeHtml(host.name)}"
                       role="button" tabindex="0">${spaceCount}</td>`
                : `<td class="px-6 py-4 text-sm text-text-muted text-center">${spaceCount}</td>`;

            return `
                <tr class="hover:bg-bg-color transition-colors duration-150">
                    <td class="px-6 py-4 text-sm text-text-primary">${host.id}</td>
                    <td class="px-6 py-4 text-sm text-text-primary font-medium truncate max-w-0" title="${escapeHtml(host.name)}">${escapeHtml(host.name)}</td>
                    ${spaceCountCell}
                    <td class="px-6 py-4 text-sm text-text-secondary">${formatDateTime(host.createdAt)}</td>
                </tr>
            `;
        }).join('');

        hostsTableBody.innerHTML = rows;

        // 테이블 렌더링 후 스크롤 상태 업데이트
        requestAnimationFrame(updateTableScrollState);
    }

    // ======================================================================
    // 페이지네이션 함수
    // ======================================================================

    /**
     * 페이지네이션 UI 업데이트
     *
     * PaginationUtil을 사용하여 숫자 페이지 네비게이션을 렌더링합니다.
     * - 현재 페이지, 전체 페이지, 전체 아이템 개수를 표시
     * - 페이지 클릭 시 goToPage 함수 호출
     * - « ‹ 1 2 3 4 5 › » 형태의 버튼 렌더링
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

    // ======================================================================
    // API 호출 함수
    // ======================================================================

    /**
     * Host 목록 로드
     *
     * 동작 흐름:
     * 1. 에러 메시지 숨김 (이전 에러 제거)
     * 2. 로딩 스피너 표시
     * 3. API 호출 (api.js의 API.getHosts 사용)
     * 4. 성공 시:
     *    - 전역 상태 업데이트 (currentPage, totalPages, totalCount 등)
     *    - 테이블 렌더링
     *    - 페이지네이션 업데이트
     *    - 총 개수 정보 업데이트
     *    - 정렬 아이콘 업데이트
     * 5. 실패 시:
     *    - 에러 메시지 표시
     *    - 콘솔에 에러 로그 출력
     * 6. 완료 시 (성공/실패 무관):
     *    - 로딩 스피너 숨김
     *
     * 에러 처리:
     * - API.getHosts는 401 에러를 자동으로 처리 (Token Refresh)
     * - 네트워크 에러, 서버 에러 등은 catch에서 처리
     * - 사용자 친화적인 메시지 표시
     */
    async function loadHosts() {
        hideError();
        showLoading();

        try {
            const sortParam = `${sortState.field},${sortState.direction}`;
            const response = await API.getHosts(currentPage, currentPageSize, sortParam);

            // API 응답 데이터로 전역 상태 업데이트
            currentPage = response.currentPage;
            currentPageSize = response.pageSize;
            totalPages = response.totalPages;
            totalCount = response.totalCount;

            // 테이블 렌더링
            renderHostsTable(response.hosts);

            // 페이지네이션 업데이트
            updatePagination();

            // 총 개수 정보 업데이트
            updateTotalCountInfo();

            // 정렬 아이콘 업데이트
            updateSortIcons();

        } catch (error) {
            console.error('Failed to load hosts:', error);
            showError(error.message || '호스트 목록을 불러오는데 실패했습니다. 다시 시도해주세요.');
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
        loadHosts();
    }

    /**
     * 페이지 이동
     *
     * @param {number} page - 이동할 페이지 번호
     *
     * 동작:
     * - 유효성 검사: page가 1 ~ totalPages 범위 내인지 확인
     * - 범위를 벗어나면 무시 (아무 동작도 하지 않음)
     * - 유효한 경우 currentPage를 업데이트하고 데이터 재로드
     * - PaginationUtil에서 페이지 버튼 클릭 시 이 함수가 호출됨
     */
    function goToPage(page) {
        if (page < 1 || page > totalPages) {
            return;
        }
        currentPage = page;
        loadHosts();
    }

    // ======================================================================
    // 유틸리티 함수
    // ======================================================================

    /**
     * HTML 이스케이프 (XSS 방지)
     * @param {string} text - 이스케이프할 텍스트
     * @returns {string} 이스케이프된 텍스트
     *
     * XSS(Cross-Site Scripting) 공격 방지:
     * - 사용자 입력값(host.name 등)을 HTML에 렌더링할 때 필수
     * - <script>alert('xss')</script> 같은 악의적인 코드가 실행되지 않도록 이스케이프
     * - &, <, >, ", '를 HTML 엔티티로 변환
     *
     * 예시:
     * - escapeHtml('<script>') => '&lt;script&gt;'
     * - escapeHtml('A & B') => 'A &amp; B'
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

    // ======================================================================
    // 이벤트 리스너 등록
    // ======================================================================

    /**
     * 페이지 크기 변경 핸들러
     * - select box 값이 변경되면 호출됩니다.
     * - currentPageSize를 업데이트하고 첫 페이지로 이동합니다.
     * - 페이지 크기가 변경되면 현재 페이지 번호는 의미가 없으므로 1페이지로 리셋
     */
    function handlePageSizeChange() {
        currentPageSize = parseInt(pageSizeSelect.value);
        currentPage = 1; // 첫 페이지로 리셋
        loadHosts();
    }

    /**
     * 로그아웃 핸들러
     * - 확인 대화상자를 표시하여 실수로 로그아웃하는 것을 방지합니다.
     * - 확인 시 Auth.logout() 호출 (토큰 삭제 + 로그인 페이지 이동)
     */
    function handleLogout() {
        if (confirm('로그아웃 하시겠습니까?')) {
            Auth.logout();
        }
    }

    // 이벤트 리스너 등록
    pageSizeSelect.addEventListener('change', handlePageSizeChange);
    logoutBtn.addEventListener('click', handleLogout);

    /**
     * 테이블 헤더 정렬 클릭 이벤트 리스너 (이벤트 위임)
     */
    const hostsTable = document.getElementById('hostsTable');
    if (hostsTable) {
        const thead = hostsTable.querySelector('thead');
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
     * 접근성 향상:
     * - 마우스 없이도 키보드로 페이지 이동 가능
     * - preventDefault()로 브라우저 기본 동작(스크롤 등) 방지
     *
     * 주의: input, textarea, select, contentEditable 요소에 포커스가 있을 때는
     * 화살표 키가 해당 요소 내에서 커서 이동에 사용되므로 페이지 이동을 무시한다.
     */
    document.addEventListener('keydown', function(event) {
        // input, textarea, select, contentEditable 요소에 포커스가 있으면 무시
        const activeElement = document.activeElement;
        const isInputFocused = activeElement && (
            activeElement.tagName === 'INPUT' ||
            activeElement.tagName === 'TEXTAREA' ||
            activeElement.tagName === 'SELECT' ||
            activeElement.isContentEditable
        );

        if (isInputFocused) {
            return;
        }

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

    // ======================================================================
    // Modal Logic - 호스트 스페이스 목록
    // ======================================================================

    /**
     * 모달 관련 DOM 요소 참조
     */
    const hostSpacesModal = document.getElementById('hostSpacesModal');
    const hostSpacesModalTitle = document.getElementById('hostSpacesModalTitle');
    const hostSpacesLoading = document.getElementById('hostSpacesLoading');
    const hostSpacesError = document.getElementById('hostSpacesError');
    const hostSpacesContent = document.getElementById('hostSpacesContent');
    const hostSpacesScrollArea = document.getElementById('hostSpacesScrollArea');
    const hostSpacesScrollFade = document.getElementById('hostSpacesScrollFade');
    const hostSpacesCountInfo = document.getElementById('hostSpacesCountInfo');
    const closeHostSpacesModalBtn = document.getElementById('closeHostSpacesModalBtn');
    const closeHostSpacesFooterBtn = document.getElementById('closeHostSpacesFooterBtn');

    // 슬라이드 전환 관련 DOM 요소
    const backToSpaceListBtn = document.getElementById('backToSpaceListBtn');
    const hostModalVisitSpaceBtn = document.getElementById('hostModalVisitSpaceBtn');
    const modalPanelContainer = document.getElementById('modalPanelContainer');
    const spaceDetailLoading = document.getElementById('spaceDetailLoading');
    const spaceDetailError = document.getElementById('spaceDetailError');
    const spaceDetailContent = document.getElementById('spaceDetailContent');

    /**
     * 모달 내부 슬라이드 상태 관리
     * - currentModalView: 현재 표시 중인 패널 ('list' | 'detail')
     * - currentDetailSpaceCode: 현재 상세 보기 중인 스페이스 코드
     * - savedModalTitle: 상세 전환 전 저장한 모달 제목 (복원용)
     */
    let currentModalView = 'list';
    let currentDetailSpaceCode = null;
    let savedModalTitle = '';

    /**
     * 모달 열기
     * - display: flex로 변경 후 다음 프레임에서 .show 추가 (CSS 애니메이션)
     * - body에 modal-open 추가하여 배경 스크롤 방지
     */
    function openHostSpacesModal() {
        hostSpacesModal.style.display = 'flex';
        requestAnimationFrame(() => {
            hostSpacesModal.classList.add('show');
        });
        document.body.classList.add('modal-open');
    }

    /**
     * 모달 닫기
     * - .show 제거 후 300ms 대기 (CSS transition과 동일) → display: none
     * - 모달 내용 초기화
     */
    function closeHostSpacesModal() {
        hostSpacesModal.classList.remove('show');
        document.body.classList.remove('modal-open');

        setTimeout(() => {
            hostSpacesModal.style.display = 'none';
            resetHostSpacesModal();
            // 슬라이드 상태 즉시 초기화 (재열기 시 항상 목록부터)
            modalPanelContainer.style.transform = 'translateX(0%)';
            backToSpaceListBtn.style.display = 'none';
            hostModalVisitSpaceBtn.style.display = 'none';
            currentModalView = 'list';
            currentDetailSpaceCode = null;
            savedModalTitle = '';
            resetSpaceDetailPanel();
        }, 300);
    }

    /**
     * 모달 내용 초기화
     */
    function resetHostSpacesModal() {
        hostSpacesLoading.style.display = 'none';
        hostSpacesError.style.display = 'none';
        hostSpacesContent.style.display = 'none';
        hostSpacesContent.innerHTML = '';
        hostSpacesScrollFade.style.display = 'none';
        hostSpacesCountInfo.textContent = '';
    }

    // ==================================================================
    // 슬라이드 전환 함수
    // ==================================================================

    /**
     * 스페이스 상세 패널 초기화
     */
    function resetSpaceDetailPanel() {
        spaceDetailLoading.style.display = 'none';
        spaceDetailError.style.display = 'none';
        spaceDetailContent.style.display = 'none';
        document.getElementById('detailSpaceId').textContent = '-';
        document.getElementById('detailSpaceCode').textContent = '-';
        document.getElementById('detailSpaceName').textContent = '-';
        document.getElementById('detailSpacePublic').innerHTML = '-';
        document.getElementById('detailProductCount').textContent = '-';
        document.getElementById('detailGuestBookCount').textContent = '-';
        document.getElementById('detailCreatedAt').textContent = '-';
        document.getElementById('detailUpdatedAt').textContent = '-';
    }

    /**
     * 목록 → 상세 슬라이드 전환
     * @param {string} spaceName - 모달 제목에 표시할 스페이스 이름
     */
    function slideToDetail(spaceName) {
        savedModalTitle = hostSpacesModalTitle.textContent;
        hostSpacesModalTitle.textContent = spaceName;
        modalPanelContainer.style.transform = 'translateX(-50%)';
        backToSpaceListBtn.style.display = 'flex';
        hostModalVisitSpaceBtn.style.display = 'inline-flex';
        hostSpacesScrollFade.style.display = 'none';
        hostSpacesCountInfo.textContent = '';
        currentModalView = 'detail';
    }

    /**
     * 상세 → 목록 슬라이드 복귀
     */
    function slideToList() {
        hostSpacesModalTitle.textContent = savedModalTitle;
        modalPanelContainer.style.transform = 'translateX(0%)';
        backToSpaceListBtn.style.display = 'none';
        hostModalVisitSpaceBtn.style.display = 'none';
        currentModalView = 'list';
        currentDetailSpaceCode = null;

        // 슬라이드 애니메이션 완료 후 상세 패널 초기화
        setTimeout(() => {
            resetSpaceDetailPanel();
            // 목록 패널 스크롤 인디케이터 복원
            requestAnimationFrame(() => updateScrollFade());
        }, 300);
    }

    /**
     * 상세 패널 로딩 상태 표시
     */
    function showSpaceDetailLoading() {
        spaceDetailLoading.style.display = 'flex';
        spaceDetailError.style.display = 'none';
        spaceDetailContent.style.display = 'none';
    }

    /**
     * 상세 패널 에러 상태 표시
     * @param {string} message - 에러 메시지
     */
    function showSpaceDetailError(message) {
        spaceDetailLoading.style.display = 'none';
        spaceDetailError.textContent = message;
        spaceDetailError.style.display = 'block';
        spaceDetailContent.style.display = 'none';
    }

    /**
     * 상세 패널 콘텐츠 렌더링
     * @param {object} data - SpaceDetailResponse (space, productCount, guestBookCount)
     */
    function showSpaceDetailContent(data) {
        spaceDetailLoading.style.display = 'none';
        spaceDetailError.style.display = 'none';

        const space = data.space;

        document.getElementById('detailSpaceId').textContent = space.id;
        document.getElementById('detailSpaceCode').textContent = space.code;
        document.getElementById('detailSpaceName').textContent = space.name;

        const publicBadge = space.isPublic
            ? '<span class="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold bg-success/10 text-success">공개</span>'
            : '<span class="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold bg-danger/10 text-danger">비공개</span>';
        document.getElementById('detailSpacePublic').innerHTML = publicBadge;

        document.getElementById('detailProductCount').textContent = `${data.productCount}개`;
        document.getElementById('detailGuestBookCount').textContent = `${data.guestBookCount}개`;
        document.getElementById('detailCreatedAt').textContent = formatDateTime(space.createdAt);
        document.getElementById('detailUpdatedAt').textContent = formatDateTime(space.updatedAt);

        spaceDetailContent.style.display = 'block';
    }

    /**
     * 모달 내 스페이스 상세 로드 + 슬라이드 전환
     * @param {string} spaceCode - 스페이스 코드
     * @param {string} spaceName - 스페이스 이름
     */
    async function loadSpaceDetailInModal(spaceCode, spaceName) {
        currentDetailSpaceCode = spaceCode;
        showSpaceDetailLoading();
        slideToDetail(spaceName);

        try {
            const data = await API.getSpaceDetail(spaceCode);
            showSpaceDetailContent(data);
        } catch (error) {
            console.error('Failed to load space detail:', error);
            showSpaceDetailError(error.message || '스페이스 정보를 불러오는데 실패했습니다.');
        }
    }

    /**
     * 게스트 페이지 URL 생성
     * @param {string} spaceCode - 스페이스 코드
     * @returns {string} 게스트 페이지 URL
     */
    function getSpacePageUrl(spaceCode) {
        const hostname = window.location.hostname;

        let guestDomain;
        if (hostname === 'api.forgather.app') {
            guestDomain = 'forgather.app';
        } else if (hostname === 'api.dev.forgather.app') {
            guestDomain = 'dev.forgather.app';
        } else {
            guestDomain = 'dev.forgather.app';
        }

        return `https://${guestDomain}/guest/${spaceCode}/home`;
    }

    /**
     * 모달 로딩 상태 표시
     */
    function showHostSpacesLoading() {
        resetHostSpacesModal();
        hostSpacesLoading.style.display = 'flex';
    }

    /**
     * 모달 에러 상태 표시
     * @param {string} message - 에러 메시지
     */
    function showHostSpacesError(message) {
        resetHostSpacesModal();
        hostSpacesError.textContent = message;
        hostSpacesError.style.display = 'block';
    }

    /**
     * 스크롤 페이드 인디케이터 업데이트
     * - 스크롤 가능할 때 하단 페이드 표시
     * - 스크롤이 끝에 도달하면 페이드 숨김
     */
    function updateScrollFade() {
        if (!hostSpacesScrollArea || !hostSpacesScrollFade) return;

        const { scrollTop, scrollHeight, clientHeight } = hostSpacesScrollArea;
        const isScrollable = scrollHeight > clientHeight + 4;
        const isAtBottom = scrollTop + clientHeight >= scrollHeight - 4;

        hostSpacesScrollFade.style.display = (isScrollable && !isAtBottom) ? 'block' : 'none';
    }

    hostSpacesScrollArea.addEventListener('scroll', updateScrollFade);

    /**
     * 스페이스 목록 렌더링
     * @param {Array} spaces - SimpleSpaceResponse 배열
     *
     * 각 아이템: 카드 형태, 라벨 표기 (이름, 코드, 생성일, 공개 여부)
     */
    function renderHostSpaces(spaces) {
        resetHostSpacesModal();

        if (!spaces || spaces.length === 0) {
            hostSpacesContent.innerHTML = `
                <div class="flex flex-col items-center justify-center py-2xl text-text-secondary">
                    <svg class="w-12 h-12 mb-md text-text-muted opacity-50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4"/>
                    </svg>
                    <p class="text-sm">스페이스가 없습니다.</p>
                </div>
            `;
            hostSpacesContent.style.display = 'block';
            hostSpacesCountInfo.textContent = '';
            return;
        }

        const items = spaces.map((space, index) => {
            const publicBadge = space.isPublic
                ? '<span class="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold bg-success/10 text-success">공개</span>'
                : '<span class="inline-flex items-center px-2.5 py-1 rounded-md text-xs font-semibold bg-danger/10 text-danger">비공개</span>';

            return `
                <div class="p-md rounded-lg border border-border-color bg-bg-color transition-colors duration-150 space-card-clickable ${index > 0 ? 'mt-sm' : ''}"
                     data-space-code="${escapeHtml(space.code)}" data-space-name="${escapeHtml(space.name)}"
                     role="button" tabindex="0">
                    <div class="flex items-start justify-between gap-md mb-2">
                        <div class="flex items-center gap-sm min-w-0">
                            <span class="text-xs font-medium text-text-muted whitespace-nowrap">이름</span>
                            <span class="text-sm font-semibold text-text-primary truncate">${escapeHtml(space.name)}</span>
                        </div>
                        <div class="flex items-center gap-xs shrink-0">
                            ${publicBadge}
                            <svg class="w-4 h-4 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                            </svg>
                        </div>
                    </div>
                    <div class="grid grid-cols-2 gap-x-lg gap-y-1">
                        <div class="flex items-center gap-sm">
                            <span class="text-xs font-medium text-text-muted whitespace-nowrap">코드</span>
                            <span class="text-xs font-mono text-text-secondary truncate">${escapeHtml(space.code)}</span>
                        </div>
                        <div class="flex items-center gap-sm">
                            <span class="text-xs font-medium text-text-muted whitespace-nowrap">생성일</span>
                            <span class="text-xs text-text-secondary">${formatDateTime(space.createdAt)}</span>
                        </div>
                    </div>
                </div>
            `;
        }).join('');

        hostSpacesContent.innerHTML = items;
        hostSpacesContent.style.display = 'block';
        hostSpacesCountInfo.textContent = `총 ${spaces.length}개 스페이스`;

        // 렌더링 후 스크롤 인디케이터 업데이트
        requestAnimationFrame(() => updateScrollFade());
    }

    /**
     * 호스트 스페이스 목록 로드
     *
     * @param {number} hostId - 호스트 ID
     * @param {string} hostName - 호스트 이름 (모달 제목에 표시)
     *
     * 동작 흐름:
     * 1. 모달 제목에 호스트명 설정
     * 2. 모달 열기 + 로딩 표시
     * 3. API 호출 (API.getHostSpaces)
     * 4. 성공: 스페이스 목록 렌더링 / 실패: 에러 메시지 표시
     */
    async function loadHostSpaces(hostId, hostName) {
        hostSpacesModalTitle.textContent = `${hostName}의 스페이스`;
        openHostSpacesModal();
        showHostSpacesLoading();

        try {
            const data = await API.getHostSpaces(hostId);
            renderHostSpaces(data.spaces);
        } catch (error) {
            console.error('Failed to load host spaces:', error);
            showHostSpacesError(error.message || '스페이스 목록을 불러오는데 실패했습니다.');
        }
    }

    /**
     * 테이블 바디 클릭 이벤트 핸들러 (이벤트 위임)
     * - 스페이스 개수 셀(data-host-id) 클릭 시 모달 열기
     */
    hostsTableBody.addEventListener('click', function(event) {
        const td = event.target.closest('td[data-host-id]');
        if (td) {
            const hostId = td.getAttribute('data-host-id');
            const hostName = td.getAttribute('data-host-name');
            if (hostId) {
                loadHostSpaces(parseInt(hostId), hostName);
            }
        }
    });

    /**
     * 스페이스 개수 셀 키보드 이벤트 (접근성)
     * - Enter 또는 Space 키로 모달 열기
     */
    hostsTableBody.addEventListener('keydown', function(event) {
        if (event.key === 'Enter' || event.key === ' ') {
            const td = event.target.closest('td[data-host-id]');
            if (td) {
                event.preventDefault();
                const hostId = td.getAttribute('data-host-id');
                const hostName = td.getAttribute('data-host-name');
                if (hostId) {
                    loadHostSpaces(parseInt(hostId), hostName);
                }
            }
        }
    });

    /**
     * 모달 닫기: 헤더 X 버튼
     */
    closeHostSpacesModalBtn.addEventListener('click', function() {
        closeHostSpacesModal();
    });

    /**
     * 모달 닫기: 푸터 닫기 버튼
     */
    closeHostSpacesFooterBtn.addEventListener('click', function() {
        closeHostSpacesModal();
    });

    /**
     * 모달 닫기: 오버레이(배경) 클릭
     * - event.target === hostSpacesModal이면 오버레이 직접 클릭
     */
    hostSpacesModal.addEventListener('click', function(event) {
        if (event.target === hostSpacesModal) {
            closeHostSpacesModal();
        }
    });

    /**
     * 모달 닫기: ESC 키
     * - 상세 화면: 목록으로 복귀
     * - 목록 화면: 모달 닫기
     */
    document.addEventListener('keydown', function(event) {
        if (event.key === 'Escape' && hostSpacesModal.classList.contains('show')) {
            if (currentModalView === 'detail') {
                slideToList();
            } else {
                closeHostSpacesModal();
            }
        }
    });

    // ======================================================================
    // 슬라이드 전환 이벤트 리스너
    // ======================================================================

    /**
     * 뒤로가기 버튼 클릭 → 목록 복귀
     */
    backToSpaceListBtn.addEventListener('click', function() {
        slideToList();
    });

    /**
     * 게스트 페이지 이동 버튼 클릭
     */
    hostModalVisitSpaceBtn.addEventListener('click', function() {
        if (currentDetailSpaceCode) {
            window.open(getSpacePageUrl(currentDetailSpaceCode), '_blank');
        }
    });

    /**
     * 스페이스 카드 클릭 → 상세 슬라이드 전환 (이벤트 위임)
     */
    hostSpacesContent.addEventListener('click', function(event) {
        const card = event.target.closest('[data-space-code]');
        if (card) {
            const spaceCode = card.getAttribute('data-space-code');
            const spaceName = card.getAttribute('data-space-name');
            if (spaceCode) {
                loadSpaceDetailInModal(spaceCode, spaceName);
            }
        }
    });

    /**
     * 스페이스 카드 키보드 접근성 (Enter/Space)
     */
    hostSpacesContent.addEventListener('keydown', function(event) {
        if (event.key === 'Enter' || event.key === ' ') {
            const card = event.target.closest('[data-space-code]');
            if (card) {
                event.preventDefault();
                const spaceCode = card.getAttribute('data-space-code');
                const spaceName = card.getAttribute('data-space-name');
                if (spaceCode) {
                    loadSpaceDetailInModal(spaceCode, spaceName);
                }
            }
        }
    });

    // ======================================================================
    // 초기 로드
    // ======================================================================

    /**
     * 페이지 로드 시 첫 번째 페이지의 데이터를 자동으로 가져옵니다.
     * - DOMContentLoaded 이벤트가 발생한 후 실행
     * - 사용자가 페이지를 열면 즉시 데이터를 볼 수 있습니다.
     */
    loadHosts();
});
