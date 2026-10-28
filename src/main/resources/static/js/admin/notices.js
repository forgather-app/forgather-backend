document.addEventListener('DOMContentLoaded', function () {
    const element = id => document.getElementById(id);
    const tableBody = element('noticesTableBody');
    const createModal = element('createModal');
    const detailModal = element('detailModal');
    const form = element('noticeForm');
    const submitButton = element('submitNoticeBtn');
    const titleInput = element('noticeTitle');
    const bodyInput = element('noticeBody');
    const textSegmenter = new Intl.Segmenter('ko', {granularity: 'grapheme'});
    let currentPage = 1;
    let listRequest = 0;
    let detailRequest = 0;
    let currentNoticeId = null;
    let isSubmitting = false;
    let formMode = 'create';
    let editingNoticeId = null;
    let loadedNotice = null;
    let toastTimeout;

    function updateCharacterCount(input, countId, warningId, limit) {
        const count = Array.from(textSegmenter.segment(input.value)).length;
        const isOverLimit = count > limit;
        const counter = element(countId);
        counter.textContent = `${count.toLocaleString('ko-KR')}/${limit.toLocaleString('ko-KR')}`;
        counter.classList.toggle('notice-limit-exceeded', isOverLimit);
        element(warningId).hidden = !isOverLimit;
        input.setAttribute('aria-invalid', String(isOverLimit));
    }

    function updateCharacterCounts() {
        updateCharacterCount(titleInput, 'titleCount', 'titleWarning', 100);
        updateCharacterCount(bodyInput, 'bodyCount', 'bodyWarning', 10000);
    }

    titleInput.addEventListener('input', updateCharacterCounts);
    bodyInput.addEventListener('input', updateCharacterCounts);

    function formatDate(value) {
        return value ? new Date(value).toLocaleString('ko-KR') : '-';
    }

    function showToast(message) {
        const toast = element('toast');
        clearTimeout(toastTimeout);
        toast.textContent = message;
        toast.className = 'toast success show';
        toastTimeout = setTimeout(() => toast.classList.remove('show'), 3000);
    }

    function openModal(modal) {
        modal.showModal();
        document.body.classList.add('modal-open');
    }

    function closeModal(modal) {
        if (isSubmitting) return;
        modal.close();
    }

    [createModal, detailModal].forEach(modal => {
        modal.addEventListener('close', () => {
            if (modal === detailModal) {
                detailRequest++;
                currentNoticeId = null;
                loadedNotice = null;
                updateActions();
            }
            if (!createModal.open && !detailModal.open) document.body.classList.remove('modal-open');
        });
        modal.addEventListener('click', event => {
            if (event.target !== modal) return;
            const bounds = modal.getBoundingClientRect();
            if (event.clientX < bounds.left || event.clientX > bounds.right ||
                event.clientY < bounds.top || event.clientY > bounds.bottom) closeModal(modal);
        });
        modal.addEventListener('cancel', event => {
            if (isSubmitting) event.preventDefault();
        });
    });
    document.querySelectorAll('[data-close]').forEach(button => {
        button.addEventListener('click', () => closeModal(element(button.dataset.close)));
    });

    function renderNotices(notices) {
        tableBody.replaceChildren();
        notices.forEach(notice => {
            const row = document.createElement('tr');
            const titleCell = document.createElement('td');
            titleCell.className = 'px-lg py-md';
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'notice-title-button notice-text';
            button.dataset.noticeId = notice.id;
            button.textContent = notice.title;
            titleCell.appendChild(button);
            const dateCell = document.createElement('td');
            dateCell.className = 'px-lg py-md text-sm text-text-secondary';
            dateCell.textContent = formatDate(notice.createdAt);
            row.append(titleCell, dateCell);
            tableBody.appendChild(row);
        });
        element('emptyMessage').hidden = notices.length !== 0;
    }

    async function loadNotices(page = 1, canCorrectPage = false) {
        const request = ++listRequest;
        currentPage = page;
        element('listLoading').hidden = false;
        element('listError').hidden = true;
        element('emptyMessage').hidden = true;
        tableBody.replaceChildren();
        element('totalCountInfo').textContent = '';
        try {
            const result = await API.getNotices(page);
            if (request !== listRequest) return;
            if (canCorrectPage && page > Math.max(1, result.totalPages)) {
                return await loadNotices(Math.max(1, result.totalPages));
            }
            currentPage = result.currentPage;
            renderNotices(result.notices);
            element('totalCountInfo').textContent = `전체 ${result.totalCount.toLocaleString()}개`;
            PaginationUtil.render(element('pagination'), result.currentPage, result.totalPages,
                result.totalCount, pageNumber => {
                    if (!isSubmitting) loadNotices(pageNumber);
                });
        } catch (error) {
            if (request !== listRequest) return;
            element('listErrorMessage').textContent = error.message || '공지를 불러오지 못했습니다.';
            element('listError').hidden = false;
        } finally {
            if (request === listRequest) element('listLoading').hidden = true;
        }
    }

    function updateActions() {
        element('editNoticeBtn').disabled = isSubmitting || !loadedNotice;
        element('deleteNoticeBtn').disabled = isSubmitting || !loadedNotice;
    }

    function setSubmitting(isBusy) {
        isSubmitting = isBusy;
        submitButton.disabled = isBusy;
        titleInput.disabled = isBusy;
        bodyInput.disabled = isBusy;
        ['openCreateBtn', 'retryListBtn', 'retryDetailBtn'].forEach(id => {
            element(id).disabled = isBusy;
        });
        document.querySelectorAll('[data-close]').forEach(button => {
            button.disabled = isBusy;
        });
        updateActions();
    }

    function openForm(notice = null) {
        if (isSubmitting) return;
        formMode = notice ? 'edit' : 'create';
        editingNoticeId = notice ? notice.id : null;
        form.reset();
        titleInput.value = notice ? notice.title : '';
        bodyInput.value = notice ? notice.body : '';
        const label = notice ? '공지 수정' : '공지 작성';
        element('createModalTitle').textContent = label;
        element('closeNoticeFormBtn').setAttribute('aria-label', `${label} 닫기`);
        submitButton.textContent = notice ? '수정 저장' : '등록';
        element('createError').hidden = true;
        updateCharacterCounts();
        openModal(createModal);
    }

    function renderDetail(notice) {
        loadedNotice = notice;
        currentNoticeId = notice.id;
        element('detailTitle').textContent = notice.title;
        element('detailBody').textContent = notice.body;
        element('detailDate').textContent = `등록일: ${formatDate(notice.createdAt)}`;
        element('detailLoading').hidden = true;
        element('detailError').hidden = true;
        element('detailContent').hidden = false;
        element('detailActionError').hidden = true;
        updateActions();
    }

    async function loadDetail(noticeId) {
        if (isSubmitting) return;
        loadedNotice = null;
        updateActions();
        element('detailActionError').hidden = true;
        const request = ++detailRequest;
        currentNoticeId = noticeId;
        if (!detailModal.open) openModal(detailModal);
        element('detailContent').hidden = true;
        element('detailError').hidden = true;
        element('detailLoading').hidden = false;
        try {
            const notice = await API.getNoticeDetail(noticeId);
            if (request !== detailRequest || !detailModal.open) return;
            renderDetail(notice);
        } catch (error) {
            if (request !== detailRequest || !detailModal.open) return;
            element('detailErrorMessage').textContent = error.message || '공지를 불러오지 못했습니다.';
            element('detailError').hidden = false;
        } finally {
            if (request === detailRequest) element('detailLoading').hidden = true;
        }
    }

    tableBody.addEventListener('click', event => {
        const button = event.target.closest('[data-notice-id]');
        if (button && !isSubmitting) loadDetail(button.dataset.noticeId);
    });
    element('retryListBtn').addEventListener('click', () => {
        if (!isSubmitting) loadNotices(currentPage);
    });
    element('retryDetailBtn').addEventListener('click', () => loadDetail(currentNoticeId));
    element('openCreateBtn').addEventListener('click', () => {
        openForm();
    });
    element('logoutBtn').addEventListener('click', () => Auth.logout());

    element('editNoticeBtn').addEventListener('click', () => {
        if (loadedNotice && !isSubmitting) openForm(loadedNotice);
    });
    element('deleteNoticeBtn').addEventListener('click', async () => {
        if (isSubmitting || !loadedNotice) return;
        if (!window.confirm('이 공지를 삭제하시겠습니까?')) return;
        const noticeId = loadedNotice.id;
        setSubmitting(true);
        detailRequest++;
        element('detailActionError').hidden = true;
        try {
            await API.deleteNotice(noticeId);
            loadedNotice = null;
            currentNoticeId = null;
            detailModal.close();
            await loadNotices(currentPage, true);
            showToast('공지가 삭제되었습니다.');
        } catch (error) {
            element('detailActionError').textContent = error.message || '공지 삭제에 실패했습니다. 다시 시도해주세요.';
            element('detailActionError').hidden = false;
        } finally {
            setSubmitting(false);
        }
    });

    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (isSubmitting) return;
        const isEditing = formMode === 'edit';
        const noticeId = editingNoticeId;
        setSubmitting(true);
        detailRequest++;
        submitButton.textContent = isEditing ? '저장 중...' : '등록 중...';
        element('createError').hidden = true;
        try {
            const notice = isEditing
                ? await API.updateNotice(noticeId, titleInput.value, bodyInput.value)
                : await API.createNotice(titleInput.value, bodyInput.value);
            form.reset();
            editingNoticeId = null;
            updateCharacterCounts();
            createModal.close();
            await loadNotices(isEditing ? currentPage : 1);
            renderDetail(notice);
            if (!detailModal.open) openModal(detailModal);
            showToast(isEditing ? '공지가 수정되었습니다.' : '공지가 등록되었습니다.');
        } catch (error) {
            element('createError').textContent = error.message ||
                (isEditing ? '공지 수정에 실패했습니다. 다시 시도해주세요.' : '공지 등록에 실패했습니다. 다시 시도해주세요.');
            element('createError').hidden = false;
        } finally {
            setSubmitting(false);
            submitButton.textContent = isEditing ? '수정 저장' : '등록';
        }
    });

    updateCharacterCounts();
    loadNotices();
});
