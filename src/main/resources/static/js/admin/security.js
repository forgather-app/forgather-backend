/**
 * Security Dashboard Module
 * 보안 모니터링 대시보드 데이터 로딩 및 렌더링
 */

/**
 * 공격 유형 한국어 레이블 매핑
 * API 응답의 key(영문) → 화면 표시용 한국어
 * 백엔드 AttackType enum의 description 필드와 동일
 */
const ATTACK_TYPE_LABELS = {
    credential_scan: '인증 정보 탈취 시도',
    cms_scan: 'CMS 관리자 경로 탐색',
    vcs_debug_scan: '버전 관리/디버그 파일 노출',
    extension_scan: '민감한 파일 확장자 탐색',
    rce_scan: '원격 코드 실행 시도',
    other: '기타 의심 요청'
};

/**
 * 자동 새로고침 간격 (ms)
 */
const REFRESH_INTERVAL = 60000;
let isDashboardLoading = false;

/**
 * XSS 방지를 위한 HTML 이스케이프
 * @param {string} str - 이스케이프할 문자열
 * @returns {string} 이스케이프된 문자열
 */
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}

/**
 * 숫자를 천 단위 콤마 포맷으로 변환
 * @param {number} num - 포맷할 숫자
 * @returns {string} 포맷된 문자열
 */
function formatNumber(num) {
    const n = Number(num);
    return Number.isFinite(n) ? n.toLocaleString('ko-KR') : '-';
}

/**
 * Toast 메시지 표시
 * @param {string} message - 표시할 메시지
 * @param {string} type - 메시지 타입 (success, error, warning, info)
 */
function showToast(message, type = 'error') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 3000);
}

/**
 * URL이 유효한 HTTP(S) 프로토콜인지 검증
 * @param {string} str - 검증할 URL 문자열
 * @returns {boolean} http: 또는 https: 프로토콜이면 true
 */
function isValidHttpUrl(str) {
    try {
        const parsed = new URL(str);
        return parsed.protocol === 'https:' || parsed.protocol === 'http:';
    } catch {
        return false;
    }
}

/**
 * 대시보드 데이터 로드 및 렌더링
 */
async function loadDashboard() {
    if (isDashboardLoading) return;
    isDashboardLoading = true;
    try {
        const data = await API.getSecuritySummary();

        if (!data) {
            showToast('보안 데이터 응답이 비어있습니다.', 'error');
            clearDashboard();
            return;
        }

        // Prometheus 가용성 체크
        const prometheusBanner = document.getElementById('prometheusBanner');
        if (!data.prometheusAvailable) {
            if (prometheusBanner) prometheusBanner.style.display = 'flex';
            clearDashboard();
            return;
        }
        if (prometheusBanner) prometheusBanner.style.display = 'none';

        // Grafana 버튼 처리
        renderGrafanaButton(data.grafanaDashboardUrl);

        // 각 섹션 렌더링
        renderSummaryCards(data);
        renderWarningBanner(data);
        renderAttackTypeChart(data.attackTypes);
        renderTopAttackIps(data.topAttackIps);

        // 마지막 업데이트 시각
        const now = new Date();
        const timeStr = now.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
        document.getElementById('lastUpdated').textContent = `마지막 업데이트: ${timeStr} (60초마다 자동 갱신)`;

    } catch (error) {
        console.error('[Security] Dashboard load failed:', error.message);
        showToast('보안 데이터를 불러오는데 실패했습니다.', 'error');
    } finally {
        isDashboardLoading = false;
    }
}

/**
 * Prometheus 미연결 시 대시보드 초기화
 */
function clearDashboard() {
    document.getElementById('todayBlocked').textContent = '-';
    document.getElementById('blockedRatio').textContent = '-';
    document.getElementById('newAttackIps').textContent = '-';
    document.getElementById('rateLimited').textContent = '-';
    document.getElementById('warningBanner').style.display = 'none';
    document.getElementById('attackTypeChart').innerHTML =
        '<p class="text-sm text-text-muted text-center py-xl">데이터를 불러올 수 없습니다.</p>';
    document.getElementById('attackIpTableBody').innerHTML =
        '<tr><td colspan="3" class="py-xl text-sm text-text-muted text-center">데이터를 불러올 수 없습니다.</td></tr>';
    document.getElementById('lastUpdated').textContent = '';
}

/**
 * Grafana 버튼 표시/숨김
 * @param {string} url - Grafana 대시보드 URL (빈 문자열이면 숨김)
 */
function renderGrafanaButton(url) {
    const btn = document.getElementById('grafanaBtn');
    if (url && isValidHttpUrl(url)) {
        btn.href = url;
        btn.style.display = 'inline-flex';
    } else {
        btn.style.display = 'none';
    }
}

/**
 * 개별 카드 값 렌더링 (null 시 '-' 표시)
 * @param {string} elementId - 대상 요소 ID
 * @param {*} value - 표시할 값 (null이면 조회 실패)
 * @param {function} formatter - 값 포맷 함수
 */
