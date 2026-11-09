document.addEventListener('DOMContentLoaded', () => {
    const element = id => document.getElementById(id);
    const modal = element('detailModal');
    let currentPage = 1;
    let currentId;
    let listRequest = 0;
    let detailRequest = 0;
    const stages = {PDF: 'PDF', EMAIL: '이메일', UNKNOWN: '알 수 없음'};
    const statuses = {PENDING: '대기', IN_PROGRESS: '조사 중', RESOLVED: '해결', IGNORED: '무시'};

    function formatDate(value) {
        if (!value) return '-';
        // 서버의 LocalDateTime은 UTC이므로 시간대를 명시한 뒤 현지 시간으로 변환한다.
        return new Date(`${value}Z`).toLocaleString('ko-KR', {
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
        });
    }

    function createCopyButton(label, text, statusId) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'copy-icon-button';
        button.setAttribute('aria-label', `${label} 복사`);
        button.title = `${label} 복사`;
        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '0 0 24 24');
        svg.setAttribute('aria-hidden', 'true');
        const back = document.createElementNS('http://www.w3.org/2000/svg', 'path');
        back.setAttribute('d', 'M8 16H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v3');
        const front = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
        front.setAttribute('x', '8');
        front.setAttribute('y', '8');
        front.setAttribute('width', '13');
        front.setAttribute('height', '13');
        front.setAttribute('rx', '2');
        svg.append(back, front);
        button.appendChild(svg);
        button.addEventListener('click', async () => {
            try {
                await navigator.clipboard.writeText(text);
                element(statusId).textContent = `${label} 복사 완료`;
            } catch {
                element(statusId).textContent = '복사하지 못했습니다. 내용을 선택해 직접 복사해주세요.';
            }
        });
        return button;
    }

    async function loadList(page = 1) {
        const request = ++listRequest;
        currentPage = page;
        element('listLoading').hidden = false;
        element('listError').hidden = true;
        element('emptyMessage').hidden = true;
        element('failuresTableBody').replaceChildren();
        element('totalCountInfo').textContent = '';
        element('listCopyStatus').textContent = '';
        element('pagination').style.display = 'none';
        try {
            const result = await API.get('/pdf/failures', {page, size: 15});
            if (request !== listRequest) return;
            currentPage = result.currentPage;
            result.failures.forEach(failure => {
                const row = document.createElement('tr');
                [formatDate(failure.failedAt), failure.jobId, failure.requestId, stages[failure.stage] ?? failure.stage,
                    failure.errorType, statuses[failure.processingStatus] ?? failure.processingStatus]
                    .forEach((value, index) => {
                        const cell = document.createElement('td');
                        cell.textContent = value ?? '-';
                        if ((index === 1 || index === 2) && value != null) {
                            const label = index === 1 ? '작업 ID' : '요청 ID';
                            cell.appendChild(createCopyButton(label, String(value), 'listCopyStatus'));
                        }
                        row.appendChild(cell);
                    });
                const cell = document.createElement('td');
                const button = document.createElement('button');
                button.type = 'button';
                button.className = 'failure-button';
                button.textContent = '상세';
                button.dataset.id = failure.id;
                button.setAttribute('aria-label', `실패 ${failure.id} 상세 보기`);
                cell.appendChild(button);
                row.appendChild(cell);
                element('failuresTableBody').appendChild(row);
            });
            element('emptyMessage').hidden = result.failures.length !== 0;
            element('totalCountInfo').textContent = `전체 ${result.totalCount.toLocaleString()}개`;
            PaginationUtil.render(element('pagination'), result.currentPage, result.totalPages,
                result.totalCount, loadList);
        } catch (error) {
            if (request !== listRequest) return;
            element('listErrorMessage').textContent = error.message || '목록을 불러오지 못했습니다.';
            element('listError').hidden = false;
        } finally {
            if (request === listRequest) element('listLoading').hidden = true;
        }
    }

    async function loadDetail(id) {
        const request = ++detailRequest;
        currentId = id;
        element('detailLoading').hidden = false;
        element('detailError').hidden = true;
        element('detailContent').hidden = true;
        element('detailContent').replaceChildren();
        element('copyStatus').textContent = '';
        if (!modal.open) modal.showModal();
        try {
            const detail = await API.get(`/pdf/failures/${encodeURIComponent(id)}`);
            if (request !== detailRequest) return;
            let rawEvent = detail.rawEvent;
            try {
                rawEvent = JSON.stringify(JSON.parse(rawEvent), null, 2);
            } catch {
                // JSON 파싱이 불가능하면 저장된 원문을 그대로 표시한다.
            }
            const fields = [
                ['실패 시각', formatDate(detail.failedAt)],
                ['실패 단계', stages[detail.stage] ?? detail.stage],
                ['오류 유형', detail.errorType],
                ['오류 메시지', detail.errorMessage],
                ['실패 조건', detail.failureCondition],
                ['호출 횟수', detail.approximateInvokeCount],
                ['작업 ID', detail.jobId],
                ['요청 ID', detail.requestId],
                ['함수 ARN', detail.functionArn],
                ['원본 실패 이벤트', rawEvent],
                ['처리 상태', statuses[detail.processingStatus] ?? detail.processingStatus],
                ['처리 메모', detail.processingNote],
                ['저장 시각', formatDate(detail.createdAt)],
                ['실패 ID', detail.id]
            ];
            fields.forEach(([label, value]) => {
                const isBlock = ['오류 메시지', '원본 실패 이벤트'].includes(label);
                const hasInlineCopy = ['작업 ID', '함수 ARN', '요청 ID'].includes(label);
                const group = document.createElement('div');
                group.className = isBlock ? 'detail-block' : 'detail-field';
                const term = document.createElement('dt');
                term.textContent = label;
                const description = document.createElement('dd');
                const text = String(value ?? '-');
                const content = document.createElement('span');
                content.textContent = text;
                description.appendChild(content);
                if (isBlock) {
                    description.tabIndex = 0;
                    description.setAttribute('aria-label', label);
                }
                if (isBlock || hasInlineCopy) {
                    const copyButton = createCopyButton(label, text, 'copyStatus');
                    (isBlock ? term : description).appendChild(copyButton);
                }
                group.append(term, description);
                element('detailContent').appendChild(group);
            });
            element('detailContent').hidden = false;
        } catch (error) {
            if (request !== detailRequest) return;
            element('detailErrorMessage').textContent = error.message || '상세 정보를 불러오지 못했습니다.';
            element('detailError').hidden = false;
        } finally {
            if (request === detailRequest) element('detailLoading').hidden = true;
        }
    }

    element('failuresTableBody').addEventListener('click', event => {
        const button = event.target.closest('button[data-id]');
        if (button) loadDetail(button.dataset.id);
    });
    element('retryListBtn').addEventListener('click', () => loadList(currentPage));
    element('retryDetailBtn').addEventListener('click', () => loadDetail(currentId));
    element('closeDetailBtn').addEventListener('click', () => modal.close());
    modal.addEventListener('close', () => detailRequest++);
    element('logoutBtn').addEventListener('click', () => Auth.logout());
    loadList();
});
