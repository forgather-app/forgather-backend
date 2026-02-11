/**
 * API Utility Module
 * Fetch API를 사용한 HTTP 요청 유틸리티
 * 세션 기반 인증 (Cookie 자동 전송)
 */

const API = {
    /**
     * API 기본 URL
     */
    BASE_URL: '/admin',

    /**
     * HTTP 메서드
     */
    METHOD: {
        GET: 'GET',
        POST: 'POST',
        PUT: 'PUT',
        DELETE: 'DELETE',
        PATCH: 'PATCH'
    },

    /**
     * 기본 헤더 생성
     * @returns {object} 헤더 객체
     */
    getHeaders() {
        return {
            'Content-Type': 'application/json'
        };
    },

    /**
     * Fetch 요청 래퍼
     *
     * @param {string} url - 요청 URL
     * @param {object} options - Fetch 옵션
     * @returns {Promise<object>} 응답 데이터
     *
     * 세션 인증:
     * - Cookie가 자동으로 전송됨 (credentials: 'same-origin')
     * - 401 에러 시 로그인 페이지로 리다이렉트
     *
     * @throws {Error} 네트워크 에러, 인증 실패, 권한 없음, 리소스 없음, 서버 에러 등
     */
    async request(url, options = {}) {
        try {
            // Cookie 자동 전송을 위해 credentials 설정
            options.credentials = 'same-origin';

            const response = await fetch(url, options);

            // 401 Unauthorized - 세션 만료 또는 미인증
            if (response.status === 401) {
                console.warn('[API] 401 Unauthorized:', url);
                Auth.logout();
                throw new Error('세션이 만료되었습니다. 다시 로그인해주세요.');
            }

            // 403 Forbidden - 권한 없음
            if (response.status === 403) {
                throw new Error('접근 권한이 없습니다.');
            }

            // 404 Not Found
            if (response.status === 404) {
                throw new Error('요청한 리소스를 찾을 수 없습니다.');
            }

            // 500 Internal Server Error
            if (response.status >= 500) {
                throw new Error('서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
            }

            // 에러 응답 처리
            if (!response.ok) {
                const errorData = await response.json().catch(() => null);
                const message = errorData?.message || `요청 실패 (${response.status})`;
                throw new Error(message);
            }

            // 204 No Content 또는 빈 응답
            if (response.status === 204 || response.headers.get('Content-Length') === '0') {
                return null;
            }

            return await response.json();
        } catch (error) {
            console.error('[API] Request Error:', error.message);
            throw error;
        }
    },

    /**
     * GET 요청
     * @param {string} endpoint - API 엔드포인트
     * @param {object} params - 쿼리 파라미터
     * @returns {Promise<object>} 응답 데이터
     */
    async get(endpoint, params = {}) {
        const url = new URL(this.BASE_URL + endpoint, window.location.origin);

        // 쿼리 파라미터 추가
        Object.keys(params).forEach(key => {
            if (params[key] !== null && params[key] !== undefined) {
                url.searchParams.append(key, params[key]);
            }
        });

        return this.request(url.toString(), {
            method: this.METHOD.GET,
            headers: this.getHeaders()
        });
    },

    /**
     * POST 요청
     * @param {string} endpoint - API 엔드포인트
     * @param {object} data - 요청 본문 데이터
     * @returns {Promise<object>} 응답 데이터
     */
    async post(endpoint, data = {}) {
        const url = this.BASE_URL + endpoint;

        return this.request(url, {
            method: this.METHOD.POST,
            headers: this.getHeaders(),
            body: JSON.stringify(data)
        });
    },

    /**
     * PUT 요청
     * @param {string} endpoint - API 엔드포인트
     * @param {object} data - 요청 본문 데이터
     * @returns {Promise<object>} 응답 데이터
     */
    async put(endpoint, data = {}) {
        const url = this.BASE_URL + endpoint;

        return this.request(url, {
            method: this.METHOD.PUT,
            headers: this.getHeaders(),
            body: JSON.stringify(data)
        });
    },

    /**
     * DELETE 요청
     * @param {string} endpoint - API 엔드포인트
     * @returns {Promise<object>} 응답 데이터
     */
    async delete(endpoint) {
        const url = this.BASE_URL + endpoint;

        return this.request(url, {
            method: this.METHOD.DELETE,
            headers: this.getHeaders()
        });
    },

    /**
     * 로그인 API
     * @param {string} username - 사용자명
     * @param {string} password - 비밀번호
     * @returns {Promise<object>} 로그인 응답 (sessionId) + Set-Cookie 헤더
     */
    async login(username, password) {
        return this.post('/login', {username, password});
    },

    /**
     * Space 목록 조회 API (전체 - 필터 없음)
     * @param {number} page - 페이지 번호 (1부터 시작)
     * @param {number} size - 페이지 크기
     * @param {string} sort - 정렬 파라미터 (예: 'createdAt,desc')
     * @returns {Promise<object>} Space 목록 응답
     */
    async getSpaces(page = 1, size = 15, sort = 'createdAt,desc') {
        return this.get('/spaces', {page, size, sort});
    },

    /**
     * Space 목록 조회 API (필터링)
     *
     * @param {number} page - 페이지 번호 (1부터 시작)
     * @param {number} size - 페이지 크기
     * @param {object} filters - 필터 조건 객체
     * @param {boolean|null} filters.hasProduct - 작품 소개 등록 여부 (true: 등록함, false: 미등록, null: 전체)
     * @param {string} sort - 정렬 파라미터 (예: 'createdAt,desc')
     * @returns {Promise<object>} Space 목록 응답
     *
     * 엔드포인트: GET /admin/spaces/search
     *
     * 쿼리 파라미터:
     * - page: 페이지 번호 (필수)
     * - size: 페이지 크기 (필수)
     * - sort: 정렬 (예: 'createdAt,desc')
     * - hasProduct: 작품 소개 등록 여부 (선택)
     *
     * 응답 구조:
     * - spaces: Space 목록 배열
     * - currentPage: 현재 페이지 번호 (1부터 시작)
     * - pageSize: 페이지 크기
     * - totalCount: 전체 Space 개수
     * - totalPages: 전체 페이지 수
     *
     * 사용 예시:
     * ```javascript
     * // 작품 소개 등록한 스페이스만 조회
     * const response = await API.getSpacesByFilters(1, 15, { hasProduct: true });
     *
     * // 작품 소개 미등록 스페이스만 조회
     * const response = await API.getSpacesByFilters(1, 15, { hasProduct: false });
     * ```
     *
     * 주의:
     * - filters.hasProduct가 null이면 해당 파라미터는 쿼리에 포함되지 않음
     * - 필터 조건이 없으면 getSpaces() 함수를 사용하는 것이 더 적합
     * - 향후 다른 필터 추가 시 filters 객체에 프로퍼티 추가
     */
    async getSpacesByFilters(page = 1, size = 15, filters = {}, sort = 'createdAt,desc') {
        const params = {page, size, sort};

        // hasProduct 필터가 명시적으로 true 또는 false인 경우에만 파라미터에 추가
        if (filters.hasProduct !== null && filters.hasProduct !== undefined) {
            params.hasProduct = filters.hasProduct;
        }

        return this.get('/spaces/search', params);
    },

    /**
     * Space 상세 정보 조회 API
     *
     * @param {string} spaceCode - 조회할 스페이스 코드 (예: "e3f6b97f19")
     * @returns {Promise<object>} 스페이스 상세 정보 응답 (SpaceDetailResponse)
     * @returns {object} response.space - 스페이스 기본 정보 (id, code, name, isPublic)
     * @returns {number} response.productCount - 등록한 작품 소개 개수 (0 이상의 정수)
     * @returns {number} response.guestBookCount - 방명록 개수
     * @throws {Error} API 호출 실패 시 에러 (404: 존재하지 않는 스페이스, 401: 인증 실패 등)
     *
     * 사용 예시:
     * ```javascript
     * try {
     *     const detail = await API.getSpaceDetail('e3f6b97f19');
     *     console.log(detail.space.name); // "졸업 전시"
     *     console.log(detail.productCount); // 5 (등록한 작품 소개 개수)
     *     console.log(detail.guestBookCount); // 42
     * } catch (error) {
     *     console.error('Failed to load space detail:', error);
     * }
     * ```
     *
     * 주의:
     * - 이 함수는 비동기(async)이므로 반드시 await 또는 .then() 사용 필요
     * - Authorization 헤더가 자동으로 포함되므로 로그인 상태여야 함
     * - spaceCode는 URL 경로에 포함되므로 특수문자가 있는 경우 인코딩 필요 없음
     */
    async getSpaceDetail(spaceCode) {
        return this.get(`/spaces/${spaceCode}`, {});
    },

    /**
     * Space 이름 검색 API
     *
     * @param {string} name - 검색할 스페이스 이름 (부분 일치)
     * @param {number} page - 페이지 번호 (1부터 시작)
     * @param {number} size - 페이지 크기 (기본값: 15)
     * @param {string} sort - 정렬 파라미터 (예: 'createdAt,desc')
     * @returns {Promise<object>} Space 목록 응답
     *
     * 엔드포인트: GET /admin/spaces/search/by-name
     *
     * 사용 예시:
     * ```javascript
     * const response = await API.searchSpacesByName('졸업', 1, 15);
     * console.log(response.spaces); // 이름에 '졸업'이 포함된 스페이스 목록
     * ```
     */
    async searchSpacesByName(name, page = 1, size = 15, sort = 'createdAt,desc') {
        return this.get('/spaces/search/by-name', { name, page, size, sort });
    },

    /**
     * Host 목록 조회 API
     *
     * @param {number} page - 페이지 번호 (1부터 시작)
     * @param {number} size - 페이지 크기 (기본값: 15)
     * @param {string} sort - 정렬 파라미터 (예: 'createdAt,desc')
     * @returns {Promise<object>} Host 목록 응답
     * @returns {Array} response.hosts - Host 목록 배열
     * @returns {number} response.currentPage - 현재 페이지 번호 (1부터 시작)
     * @returns {number} response.pageSize - 페이지 크기
     * @returns {number} response.totalCount - 전체 Host 개수
     * @returns {number} response.totalPages - 전체 페이지 수
     * @throws {Error} API 호출 실패 시 에러 (401: 인증 실패, 500: 서버 에러 등)
     *
     * Host 객체 구조:
     * - id: Host ID (숫자)
     * - name: Host 이름 (문자열)
     * - createdAt: 생성 일시 (ISO 8601 문자열)
     * - spaceCount: 소유한 Space 개수 (숫자)
     *
     * 사용 예시:
     * ```javascript
     * try {
     *     const response = await API.getHosts(1, 15);
     *     console.log(response.hosts); // [{ id: 1, name: "홍길동", ... }, ...]
     *     console.log(response.totalCount); // 100
     * } catch (error) {
     *     console.error('Failed to load hosts:', error);
     * }
     * ```
     *
     * 주의:
     * - 이 함수는 비동기(async)이므로 반드시 await 또는 .then() 사용 필요
     * - Authorization 헤더가 자동으로 포함되므로 로그인 상태여야 함
     * - 페이지 번호는 1부터 시작 (서버에서 0-based를 1-based로 변환하여 반환)
     */
    async getHosts(page = 1, size = 15, sort = 'createdAt,desc') {
        return this.get('/hosts', {page, size, sort});
    },

    /**
     * 특정 Host의 스페이스 목록 조회 API
     *
     * @param {number} hostId - 조회할 호스트 ID
     * @returns {Promise<object>} 호스트 스페이스 목록 응답 (HostSpacesResponse)
     * @returns {number} response.hostId - 호스트 ID
     * @returns {string} response.hostName - 호스트 이름
     * @returns {Array} response.spaces - 스페이스 목록 배열
     *
     * Space 객체 구조:
     * - id: Space ID (숫자)
     * - code: Space 코드 (문자열)
     * - name: Space 이름 (문자열)
     * - isPublic: 공개 여부 (boolean)
     * - createdAt: 생성 일시 (ISO 8601 문자열)
     * - updatedAt: 수정 일시 (ISO 8601 문자열)
     *
     * 엔드포인트: GET /admin/hosts/{hostId}/spaces
     */
    async getHostSpaces(hostId) {
        return this.get(`/hosts/${hostId}/spaces`, {});
    }
};

// 전역 객체로 노출
window.API = API;
