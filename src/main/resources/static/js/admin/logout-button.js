/**
 * 헤더 로그아웃 버튼 연결
 * - 페이지 전용 JS에 로그아웃 처리가 없는 화면(통계 등)에서 사용한다.
 * - auth.js 이후에 로드해야 한다.
 */
document.addEventListener('DOMContentLoaded', function() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (!logoutBtn) {
        return;
    }
    logoutBtn.addEventListener('click', () => {
        if (confirm('로그아웃 하시겠습니까?')) {
            Auth.logout();
        }
    });
});
