/**
 * Products Management Page Script
 * 작품 목록 조회, 스페이스 코드 필터, 작품명 검색, 정렬, 페이지네이션, 상세 모달 처리
 */

// 전역 상태
let currentPage = 1;
let currentPageSize = 15;
let totalPages = 1;
let totalCount = 0;

/**
 * 정렬 상태
 *
 * field: 정렬 기준 필드 (title, createdAt, updatedAt)
 * direction: 정렬 방향 (asc, desc)
 *
 * 기본값: 등록 시간 내림차순 (최신순)
 * 정렬 값이 같은 작품은 서버가 id 정렬을 덧붙여 페이지 간 순서를 고정한다.
 */
const sortState = {
    field: 'createdAt',
    direction: 'desc'
};

/**
 * 필터 상태
 *
 * spaceCode 값:
 * - null: 필터 없음 (전체)
 * - 문자열: 해당 스페이스 코드와 완전히 일치하는 작품만 조회
 */
const filterState = {
    spaceCode: null
};

/**
 * 작품명 검색 상태
 *
 * 동작 규칙:
 * - 작품명 검색과 필터는 상호 배타적
 * - 작품명 검색 시 필터 초기화, 필터 적용 시 작품명 검색 초기화
 *
 * title 값:
 * - null: 검색 비활성화
 * - 문자열: 해당 작품명으로 부분 일치 검색
 */
const searchState = {
    title: null
};

