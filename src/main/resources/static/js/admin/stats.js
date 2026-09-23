/**
 * 신규 가입 추이 페이지
 * - 단위 탭(DAY/WEEK/MONTH) 전환 시 API 재조회 후 차트·표 갱신
 * - MONTH 탭에서만 시작/종료 월(최대 12개월)을 지정해 조회
 */
document.addEventListener('DOMContentLoaded', function() {
    const tabs = document.querySelectorAll('.unit-tab');
    const totalCountEl = document.getElementById('totalCount');
    const tableBody = document.getElementById('trendTableBody');
    const errorEl = document.getElementById('errorMessage');
    const monthRangeForm = document.getElementById('monthRangeForm');
    const fromMonthInput = document.getElementById('fromMonth');
    const toMonthInput = document.getElementById('toMonth');
    let chart = null;

    // yyyy-MM 문자열 (브라우저 로컬 시간 기준)
    function formatMonth(date) {
        return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    }

    // 기본 기간: 이번 달 포함 최근 12개월, 이번 달 이후는 선택 불가
    function initMonthRange() {
        const now = new Date();
        const currentMonth = formatMonth(now);
        fromMonthInput.value = formatMonth(new Date(now.getFullYear(), now.getMonth() - 11, 1));
        toMonthInput.value = currentMonth;
        fromMonthInput.max = currentMonth;
        toMonthInput.max = currentMonth;
    }

    function setActiveTab(unit) {
        tabs.forEach(tab => {
            const active = tab.dataset.unit === unit;
            tab.classList.toggle('bg-primary', active);
            tab.classList.toggle('text-white', active);
            tab.setAttribute('aria-selected', String(active));
        });
    }

    // 월 단위는 yyyy-MM, 그 외는 yyyy-MM-dd로 표시
    function formatPeriod(periodStart, unit) {
        return unit === 'MONTH' ? periodStart.slice(0, 7) : periodStart;
    }

    function renderChart(points, unit) {
        const labels = points.map(p => formatPeriod(p.periodStart, unit));
        const data = points.map(p => p.count);
        if (chart) {
            chart.data.labels = labels;
            chart.data.datasets[0].data = data;
            chart.update();
            return;
        }
        chart = new Chart(document.getElementById('signupChart'), {
            type: 'line',
            data: {labels, datasets: [{label: '신규 가입', data, tension: 0.2, fill: false}]},
            options: {
                maintainAspectRatio: false,
                plugins: {legend: {display: false}},
                scales: {y: {beginAtZero: true, ticks: {precision: 0}}}
            }
        });
    }

    function renderTable(points, unit) {
        tableBody.innerHTML = '';
        [...points].reverse().forEach(p => {
            const tr = document.createElement('tr');
            tr.className = 'border-b border-border-color';
            const dateTd = document.createElement('td');
            dateTd.className = 'px-lg py-md';
            dateTd.textContent = formatPeriod(p.periodStart, unit);
            const countTd = document.createElement('td');
            countTd.className = 'px-lg py-md text-right';
            countTd.textContent = p.count;
            tr.append(dateTd, countTd);
            tableBody.appendChild(tr);
        });
    }

    async function load(unit) {
        setActiveTab(unit);
        monthRangeForm.classList.toggle('hidden', unit !== 'MONTH');
        errorEl.classList.add('hidden');
        try {
            const response = unit === 'MONTH'
                ? await API.getSignupTrend(unit, fromMonthInput.value, toMonthInput.value)
                : await API.getSignupTrend(unit);
            totalCountEl.textContent = response.totalCount;
            renderChart(response.points, response.unit);
            renderTable(response.points, response.unit);
        } catch (error) {
            errorEl.textContent = error.message || '가입 추이를 불러오지 못했습니다.';
            errorEl.classList.remove('hidden');
        }
    }

    monthRangeForm.addEventListener('submit', event => {
        event.preventDefault();
        load('MONTH');
    });
    tabs.forEach(tab => tab.addEventListener('click', () => load(tab.dataset.unit)));
    initMonthRange();
    load('DAY');
});
