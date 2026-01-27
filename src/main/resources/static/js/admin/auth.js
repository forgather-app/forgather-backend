/**
 * Authentication Utility Module
 * 세션 기반 인증 상태 확인
 *
 * 세션 방식에서는 Cookie가 자동으로 전송되므로
 * 클라이언트에서 토큰을 관리할 필요가 없습니다.
 * 인증 여부는 서버의 인터셉터가 처리합니다.
 */

const Auth = {
    /**
     * 세션 쿠키 이름
     */
    SESSION_COOKIE_NAME: 'ADMIN_SESSION_ID',

    /**
     * 세션 쿠키 존재 여부 확인
     * @returns {boolean} 세션 쿠키 존재 여부
     */
    hasSessionCookie() {
        return document.cookie.split(';').some(cookie =>
            cookie.trim().startsWith(this.SESSION_COOKIE_NAME + '=')
        );
    },

    /**
     * 인증 여부 확인
     * 세션 쿠키가 있으면 인증된 것으로 간주
     * (실제 세션 유효성은 서버에서 검증)
     * @returns {boolean} 인증 여부
     */
    isAuthenticated() {
        return this.hasSessionCookie();
    },

    /**
     * 로그인 페이지로 리다이렉트
     */
    redirectToLogin() {
        window.location.href = '/view/admin/login';
    },

    /**
     * 인증 확인 및 리다이렉트
     * 세션 쿠키가 없는 경우 로그인 페이지로 이동
     * 세션 만료 여부는 API 호출 시 서버에서 검증
     */
    requireAuth() {
        if (!this.isAuthenticated()) {
            this.redirectToLogin();
            return false;
        }
        return true;
    },

    /**
     * 로그아웃 처리
     * 세션 쿠키 삭제 후 로그인 페이지로 이동
     */
    logout() {
        // 쿠키 삭제 (만료 시간을 과거로 설정)
        document.cookie = this.SESSION_COOKIE_NAME + '=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
        this.redirectToLogin();
    }
};

// 전역 객체로 노출
window.Auth = Auth;
