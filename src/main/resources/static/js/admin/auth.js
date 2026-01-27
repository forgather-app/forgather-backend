/**
 * Authentication Utility Module
 * 세션 기반 인증 유틸리티
 *
 * httpOnly 쿠키를 사용하므로 클라이언트에서 쿠키를 직접 확인할 수 없습니다.
 * 인증 여부는 서버의 인터셉터가 처리합니다.
 */

const Auth = {
    /**
     * 로그아웃 처리 - 서버 API 호출
     * httpOnly 쿠키는 JS에서 삭제 불가하므로 서버에서 처리
     */
    async logout() {
        try {
            await fetch('/admin/logout', {
                method: 'POST',
                credentials: 'same-origin'
            });
        } catch (e) {
            // 네트워크 에러 무시 (어차피 로그인 페이지로 이동)
        }
        window.location.href = '/view/admin/login';
    },

    /**
     * 로그인 페이지로 리다이렉트
     */
    redirectToLogin() {
        window.location.href = '/view/admin/login';
    }
};

// 전역 객체로 노출
window.Auth = Auth;