function renderCardValue(elementId, value, formatter) {
    const el = document.getElementById(elementId);
    el.classList.remove('security-skeleton');
    if (value === null || value === undefined) {
        el.textContent = '-';
        el.title = '데이터를 불러올 수 없습니다';
    } else {
        el.textContent = formatter(value);
        el.title = '';
    }
}

/**
 * 요약 카드 4개 렌더링
 * @param {object} data - API 응답 데이터
 */
function renderSummaryCards(data) {
    renderCardValue('todayBlocked', data.todayBlocked, v => formatNumber(v));
    renderCardValue('blockedRatio', data.blockedRatio, v => {
        const ratio = Number(v);
        return (isNaN(ratio) ? '0.0' : ratio.toFixed(1)) + '%';
    });
    renderCardValue('newAttackIps', data.newAttackIps, v => formatNumber(v));
    renderCardValue('rateLimited', data.rateLimited, v => formatNumber(v));
}

/**
 * 주의 필요 배너 (unblocked4xx) 렌더링
 * @param {object} data - API 응답 데이터
 */
function renderWarningBanner(data) {
    const banner = document.getElementById('warningBanner');
    const count = document.getElementById('unblocked4xxCount');

    if (data.unblocked4xx != null && data.unblocked4xx > 0) {
        count.textContent = formatNumber(data.unblocked4xx);
        banner.style.display = 'flex';
    } else {
        banner.style.display = 'none';
    }
}

/**
 * 공격 유형 분포 CSS 막대 그래프 렌더링
 * @param {object} attackTypes - { "credential_scan": 774, "cms_scan": 312, ... }
 */
function renderAttackTypeChart(attackTypes) {
    const container = document.getElementById('attackTypeChart');

    if (attackTypes === null || attackTypes === undefined) {
        container.innerHTML = '<p class="text-sm text-text-muted text-center py-xl">데이터를 불러올 수 없습니다.</p>';
        return;
    }
    if (Object.keys(attackTypes).length === 0) {
        container.innerHTML = '<p class="text-sm text-text-muted text-center py-xl">공격 유형 데이터가 없습니다.</p>';
        return;
    }

    // 값 기준 내림차순 정렬
    const sorted = Object.entries(attackTypes)
        .sort((a, b) => b[1] - a[1]);

    const total = sorted.reduce((sum, [, count]) => sum + count, 0);
    const maxCount = sorted[0][1];

    let html = '';
    sorted.forEach(([key, count], index) => {
        const label = ATTACK_TYPE_LABELS[key] || key;
        const percentage = total > 0 ? ((count / total) * 100).toFixed(1) : '0.0';
        const barWidth = maxCount > 0 ? ((count / maxCount) * 100).toFixed(1) : '0';

        html += `<div class="security-bar-row" role="listitem">
            <span class="security-bar-label" title="${escapeHtml(label)}">${escapeHtml(label)}</span>
            <div class="security-bar-track" role="meter" aria-label="${escapeHtml(label)}"
                 aria-valuenow="${percentage}" aria-valuemin="0" aria-valuemax="100">
                <div class="security-bar-fill" data-rank="${index}" style="width: ${barWidth}%"></div>
            </div>
            <span class="security-bar-value">${escapeHtml(formatNumber(count))} (${percentage}%)</span>
        </div>`;
    });

    container.innerHTML = html;
}

/**
 * 공격 IP Top 5 테이블 렌더링
 * @param {Array} topAttackIps - [{ ip: "1.2.3.4", count: 342 }, ...]
 */
function renderTopAttackIps(topAttackIps) {
    const tbody = document.getElementById('attackIpTableBody');

    if (topAttackIps === null || topAttackIps === undefined) {
        tbody.innerHTML = '<tr><td colspan="3" class="py-xl text-sm text-text-muted text-center">데이터를 불러올 수 없습니다.</td></tr>';
        return;
    }
    if (topAttackIps.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" class="py-xl text-sm text-text-muted text-center">공격 IP 데이터가 없습니다.</td></tr>';
        return;
    }

    let html = '';
    topAttackIps.forEach((item, index) => {
        html += `<tr class="hover:bg-bg-color transition-colors duration-150">
            <td class="py-md text-sm text-text-secondary font-medium">${index + 1}</td>
            <td class="py-md text-sm text-text-primary font-mono">${escapeHtml(item.ip)}</td>
            <td class="py-md text-sm text-text-primary font-semibold text-right font-mono">${escapeHtml(formatNumber(item.count))}</td>
        </tr>`;
    });

    tbody.innerHTML = html;
}

/**
 * 로그아웃 버튼 이벤트 바인딩
 */
function initLogout() {
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => Auth.logout());
    }
}

/**
 * 초기화 및 자동 새로고침 설정
 */
document.addEventListener('DOMContentLoaded', () => {
    initLogout();
    loadDashboard();

    // 60초마다 자동 새로고침 (비활성 탭에서는 건너뜀)
    setInterval(() => {
        if (!document.hidden) {
            loadDashboard();
        }
    }, REFRESH_INTERVAL);
});