document.addEventListener('DOMContentLoaded', function () {
    // 인증 확인은 서버 인터셉터가 처리 (미인증 시 로그인 페이지로 리다이렉트)

    // DOM 요소 참조
    const productsTable = document.getElementById('productsTable');
    const productsTableBody = document.getElementById('productsTableBody');
    const errorMessage = document.getElementById('errorMessage');
    const pageSizeSelect = document.getElementById('pageSize');
    const paginationContainer = document.getElementById('pagination');
    const totalCountInfo = document.getElementById('totalCountInfo');
    const logoutBtn = document.getElementById('logoutBtn');
    const tableScrollWrapper = document.getElementById('tableScrollWrapper');

    const spaceCodeFilterInput = document.getElementById('spaceCodeFilterInput');
    const applyFilterBtn = document.getElementById('applyFilterBtn');
    const resetFilterBtn = document.getElementById('resetFilterBtn');
    const titleSearchInput = document.getElementById('titleSearchInput');
    const titleSearchBtn = document.getElementById('titleSearchBtn');
    const resetSearchBtn = document.getElementById('resetSearchBtn');

    const COLUMN_COUNT = 8;

    // ==========================================================================
    // 공통 유틸리티
    // ==========================================================================

    /**
     * HTML 이스케이프 (XSS 방지)
     * - 서버 데이터는 모두 사용자 입력이므로 innerHTML에 넣기 전에 반드시 거친다.
     *
     * @param {*} text - 이스케이프할 값
     * @returns {string} 이스케이프된 문자열 (null, undefined는 빈 문자열)
     */
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

    /**
     * 날짜/시간 포맷팅 (yyyy.MM.dd HH:mm)
     *
     * @param {string} dateTimeString - ISO 8601 형식의 날짜/시간 문자열
     * @returns {string} 포맷된 문자열, 값이 없거나 유효하지 않으면 "-"
     */
    function formatDateTime(dateTimeString) {
        if (!dateTimeString) {
            return '-';
        }
        const date = new Date(dateTimeString);
        if (isNaN(date.getTime())) {
            return '-';
        }
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${year}.${month}.${day} ${hours}:${minutes}`;
    }

    /**
     * 파일 용량 포맷팅
     *
     * @param {number} bytes - 바이트 단위 용량
     * @returns {string} 사람이 읽기 쉬운 용량 문자열 (예: "512 B", "1.5 KB", "2.3 MB")
     */
    function formatCapacity(bytes) {
        if (bytes === null || bytes === undefined || isNaN(bytes)) {
            return '-';
        }
        if (bytes < 1024) {
            return `${bytes} B`;
        }
        if (bytes < 1024 * 1024) {
            return `${(bytes / 1024).toFixed(1)} KB`;
        }
        return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    }

    /**
     * http(s) 링크인지 확인
     * - videoUrl은 사용자 입력이라 javascript: 같은 스킴이 들어올 수 있다.
     * - 이스케이프만으로는 href의 스킴 공격을 막을 수 없으므로 http(s)일 때만 링크로 렌더링한다.
     *
     * @param {string} url - 검사할 문자열
     * @returns {boolean} http 또는 https URL이면 true
     */
    function isHttpUrl(url) {
        try {
            const parsed = new URL(url);
            return parsed.protocol === 'http:' || parsed.protocol === 'https:';
        } catch (error) {
            return false;
        }
    }

    /**
     * 값이 비어 있으면 "미입력"을 흐린 글씨로 표시하는 HTML을 만든다.
     *
     * @param {string} value - 표시할 값
     * @returns {string} 이스케이프된 HTML
     */
    function textOrPlaceholder(value) {
        if (!value) {
            return '<span class="text-text-muted">미입력</span>';
        }
        return escapeHtml(value);
    }

    // ==========================================================================
    // 목록 화면 상태 표시
    // ==========================================================================

    /**
     * 테이블 스크롤 상태 업데이트
     * - 스크롤이 가능하면 'has-scroll' 클래스 추가
     * - 끝까지 스크롤하면 'scrolled-end' 클래스 추가
     */
    function updateTableScrollState() {
        if (!tableScrollWrapper) return;

        const { scrollWidth, clientWidth, scrollLeft } = tableScrollWrapper;
        const hasScroll = scrollWidth > clientWidth;
        const scrolledEnd = scrollLeft + clientWidth >= scrollWidth - 5;

        tableScrollWrapper.classList.toggle('has-scroll', hasScroll && !scrolledEnd);
        tableScrollWrapper.classList.toggle('scrolled-end', scrolledEnd);
    }

    /**
     * 스켈레톤 로딩 UI 렌더링 (레이아웃 흔들림 최소화)
     *
     * @param {number} rowCount - 표시할 스켈레톤 행 개수
     */
    function renderSkeletonLoading(rowCount) {
        productsTableBody.innerHTML = Array(rowCount).fill(null).map(() => `
            <tr class="animate-pulse">
                <td class="px-lg py-md"><div class="h-4 bg-neutral-200 rounded w-8"></div></td>
                <td class="px-lg py-md"><div class="h-4 bg-neutral-200 rounded w-40"></div></td>
                <td class="px-lg py-md"><div class="h-4 bg-neutral-200 rounded w-20"></div></td>
                <td class="px-lg py-md"><div class="h-4 bg-neutral-200 rounded w-32"></div></td>
                <td class="px-md py-md"><div class="h-4 bg-neutral-200 rounded w-6 mx-auto"></div></td>
                <td class="px-md py-md"><div class="h-5 bg-neutral-200 rounded-full w-12 mx-auto"></div></td>
                <td class="px-md py-md"><div class="h-4 bg-neutral-200 rounded w-28"></div></td>
                <td class="px-md py-md"><div class="h-4 bg-neutral-200 rounded w-28"></div></td>
            </tr>
        `).join('');
    }

    function showLoading() {
        renderSkeletonLoading(currentPageSize > 15 ? 10 : 5);
        paginationContainer.style.display = 'none';
    }

    /**
     * 에러 메시지 표시
     * @param {string} message - 에러 메시지
     */
    function showError(message) {
        errorMessage.textContent = message;
        errorMessage.style.display = 'block';
        productsTableBody.innerHTML = '';
        paginationContainer.style.display = 'none';
    }

    function hideError() {
        errorMessage.textContent = '';
        errorMessage.style.display = 'none';
    }

    function updateTotalCountInfo() {
        totalCountInfo.textContent = `Total: ${totalCount.toLocaleString()}개`;
    }

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
     * 정렬 아이콘 업데이트
     * - 현재 정렬 필드는 방향 아이콘(▲/▼) 표시
     * - 나머지 정렬 가능 필드는 hover 시 ⇅ 아이콘 표시
     */
    function updateSortIcons() {
        document.querySelectorAll('th[data-sort]').forEach(header => {
            const field = header.getAttribute('data-sort');
            const iconSpan = header.querySelector('.sort-icon');
            if (!iconSpan) {
                return;
            }
            if (field === sortState.field) {
                iconSpan.textContent = sortState.direction === 'asc' ? '▲' : '▼';
                iconSpan.classList.remove('text-text-muted', 'opacity-0', 'group-hover:opacity-100');
                iconSpan.classList.add('text-primary');
                header.setAttribute('aria-sort', sortState.direction === 'asc' ? 'ascending' : 'descending');
            } else {
                iconSpan.textContent = '⇅';
                iconSpan.classList.remove('text-primary');
                iconSpan.classList.add('text-text-muted', 'opacity-0', 'group-hover:opacity-100');
                header.removeAttribute('aria-sort');
            }
        });
    }

    function updateFilterAndSearchUI() {
        resetFilterBtn.style.display = hasActiveFilter() ? 'inline-flex' : 'none';
        resetSearchBtn.style.display = hasActiveTitleSearch() ? 'inline-flex' : 'none';
    }

    // ==========================================================================
    // 목록 렌더링
    // ==========================================================================

    /**
     * 빈 상태 렌더링
     * - 필터나 검색이 활성화되어 있으면 초기화 버튼을 함께 표시한다.
     */
    function renderEmptyState() {
        const hasCondition = hasActiveFilter() || hasActiveTitleSearch();
        const resetButtonHtml = hasCondition ? `
            <button type="button"
                    id="resetEmptyStateBtn"
                    class="inline-flex items-center gap-2 px-4 py-2.5 min-h-[44px]
                           text-sm font-medium text-white bg-primary rounded-lg
                           hover:bg-primary-hover active:scale-[0.98]
                           transition-all duration-200 cursor-pointer
                           focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                </svg>
                필터 초기화
            </button>
        ` : '';

        productsTableBody.innerHTML = `
            <tr>
                <td colspan="${COLUMN_COUNT}" class="text-center py-16">
                    <div class="flex flex-col items-center justify-center text-text-secondary">
                        <svg class="w-20 h-20 mb-6 text-text-muted opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"/>
                        </svg>
                        <h3 class="text-xl font-semibold text-text-primary mb-2">작품을 찾을 수 없습니다</h3>
                        <p class="text-sm mb-6">${hasCondition ? '검색 조건에 맞는 작품이 없습니다.' : '표시할 작품이 없습니다.'}</p>
                        ${resetButtonHtml}
                    </div>
                </td>
            </tr>
        `;
    }

    /**
     * 작품 테이블 렌더링
     *
     * @param {Array} products - 작품 목록 (AdminProductSummaryResponse[])
     *
     * 작품명 셀은 data-product-id 속성을 가지며, 클릭이나 Enter/Space로 상세 모달을 연다.
     */
    function renderProductsTable(products) {
        if (!products || products.length === 0) {
            renderEmptyState();
            return;
        }

        productsTableBody.innerHTML = products.map(product => {
            const videoBadge = product.videoUrl
                ? '<span class="badge badge-success">있음</span>'
                : '<span class="badge badge-neutral">없음</span>';
            const photoCountClass = product.photoCount === 0 ? 'text-text-muted' : 'text-text-primary';

            return `
                <tr class="hover:bg-bg-color transition-colors duration-150">
                    <td class="px-lg py-md text-sm text-text-primary">${escapeHtml(product.id)}</td>
                    <td class="px-lg py-md text-sm clickable-cell" data-product-id="${escapeHtml(product.id)}">
                        <span class="inline-flex max-w-full items-center gap-2 text-primary cursor-pointer
                                     px-2 py-1 -mx-2 rounded-md
                                     bg-neutral-50 border border-neutral-200
                                     hover:bg-neutral-100 hover:border-neutral-300
                                     active:bg-neutral-200 active:scale-[0.98]
                                     transition-all duration-150"
                              role="button"
                              tabindex="0"
                              title="${escapeHtml(product.title)}"
                              aria-label="${escapeHtml(product.title)} 작품 상세 보기">
                            <span class="truncate underline decoration-dashed decoration-1 underline-offset-2">${escapeHtml(product.title)}</span>
                            <svg class="w-4 h-4 shrink-0 text-text-secondary" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                            </svg>
                        </span>
                    </td>
                    <td class="px-lg py-md text-sm text-text-primary truncate" title="${escapeHtml(product.authorName)}">${textOrPlaceholder(product.authorName)}</td>
                    <td class="px-lg py-md text-sm">
                        <div class="truncate text-text-primary" title="${escapeHtml(product.spaceName)}">${escapeHtml(product.spaceName)}</div>
                        <div class="font-mono text-xs text-text-muted">${escapeHtml(product.spaceCode)}</div>
                    </td>
                    <td class="px-md py-md text-sm text-center ${photoCountClass}">${escapeHtml(product.photoCount)}</td>
                    <td class="px-md py-md text-sm text-center">${videoBadge}</td>
                    <td class="px-md py-md text-sm text-text-secondary font-mono whitespace-nowrap">${formatDateTime(product.createdAt)}</td>
                    <td class="px-md py-md text-sm text-text-secondary font-mono whitespace-nowrap">${formatDateTime(product.updatedAt)}</td>
                </tr>
            `;
        }).join('');

        requestAnimationFrame(updateTableScrollState);
    }

    // ==========================================================================
    // 데이터 로드
    // ==========================================================================

    function hasActiveFilter() {
        return filterState.spaceCode !== null && filterState.spaceCode !== '';
    }

    function hasActiveTitleSearch() {
        return searchState.title !== null && searchState.title !== '';
    }

    /**
     * 작품 목록 로드
     *
     * API 분기 우선순위:
     * 1. 작품명 검색 활성화 → /admin/products/search/by-title
     * 2. 스페이스 코드 필터 활성화 → /admin/products/search
     * 3. 전체 조회 → /admin/products
     */
    async function loadProducts() {
        hideError();
        showLoading();

        try {
            const sortParam = `${sortState.field},${sortState.direction}`;
            let response;

            if (hasActiveTitleSearch()) {
                response = await API.searchProductsByTitle(searchState.title, currentPage, currentPageSize, sortParam);
            } else if (hasActiveFilter()) {
                response = await API.getProductsByFilters(currentPage, currentPageSize, filterState, sortParam);
            } else {
                response = await API.getProducts(currentPage, currentPageSize, sortParam);
            }

            currentPage = response.currentPage;
            currentPageSize = response.pageSize;
            totalPages = response.totalPages;
            totalCount = response.totalCount;

            renderProductsTable(response.products);
            updatePagination();
            updateTotalCountInfo();
            updateSortIcons();
        } catch (error) {
            console.error('Failed to load products:', error);
            showError(error.message || '작품 목록을 불러오는데 실패했습니다. 다시 시도해주세요.');
        }
    }

    /**
     * 페이지 이동
     * @param {number} page - 이동할 페이지 번호 (1 ~ totalPages)
     */
    function goToPage(page) {
        if (page < 1 || page > totalPages) {
            return;
        }
        currentPage = page;
        loadProducts();
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
            sortState.direction = sortState.direction === 'asc' ? 'desc' : 'asc';
        } else {
            sortState.field = field;
            sortState.direction = 'asc';
        }
        currentPage = 1;
        loadProducts();
    }

    // ==========================================================================
    // 필터와 검색
    // ==========================================================================

    function clearFilterState() {
        filterState.spaceCode = null;
        spaceCodeFilterInput.value = '';
    }

    function clearTitleSearchState() {
        searchState.title = null;
        titleSearchInput.value = '';
    }

    /**
     * 스페이스 코드 필터 적용
     * - 작품명 검색을 초기화한다 (상호 배타)
     * - 입력이 비어 있으면 필터를 해제한다
     */
    function applyFilter() {
        clearTitleSearchState();
        const spaceCode = spaceCodeFilterInput.value.trim();
        filterState.spaceCode = spaceCode === '' ? null : spaceCode;
        spaceCodeFilterInput.value = spaceCode;
        updateFilterAndSearchUI();
        currentPage = 1;
        loadProducts();
    }

    function resetFilter() {
        clearFilterState();
        updateFilterAndSearchUI();
        currentPage = 1;
        loadProducts();
    }

    /**
     * 작품명 검색 실행
     * - 스페이스 코드 필터를 초기화한다 (상호 배타)
     * - 검색어가 비어 있으면 검색을 해제하고 전체를 조회한다
     */
    function searchByTitle() {
        const title = titleSearchInput.value.trim();
        if (!title) {
            resetTitleSearch();
            return;
        }
        clearFilterState();
        searchState.title = title;
        updateFilterAndSearchUI();
        currentPage = 1;
        loadProducts();
    }

    function resetTitleSearch() {
        clearTitleSearchState();
        updateFilterAndSearchUI();
        currentPage = 1;
        loadProducts();
    }

    function handlePageSizeChange() {
        currentPageSize = parseInt(pageSizeSelect.value, 10);
        currentPage = 1;
        loadProducts();
    }

    function handleLogout() {
        if (confirm('로그아웃 하시겠습니까?')) {
            Auth.logout();
        }
    }

    // ==========================================================================
    // 상세 모달
    // ==========================================================================

    const modal = document.getElementById('productDetailModal');
    const modalLoading = document.getElementById('modalLoading');
    const modalError = document.getElementById('modalError');
    const modalContent = document.getElementById('modalContent');
    const closeModalBtn = document.getElementById('closeModalBtn');
    const closeModalFooterBtn = document.getElementById('closeModalFooterBtn');

    const modalProductId = document.getElementById('modalProductId');
    const modalProductTitle = document.getElementById('modalProductTitle');
    const modalAuthorName = document.getElementById('modalAuthorName');
    const modalSpace = document.getElementById('modalSpace');
    const modalHost = document.getElementById('modalHost');
    const visitSpaceBtn = document.getElementById('visitSpaceBtn');
    const modalVideoUrl = document.getElementById('modalVideoUrl');
    const modalVideoPosition = document.getElementById('modalVideoPosition');
    const modalCreatedAt = document.getElementById('modalCreatedAt');
    const modalUpdatedAt = document.getElementById('modalUpdatedAt');
    const modalDescription = document.getElementById('modalDescription');
    const modalPhotoCount = document.getElementById('modalPhotoCount');
    const modalPhotosEmpty = document.getElementById('modalPhotosEmpty');
    const modalPhotosTableWrapper = document.getElementById('modalPhotosTableWrapper');
    const modalPhotosBody = document.getElementById('modalPhotosBody');

    /**
     * 가장 최근에 요청한 상세 작품 id
     * - 응답이 늦게 도착한 이전 요청이 현재 모달 내용을 덮어쓰지 않도록 비교에 사용한다.
     */
    let requestedProductId = null;
    let lastFocusedElement = null;

    /**
     * 현재 모달에 표시된 작품이 속한 스페이스 코드
     * - 게스트 페이지로 이동 버튼이 사용한다. 모달이 닫혀 있으면 null
     */
    let currentSpaceCode = null;

    // 공용 CSS(.modal-overlay)의 transition 시간과 같다.
    const MODAL_TRANSITION_MS = 300;

    function openModal() {
        lastFocusedElement = document.activeElement;
        modal.style.display = 'flex';
        requestAnimationFrame(() => {
            modal.classList.add('show');
        });
        document.body.classList.add('modal-open');

        // 오버레이의 visibility 전환이 시작되는 프레임에는 내부 요소가 포커스를 받지 못하므로,
        // 전환이 끝난 뒤 키보드 사용자를 위해 닫기 버튼으로 포커스를 옮긴다.
        setTimeout(() => {
            if (modal.classList.contains('show')) {
                closeModalBtn.focus();
            }
        }, MODAL_TRANSITION_MS);
    }

    function closeModal() {
        modal.classList.remove('show');
        document.body.classList.remove('modal-open');
        requestedProductId = null;

        setTimeout(() => {
            // 닫는 사이에 다른 작품으로 모달을 다시 열었으면 숨기지 않는다.
            if (modal.classList.contains('show')) {
                return;
            }
            modal.style.display = 'none';
            resetModalContent();
        }, MODAL_TRANSITION_MS);

        if (lastFocusedElement && typeof lastFocusedElement.focus === 'function') {
            lastFocusedElement.focus();
        }
    }

    function resetModalContent() {
        modalLoading.style.display = 'none';
        modalError.style.display = 'none';
        modalContent.style.display = 'none';
        visitSpaceBtn.style.display = 'none';
        currentSpaceCode = null;
    }

    function showModalLoading() {
        resetModalContent();
        modalLoading.style.display = 'flex';
    }

    function showModalError(message) {
        resetModalContent();
        modalError.textContent = message;
        modalError.style.display = 'block';
    }

    /**
     * 사진 목록 렌더링
     * @param {Array} photos - 사진 목록 (AdminProductPhotoResponse[], sortOrder 오름차순)
     */
    function renderPhotos(photos) {
        modalPhotoCount.textContent = `(${photos.length}장)`;

        if (photos.length === 0) {
            modalPhotosEmpty.style.display = 'block';
            modalPhotosTableWrapper.style.display = 'none';
            modalPhotosBody.innerHTML = '';
            return;
        }

        modalPhotosEmpty.style.display = 'none';
        modalPhotosTableWrapper.style.display = 'block';
        modalPhotosBody.innerHTML = photos.map(photo => `
            <tr>
                <td class="px-3 py-2 text-sm text-text-primary">${escapeHtml(photo.sortOrder)}</td>
                <td class="px-3 py-2 text-sm text-text-primary truncate" title="${escapeHtml(photo.originalName)}">${textOrPlaceholder(photo.originalName)}</td>
                <td class="px-3 py-2 text-sm text-text-secondary text-right whitespace-nowrap">${formatCapacity(photo.capacity)}</td>
            </tr>
        `).join('');
    }

    /**
     * 영상 링크 렌더링
     * - http(s) 링크만 새 탭 링크로 만들고, 그 외 값은 텍스트로 보여준다.
     *
     * @param {string} videoUrl - 임베드 영상 링크 (미입력이면 빈 문자열)
     */
    function renderVideoUrl(videoUrl) {
        if (!videoUrl) {
            modalVideoUrl.innerHTML = '<span class="text-text-muted">없음</span>';
            return;
        }
        if (isHttpUrl(videoUrl)) {
            modalVideoUrl.innerHTML = `<a href="${escapeHtml(videoUrl)}" target="_blank" rel="noopener noreferrer"
                class="text-info underline underline-offset-2 hover:text-primary">${escapeHtml(videoUrl)}</a>`;
            return;
        }
        modalVideoUrl.textContent = videoUrl;
    }

    /**
     * 모달에 작품 상세 정보 표시
     *
     * @param {object} data - API 응답 (AdminProductDetailResponse)
     */
    function showModalContent(data) {
        resetModalContent();

        modalProductId.textContent = data.id;
        modalProductTitle.textContent = data.title;
        modalAuthorName.innerHTML = textOrPlaceholder(data.authorName);
        modalSpace.innerHTML = `
            <span class="font-medium">${escapeHtml(data.space.name)}</span>
            <span class="ml-2 font-mono text-xs text-text-muted">${escapeHtml(data.space.code)}</span>
        `;
        renderHost(data.host);
        renderVideoUrl(data.videoUrl);
        modalVideoPosition.textContent = data.videoUrl
            ? (data.isVideoAfterPhoto ? '사진 뒤' : '사진 앞')
            : '-';
        modalCreatedAt.textContent = formatDateTime(data.createdAt);
        modalUpdatedAt.textContent = formatDateTime(data.updatedAt);

        if (data.description) {
            modalDescription.textContent = data.description;
        } else {
            modalDescription.innerHTML = '<span class="text-text-muted">미입력</span>';
        }

        renderPhotos(data.photos || []);
        modalContent.style.display = 'block';

        currentSpaceCode = data.space.code;
        visitSpaceBtn.style.display = 'block';
    }

    /**
     * 호스트 렌더링
     * - 이름과 닉네임을 함께 보여준다. 닉네임이 없거나 이름과 같으면 이름만 보여준다.
     * - 호스트가 탈퇴했거나 스페이스에 연결되지 않았으면 null이 온다.
     *
     * @param {object|null} host - 호스트 (id, name, nickname)
     */
    function renderHost(host) {
        if (!host) {
            modalHost.innerHTML = '<span class="text-text-muted">없음 (탈퇴 또는 미연결)</span>';
            return;
        }
        const nickname = host.nickname && host.nickname !== host.name
            ? `<span class="ml-2 text-xs text-text-secondary">닉네임: ${escapeHtml(host.nickname)}</span>`
            : '';
        modalHost.innerHTML = `<span class="font-medium">${escapeHtml(host.name)}</span>${nickname}`;
    }

    /**
     * 게스트 페이지 URL 생성 (spaces.js와 같은 규칙)
     *
     * - api.forgather.app → forgather.app
     * - api.dev.forgather.app → dev.forgather.app
     * - 그 외(localhost 등) → dev.forgather.app
     *
     * @param {string} spaceCode - 스페이스 코드
     * @returns {string} https://{guestDomain}/guest/{spaceCode}/home
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
        return `https://${guestDomain}/guest/${encodeURIComponent(spaceCode)}/home`;
    }

    /**
     * 작품 상세 정보 로드
     * @param {string} productId - 조회할 작품 id
     */
    async function loadProductDetail(productId) {
        requestedProductId = productId;
        openModal();
        showModalLoading();

        try {
            const data = await API.getProductDetail(productId);
            if (requestedProductId !== productId) {
                return;
            }
            showModalContent(data);
        } catch (error) {
            if (requestedProductId !== productId) {
                return;
            }
            console.error('Failed to load product detail:', error);
            showModalError(error.message || '작품 정보를 불러오는데 실패했습니다. 다시 시도해주세요.');
        }
    }

    function handleProductInteraction(event) {
        const td = event.target.closest('td[data-product-id]');
        if (!td) {
            return;
        }
        const productId = td.getAttribute('data-product-id');
        if (productId) {
            loadProductDetail(productId);
        }
    }

    // ==========================================================================
    // 이벤트 리스너 등록
    // ==========================================================================

    if (tableScrollWrapper) {
        tableScrollWrapper.addEventListener('scroll', updateTableScrollState);
        window.addEventListener('resize', updateTableScrollState);
    }

    pageSizeSelect.addEventListener('change', handlePageSizeChange);
    logoutBtn.addEventListener('click', handleLogout);

    applyFilterBtn.addEventListener('click', applyFilter);
    resetFilterBtn.addEventListener('click', resetFilter);
    spaceCodeFilterInput.addEventListener('keydown', function (event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            applyFilter();
        }
    });

    titleSearchBtn.addEventListener('click', searchByTitle);
    resetSearchBtn.addEventListener('click', resetTitleSearch);
    titleSearchInput.addEventListener('keydown', function (event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            searchByTitle();
        }
    });

    // 정렬 헤더 클릭 (이벤트 위임)
    productsTable.querySelector('thead').addEventListener('click', function (event) {
        const th = event.target.closest('th[data-sort]');
        if (th) {
            toggleSort(th.getAttribute('data-sort'));
        }
    });

    // 테이블 본문 클릭 (이벤트 위임): 빈 상태 초기화 버튼, 작품명 셀
    productsTableBody.addEventListener('click', function (event) {
        if (event.target.closest('#resetEmptyStateBtn')) {
            clearFilterState();
            clearTitleSearchState();
            updateFilterAndSearchUI();
            currentPage = 1;
            loadProducts();
            return;
        }
        handleProductInteraction(event);
    });

    // 작품명 셀 키보드 접근성: Enter 또는 Space로 상세 모달 열기
    productsTableBody.addEventListener('keydown', function (event) {
        if (event.key !== 'Enter' && event.key !== ' ') {
            return;
        }
        if (event.target.closest('td[data-product-id]')) {
            event.preventDefault();
            handleProductInteraction(event);
        }
    });

    closeModalBtn.addEventListener('click', closeModal);
    closeModalFooterBtn.addEventListener('click', closeModal);

    // 게스트 페이지로 이동: 새 탭에서 열고 opener 접근을 차단한다
    visitSpaceBtn.addEventListener('click', function () {
        if (currentSpaceCode) {
            window.open(getSpacePageUrl(currentSpaceCode), '_blank', 'noopener,noreferrer');
        }
    });

    // 오버레이(모달 바깥 영역) 클릭 시 닫기
    modal.addEventListener('click', function (event) {
        if (event.target === modal) {
            closeModal();
        }
    });

    /**
     * 전역 키보드 처리
     * - ESC: 모달이 열려 있으면 닫기
     * - 좌우 화살표: 모달이 닫혀 있고 입력 요소에 포커스가 없을 때 페이지 이동
     */
    document.addEventListener('keydown', function (event) {
        const isModalOpen = modal.classList.contains('show');

        if (event.key === 'Escape' && isModalOpen) {
            closeModal();
            return;
        }
        if (isModalOpen) {
            return;
        }

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

        if (event.key === 'ArrowLeft' && currentPage > 1) {
            event.preventDefault();
            goToPage(currentPage - 1);
        } else if (event.key === 'ArrowRight' && currentPage < totalPages) {
            event.preventDefault();
            goToPage(currentPage + 1);
        }
    });

    // 초기 로드
    loadProducts();
});
