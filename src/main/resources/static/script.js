let selectedUploadFile = null;
let previewRows = [];
let currentMonth = new Date().getMonth() + 1; // 1~12
let currentYear = new Date().getFullYear();
let currentProvider = 'TOSS';
let previewMiscOnly = false;
const previewSelectedIndices = new Set();
let previewDateFilterStart = '';
let previewDateFilterEnd = '';

let currentUserRole = '';

const CATEGORY_OPTIONS_EXPENSE = [
    '식비',
    '편의점/마트',
    '카페',
    '배달',
    '교통',
    '주유',
    '주거/관리',
    '공과금',
    '통신',
    '보험',
    '의료',
    '교육',
    '쇼핑',
    '문화/여가',
    '구독',
    '수수료',
    '이체/송금',
    '환급/리워드',
    '수입',
    '기타'
];

function escapeAttr(v) {
    return String(v ?? '').replace(/&/g, '&amp;')
        .replace(/"/g, '&quot;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
}

function getUploadFileState(provider, year, month) {
    const p = String(provider || '').trim().toUpperCase();
    const y = Number(year);
    const m = Number(month);
    if (!p || !Number.isFinite(y) || !Number.isFinite(m)) return null;
    return uploadFileState?.[p]?.[y]?.[m] || null;
}

function setUploadFileState(provider, year, month, file) {
    const p = String(provider || '').trim().toUpperCase();
    const y = Number(year);
    const m = Number(month);
    if (!p || !Number.isFinite(y) || !Number.isFinite(m) || !file) return;
    if (!uploadFileState[p]) uploadFileState[p] = {};
    if (!uploadFileState[p][y]) uploadFileState[p][y] = {};
    uploadFileState[p][y][m] = { file, name: file?.name || '' };
}

function clearUploadFileState(provider, year, month) {
    const p = String(provider || '').trim().toUpperCase();
    const y = Number(year);
    const m = Number(month);
    if (!p || !Number.isFinite(y) || !Number.isFinite(m)) return;
    if (uploadFileState?.[p]?.[y]) {
        delete uploadFileState[p][y][m];
    }
}

function getPreviewMiscOnly() {
    return !!previewMiscOnly;
}

function setPreviewMiscOnly(next) {
    previewMiscOnly = !!next;
}

function setPreviewDateFilter(start, end) {
    previewDateFilterStart = String(start || '').trim();
    previewDateFilterEnd = String(end || '').trim();
}

function clearPreviewDateFilter() {
    previewDateFilterStart = '';
    previewDateFilterEnd = '';
}

function updatePreviewSelectedCount() {
    const el = document.getElementById('preview-selected-count');
    if (!el) return;
    const count = previewSelectedIndices.size;
    el.textContent = count ? `선택 ${count}건` : '';
}

function clearPreviewSelection() {
    previewSelectedIndices.clear();
    updatePreviewSelectedCount();
    const selectAll = document.getElementById('preview-select-all');
    if (selectAll) selectAll.checked = false;
}

function initPreviewBulkControls() {
    const bulkSelect = document.getElementById('preview-bulk-category');
    if (bulkSelect) {
        bulkSelect.innerHTML = renderCategoryOptions('');
    }

    const miscOnlyEl = document.getElementById('preview-filter-misc-only');
    if (miscOnlyEl && !miscOnlyEl.dataset.bound) {
        miscOnlyEl.dataset.bound = '1';
        miscOnlyEl.addEventListener('change', () => {
            if (isCurrentMonthConfirmed) {
                miscOnlyEl.checked = !miscOnlyEl.checked;
                showToast('카테고리 변경은 확정 전만 가능합니다.', 'error');
                return;
            }
            setPreviewMiscOnly(!!miscOnlyEl.checked);
            previewCurrentPage = 1;
            renderPreviewTable(previewRows);
        });
    }

    const applyBtn = document.getElementById('preview-bulk-apply-btn');
    if (applyBtn && !applyBtn.dataset.bound) {
        applyBtn.dataset.bound = '1';
        applyBtn.addEventListener('click', () => {
            if (isCurrentMonthConfirmed) {
                showToast('카테고리 변경은 확정 전만 가능합니다.', 'error');
                return;
            }
            const cat = bulkSelect ? String(bulkSelect.value || '').trim() : '';
            if (!cat) {
                showToast('카테고리를 선택해주세요.', 'error');
                return;
            }
            if (!previewSelectedIndices.size) {
                showToast('선택된 항목이 없습니다.', 'error');
                return;
            }
            previewSelectedIndices.forEach(idx => {
                const r = previewRows[idx];
                if (!r) return;
                r.category = cat;
            });
            clearPreviewSelection();
            renderPreviewTable(previewRows);
            showToast('일괄 적용 완료', 'success');
        });
    }

    const clearBtn = document.getElementById('preview-bulk-clear-btn');
    if (clearBtn && !clearBtn.dataset.bound) {
        clearBtn.dataset.bound = '1';
        clearBtn.addEventListener('click', () => {
            if (isCurrentMonthConfirmed) {
                showToast('카테고리 변경은 확정 전만 가능합니다.', 'error');
                return;
            }
            clearPreviewSelection();
            renderPreviewTable(previewRows);
        });
    }

    const selectAll = document.getElementById('preview-select-all');
    if (selectAll && !selectAll.dataset.bound) {
        selectAll.dataset.bound = '1';
        selectAll.addEventListener('change', () => {
            const checked = !!selectAll.checked;
            const currentPage = Array.isArray(window.__previewVisiblePageIndices) ? window.__previewVisiblePageIndices : [];
            currentPage.forEach(idx => {
                if (checked) {
                    previewSelectedIndices.add(idx);
                } else {
                    previewSelectedIndices.delete(idx);
                }
            });
            updatePreviewSelectedCount();
            renderPreviewTable(previewRows);
        });
    }

    updatePreviewSelectedCount();
}

function renderCategoryOptions(selected) {
    const s = String(selected ?? '').trim();
    const selectedSet = new Set(CATEGORY_OPTIONS_EXPENSE);
    const list = s && !selectedSet.has(s) ? [...CATEGORY_OPTIONS_EXPENSE, s] : CATEGORY_OPTIONS_EXPENSE;
    const selectedEsc = escapeAttr(s);
    return `<option value="">선택</option>` + list.map(name => {
        const val = escapeAttr(name);
        const isSel = (name === s) ? 'selected' : '';
        return `<option value="${val}" ${isSel}>${escapeHtml(name)}</option>`;
    }).join('');
}

function suggestCategoryForRow(row) {
    const txType = String(row?.txType || '').toLowerCase();
    const text = `${row?.description || ''} ${row?.txDetail || ''}`.toLowerCase();

    // 현금흐름 성격 우선
    if (txType.includes('캐시백') || txType.includes('포인트') || text.includes('캐시백') || text.includes('포인트')) {
        return '환급/리워드';
    }
    if (txType.includes('이체') || txType.includes('송금') || text.includes('이체') || text.includes('송금') || text.includes('계좌이체')) {
        return '이체/송금';
    }
    if (txType.includes('수수료') || text.includes('수수료')) {
        return '수수료';
    }

    // 가맹점/키워드
    if (/(gs25|cu|세븐일레븐|이마트24|미니스톱|emart|이마트|홈플러스|롯데마트|코스트코)/.test(text)) {
        return '편의점/마트';
    }
    if (/(스타벅스|투썸|이디야|커피빈|폴바셋|메가커피|컴포즈|빽다방|카페)/.test(text)) {
        return '카페';
    }
    if (/(배달의민족|요기요|쿠팡이츠|배민|배달)/.test(text)) {
        return '배달';
    }
    if (/(택시|카카오t|카카오택시|지하철|버스|교통)/.test(text)) {
        return '교통';
    }
    if (/(sk에너지|gs칼텍스|s-oil|현대오일뱅크|주유)/.test(text)) {
        return '주유';
    }
    if (/(넷플릭스|유튜브|youtube|spotify|왓챠|티빙|웨이브|쿠팡플레이|디즈니|구독|멤버십)/.test(text)) {
        return '구독';
    }

    // txType 기반(식비/결제류)
    if (txType.includes('체크카드') || txType.includes('카드') || txType.includes('결제')) {
        return '식비';
    }

    const amt = typeof row?.amount === 'number' ? row.amount : Number(row?.amount);
    if (Number.isFinite(amt) && amt > 0) {
        return '수입';
    }

    return '기타';
}

function shouldAutoSuggestCategory(currentCategory) {
    const c = String(currentCategory ?? '').trim();
    if (!c) return true;
    // backend may fill generic placeholders; allow overriding those
    const generic = new Set(['기타', '카드결제', '체크카드결제']);
    return generic.has(c);
}
let bankMonthState = {}; // { [provider:string]: { [month:number]: { fileName: string, rows: any[], errorMessage?: string } } }
let uploadFileState = {}; // { [provider:string]: { [year:number]: { [month:number]: { file: File, name: string } } } }

let lastAuthPromptAt = 0;
function promptLoginOnce() {
    const now = Date.now();
    // Always show a visible hint (toast + open modal), but debounce the blocking alert.
    try {
        showToast('로그인이 필요합니다.', 'error');
    } catch {
        // ignore
    }
    try {
        openAuthModal('login');
    } catch {
        // ignore
    }
    if (now - lastAuthPromptAt < 1500) {
        return;
    }
    lastAuthPromptAt = now;
}

function initFixedExpenseAutoMonthSelectors(yearSel, monthSel) {
    if (!yearSel || !monthSel) return;
    if (yearSel.options && yearSel.options.length > 0 && monthSel.options && monthSel.options.length > 0) return;

    const now = new Date();
    const curYear = now.getFullYear();
    const curMonth = now.getMonth() + 1;

    const prevYear = yearSel.value;
    const prevMonth = monthSel.value;

    yearSel.innerHTML = '';
    for (let y = curYear - 2; y <= curYear + 1; y++) {
        const opt = document.createElement('option');
        opt.value = String(y);
        opt.textContent = String(y);
        if (String(y) === String(prevYear || curYear)) opt.selected = true;
        yearSel.appendChild(opt);
    }

    monthSel.innerHTML = '';
    for (let m = 1; m <= 12; m++) {
        const opt = document.createElement('option');
        opt.value = String(m);
        opt.textContent = `${m}월`;
        if (String(m) === String(prevMonth || curMonth)) opt.selected = true;
        monthSel.appendChild(opt);
    }
}

async function loadFixedExpenseAutoMonthTransactions(year, month, summaryEl) {
    const table = document.getElementById('fixed-expense-auto-table');
    if (!table) return;
    const tbody = table.querySelector('tbody');
    if (!tbody) return;

    try {
        if (summaryEl) {
            summaryEl.textContent = `${year}년 ${month}월: 불러오는 중...`;
        }
        const r = await apiFetch(`/api/fixed-expenses/auto/transactions?year=${encodeURIComponent(year)}&month=${encodeURIComponent(month)}`);
        const d = await r.json();
        if (!r.ok || !d?.success) {
            throw new Error(d?.error || '내역을 불러오지 못했습니다.');
        }

        const items = Array.isArray(d.items) ? d.items : [];
        const unconfirmed = Number(d.unconfirmedCount ?? 0);
        const confirmed = Number(d.confirmedCount ?? 0);

        fixedExpenseMonthLocked = confirmed > 0;
        renderFixedExpenseTable(fixedExpenseItems);

        applyFixedExpenseAutoButtonState({ total: items.length, unconfirmed, confirmed });

        if (summaryEl) {
            summaryEl.textContent = `${year}년 ${month}월: 총 ${items.length}건 (미확정 ${unconfirmed} / 확정 ${confirmed})`;
        }

        tbody.innerHTML = '';
        if (items.length === 0) {
            const tr = document.createElement('tr');
            tr.className = 'empty-row';
            tr.innerHTML = '<td colspan="5">선택한 월의 고정지출 생성 내역이 없습니다.</td>';
            tbody.appendChild(tr);
            return;
        }

        for (const it of items) {
            const tr = document.createElement('tr');
            const amt = typeof it.amount === 'number' ? it.amount : null;
            const absAmt = amt == null ? '' : formatNumber(Math.abs(amt));
            const conf = String(it.confirmed || 'N').toUpperCase() === 'Y' ? 'Y' : 'N';
            tr.innerHTML = `
                <td>${it.date || ''}</td>
                <td>${escapeHtml(it.description || '')}</td>
                <td>${escapeHtml(it.category || '')}</td>
                <td style="text-align:right;">${absAmt}</td>
                <td style="text-align:center;">${conf}</td>
            `;
            tbody.appendChild(tr);
        }
    } catch (err) {
        if (String(err?.message || '') === 'UNAUTHORIZED') return;
        fixedExpenseMonthLocked = false;
        renderFixedExpenseTable(fixedExpenseItems);
        applyFixedExpenseAutoButtonState({ total: 0, unconfirmed: 0, confirmed: 0 });
        const msg = err?.message ? String(err.message) : '내역을 불러오지 못했습니다.';
        if (summaryEl) summaryEl.textContent = `${year}년 ${month}월: ${msg}`;
        tbody.innerHTML = `<tr class="empty-row"><td colspan="5">${escapeHtml(msg)}</td></tr>`;
    }
}

function applyFixedExpenseAutoButtonState(state) {
    const genBtn = document.getElementById('fixed-expense-auto-generate-btn');
    const confirmBtn = document.getElementById('fixed-expense-auto-confirm-btn');
    const unconfirmBtn = document.getElementById('fixed-expense-auto-unconfirm-btn');
    if (!genBtn || !confirmBtn || !unconfirmBtn) return;

    const total = Number(state?.total ?? 0);
    const unconfirmed = Number(state?.unconfirmed ?? 0);
    const confirmed = Number(state?.confirmed ?? 0);

    // C안 규칙
    // - total==0: 생성만 가능
    // - unconfirmed>0: 확정 가능
    // - confirmed>0: 확정 취소 가능
    genBtn.disabled = !(total === 0 || unconfirmed > 0) ? true : false;
    confirmBtn.disabled = !(unconfirmed > 0);
    unconfirmBtn.disabled = !(confirmed > 0);
}

function escapeHtml(str) {
    return String(str)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

function formatNumber(value) {
    const n = Number(value);
    if (!Number.isFinite(n)) return '';
    return n.toLocaleString('ko-KR');
}

let toastTimer = null;
function showToast(message, type) {
    const el = document.getElementById('toast');
    if (!el) return;
    if (toastTimer) {
        clearTimeout(toastTimer);
        toastTimer = null;
    }
    el.className = `toast${type ? ' toast-' + type : ''}`;
    el.textContent = message || '';
    el.style.display = '';
    toastTimer = setTimeout(() => {
        el.style.display = 'none';
    }, 2400);
}

function setButtonLoading(btn, loadingText) {
    if (!btn) return () => {};
    const prevDisabled = btn.disabled;
    const prevText = btn.textContent;
    btn.disabled = true;
    if (loadingText) btn.textContent = loadingText;
    return () => {
        btn.disabled = prevDisabled;
        btn.textContent = prevText;
    };
}

function setupDashboardTabs() {
    const container = document.getElementById('dashboard-tabs');
    if (!container) return;
    const tabs = container.querySelectorAll('.dashboard-tab');
    if (!tabs.length) return;
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const key = tab.getAttribute('data-tab');
            if (!key) return;
            tabs.forEach(t => t.classList.toggle('active', t === tab));
            const panels = document.querySelectorAll('#dashboard-content .dashboard-tab-content');
            panels.forEach(panel => {
                panel.classList.toggle('active', panel.getAttribute('data-tab-panel') === key);
            });
            if (key === 'calendar') {
                loadDashboardCalendar();
            }
        });
    });
}

function setupFixedItemTabs() {
    const container = document.getElementById('fixed-item-tabs');
    if (!container) return;
    const tabs = container.querySelectorAll('.panel-tab');
    if (!tabs.length) return;

    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const key = tab.getAttribute('data-tab');
            if (!key) return;
            tabs.forEach(t => t.classList.toggle('active', t === tab));
            const panels = document.querySelectorAll('#fixed-expense-content .panel-tab-content');
            panels.forEach(panel => {
                panel.classList.toggle('active', panel.getAttribute('data-tab-panel') === key);
            });
        });
    });
}

function setupDashboardCalendarNav() {
    const prevBtn = document.getElementById('calendar-prev');
    const nextBtn = document.getElementById('calendar-next');
    const yearEl = document.getElementById('dashboard-year');
    const monthEl = document.getElementById('dashboard-month');
    if (!prevBtn || !nextBtn || !yearEl || !monthEl) return;

    const shift = (delta) => {
        let y = Number(yearEl.value);
        let m = Number(monthEl.value);
        if (!Number.isFinite(y)) return;
        if (!Number.isFinite(m) || m === 0) {
            m = currentMonth;
        }
        m += delta;
        if (m < 1) {
            m = 12;
            y -= 1;
        } else if (m > 12) {
            m = 1;
            y += 1;
        }

        yearEl.value = String(y);
        monthEl.value = String(m);
        loadDashboardCalendar();
    };

    prevBtn.addEventListener('click', () => shift(-1));
    nextBtn.addEventListener('click', () => shift(1));
}

async function loadDashboardCalendar() {
    const yearEl = document.getElementById('dashboard-year');
    const monthEl = document.getElementById('dashboard-month');
    const providerEl = document.getElementById('dashboard-provider');
    const titleEl = document.getElementById('calendar-title');
    const gridEl = document.getElementById('dashboard-calendar-grid');
    const hintEl = document.getElementById('calendar-hint');
    if (!yearEl || !monthEl || !providerEl || !titleEl || !gridEl || !hintEl) return;

    const year = Number(yearEl.value);
    const month = Number(monthEl.value);
    const provider = String(providerEl.value || 'ALL').trim().toUpperCase();

    titleEl.textContent = Number.isFinite(year) && Number.isFinite(month) && month >= 1 && month <= 12
            ? `${year}년 ${month}월`
            : '-';

    if (!Number.isFinite(year) || !Number.isFinite(month) || month < 1 || month > 12) {
        gridEl.innerHTML = '';
        hintEl.style.display = '';
        hintEl.textContent = '월(상세)을 선택하면 달력 집계를 볼 수 있습니다.';
        return;
    }

    hintEl.style.display = 'none';
    try {
        const res = await apiFetch(`/api/dashboard/daily?year=${encodeURIComponent(year)}&month=${encodeURIComponent(month)}&provider=${encodeURIComponent(provider)}`);
        const data = await res.json();
        if (!res.ok || !data?.success) {
            throw new Error(data?.error || '달력 데이터를 불러오지 못했습니다.');
        }
        renderDashboardCalendar(data);
    } catch (e) {
        if (String(e?.message || '') === 'UNAUTHORIZED') return;
        gridEl.innerHTML = '';
        hintEl.style.display = '';
        hintEl.textContent = e?.message || '달력 로딩 실패';
    }
}

function renderDashboardCalendar(data) {
    const gridEl = document.getElementById('dashboard-calendar-grid');
    const hintEl = document.getElementById('calendar-hint');
    if (!gridEl || !hintEl) return;

    const year = Number(data?.year);
    const month = Number(data?.month);
    const days = Array.isArray(data?.days) ? data.days : [];
    if (!Number.isFinite(year) || !Number.isFinite(month)) return;

    const first = new Date(year, month - 1, 1);
    const startDow = first.getDay();
    const lastDay = new Date(year, month, 0).getDate();

    const byDay = {};
    days.forEach(d => {
        const key = Number(d?.day);
        if (!Number.isFinite(key)) return;
        byDay[key] = {
            income: Number(d?.income ?? 0),
            expense: Number(d?.expense ?? 0)
        };
    });

    const cells = [];
    const totalCells = 42;
    for (let i = 0; i < totalCells; i++) {
        const dayNum = i - startDow + 1;
        const isCurrent = dayNum >= 1 && dayNum <= lastDay;
        const info = isCurrent ? (byDay[dayNum] || { income: 0, expense: 0 }) : { income: 0, expense: 0 };
        const incomeText = info.income > 0 ? `수입 ${info.income.toLocaleString()}` : '';
        const expenseText = info.expense > 0 ? `지출 ${info.expense.toLocaleString()}` : '';
        const title = isCurrent
                ? `${dayNum}일\n${incomeText}\n${expenseText}`.trim()
                : '';
        cells.push(`
            <div class="calendar-cell ${isCurrent ? '' : 'muted'}" title="${title}" data-day="${isCurrent ? dayNum : ''}">
                <div class="calendar-day">${isCurrent ? dayNum : ''}</div>
                <div class="calendar-amounts">
                    <div class="calendar-income">${incomeText}</div>
                    <div class="calendar-expense">${expenseText}</div>
                </div>
            </div>
        `);
    }
    gridEl.innerHTML = cells.join('');
    hintEl.style.display = 'none';

    gridEl.querySelectorAll('.calendar-cell[data-day]').forEach(cell => {
        const d = Number(cell.getAttribute('data-day'));
        if (!Number.isFinite(d) || d < 1) return;
        cell.classList.add('clickable');
        cell.addEventListener('click', () => {
            openDashboardDayModal(d);
        });
    });
}

function closeDashboardDayModal() {
    const overlay = document.getElementById('dashboard-day-modal');
    if (!overlay) return;
    overlay.style.display = 'none';
}

function initDashboardDayModal() {
    const overlay = document.getElementById('dashboard-day-modal');
    const closeBtn = document.getElementById('dashboard-day-modal-close');
    if (!overlay) return;

    if (closeBtn) {
        closeBtn.addEventListener('click', closeDashboardDayModal);
    }
}

function renderDashboardDayTransactions(data) {
    const tbody = document.querySelector('#dashboard-day-table tbody');
    if (!tbody) return;

    const items = Array.isArray(data?.items) ? data.items : [];
    if (items.length === 0) {
        tbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="7">선택한 날짜의 거래 내역이 없습니다.</td>
            </tr>
        `;
        return;
    }

    const rows = items.map(it => {
        const prov = it?.provider ? String(it.provider) : '';
        const desc = it?.description ? String(it.description) : '';
        const txType = it?.txType ? String(it.txType) : '';
        const txDetail = it?.txDetail ? String(it.txDetail) : '';
        const cat = it?.category ? String(it.category) : '';
        const amt = Number(it?.amount ?? 0);
        const amtText = Math.round(amt).toLocaleString();
        const bal = (typeof it?.postBalance === 'number') ? it.postBalance : Number(it?.postBalance);
        const balText = Number.isFinite(bal) ? Math.round(bal).toLocaleString() : '';

        const descEsc = escapeHtml(desc);
        const txTypeEsc = escapeHtml(txType);
        const txDetailEsc = escapeHtml(txDetail);
        const catEsc = escapeHtml(cat);
        return `
            <tr>
                <td>${prov}</td>
                <td title="${descEsc}">${descEsc}</td>
                <td title="${txTypeEsc}">${txTypeEsc}</td>
                <td title="${txDetailEsc}">${txDetailEsc}</td>
                <td title="${catEsc}">${catEsc}</td>
                <td>${amtText}</td>
                <td>${balText}</td>
            </tr>
        `;
    });
    tbody.innerHTML = rows.join('');
}

async function openDashboardDayModal(day) {
    const overlay = document.getElementById('dashboard-day-modal');
    const titleEl = document.getElementById('dashboard-day-modal-title');
    const yearEl = document.getElementById('dashboard-year');
    const monthEl = document.getElementById('dashboard-month');
    const providerEl = document.getElementById('dashboard-provider');
    if (!overlay || !yearEl || !monthEl || !providerEl) return;

    const year = Number(yearEl.value);
    const month = Number(monthEl.value);
    const provider = String(providerEl.value || 'ALL').trim().toUpperCase();

    if (!Number.isFinite(year) || !Number.isFinite(month) || month < 1 || month > 12) {
        showToast('년도/월을 먼저 선택해주세요.', 'error');
        return;
    }

    if (titleEl) {
        titleEl.textContent = `${year}년 ${month}월 ${day}일 거래 내역`;
    }
    overlay.style.display = '';

    const tbody = document.querySelector('#dashboard-day-table tbody');
    if (tbody) {
        tbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="7">불러오는 중...</td>
            </tr>
        `;
    }

    try {
        const res = await apiFetch(`/api/dashboard/day-transactions?year=${encodeURIComponent(year)}&month=${encodeURIComponent(month)}&day=${encodeURIComponent(day)}&provider=${encodeURIComponent(provider)}`);
        const data = await res.json();
        if (!res.ok || !data?.success) {
            throw new Error(data?.error || '거래 내역을 불러오지 못했습니다.');
        }
        renderDashboardDayTransactions(data);
    } catch (e) {
        if (String(e?.message || '') === 'UNAUTHORIZED') {
            closeDashboardDayModal();
            return;
        }
        if (tbody) {
            tbody.innerHTML = `
                <tr class="empty-row">
                    <td colspan="7">${String(e?.message || '불러오기 실패')}</td>
                </tr>
            `;
        }
    }
}

async function loadConfirmedMonthForYear(provider, year, month) {
    const p = String(provider || '').trim().toUpperCase();
    const y = Number(year);
    const m = Number(month);
    if (!p || !y || y < 1900 || y > 2100 || !m || m < 1 || m > 12) {
        return;
    }

    try {
        const res = await apiFetch(`/api/import/confirmed?provider=${encodeURIComponent(p)}&year=${y}&month=${m}`);
        const result = await res.json();
        if (!res.ok || !result?.success) {
            setConfirmedLock(false);
            return;
        }

        if (result.confirmed) {
            previewRows = (result.rows || []).map(r => ({
                date: r.date ?? '',
                description: r.description ?? '',
                txType: r.txType ?? '',
                txDetail: r.txDetail ?? '',
                amount: (typeof r.amount === 'number') ? r.amount : null,
                postBalance: (typeof r.postBalance === 'number') ? r.postBalance : null,
                category: r.category ?? '',
                _errors: []
            }));
            renderPreviewTable(previewRows);
            updatePreviewProviderInfo();
            setPreviewDataVisible(true);
            setConfirmedLock(true);
            return;
        }

        setConfirmedLock(false);
    } catch {
        // ignore
    }
}

let isLoggedIn = false;
let fixedExpenseEditingId = null;
let fixedExpenseItems = [];
let fixedExpenseSortKey = 'title';
let fixedExpenseSortDir = 'asc';
let fixedExpenseMonthLocked = false;
let fixedExpenseTypeFilter = 'ALL';

let txSearchCurrentPage = 0;
let txSearchLastTotalPages = 0;
let txSearchLastQueryKey = '';
let txSearchLastTotalElements = 0;
let txSearchLastStartDate = '';
let txSearchLastEndDate = '';
let txSearchLastSize = 20;
let txSearchActive = false;

let fixedIncomeEditingId = null;
let fixedIncomeItems = [];
let fixedIncomeSortKey = 'title';
let fixedIncomeSortDir = 'asc';
let fixedIncomeMonthLocked = false;
let fixedIncomeTypeFilter = 'ALL';

async function updateFixedExpenseStatus(id, nextStatus) {
    const item = fixedExpenseItems.find(e => e.id === id);
    if (!item) {
        throw new Error('해당 항목을 찾을 수 없습니다.');
    }
    const payload = {
        id: item.id,
        title: item.title,
        account: item.account,
        amount: item.amount,
        category: item.category,
        billingDay: item.billingDay,
        memo: item.memo,
        status: nextStatus
    };

    const res = await apiFetch('/api/fixed-expenses/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok || !data?.success) {
        throw new Error(data?.error || '상태 변경에 실패했습니다.');
    }
}

async function apiFetch(url, options) {
    const res = await fetch(url, { ...(options || {}), credentials: 'same-origin' });
    if (res.status === 401) {
        promptLoginOnce();
        throw new Error('UNAUTHORIZED');
    }
    return res;
}

function initDashboardControls() {
    const yearEl = document.getElementById('dashboard-year');
    const monthEl = document.getElementById('dashboard-month');
    const providerEl = document.getElementById('dashboard-provider');
    const loadBtn = document.getElementById('dashboard-load-btn');
    const catModeEl = document.getElementById('dashboard-category-mode');
    if (!yearEl || !monthEl || !providerEl || !loadBtn) {
        return;
    }

    yearEl.innerHTML = '';
    const nowY = new Date().getFullYear();
    for (let y = nowY - 5; y <= nowY + 5; y++) {
        const opt = document.createElement('option');
        opt.value = String(y);
        opt.textContent = `${y}년`;
        if (y === nowY) opt.selected = true;
        yearEl.appendChild(opt);
    }

    monthEl.innerHTML = '';
    {
        const allOpt = document.createElement('option');
        allOpt.value = '0';
        allOpt.textContent = '전체';
        monthEl.appendChild(allOpt);
    }
    for (let m = 1; m <= 12; m++) {
        const opt = document.createElement('option');
        opt.value = String(m);
        opt.textContent = `${m}월`;
        if (m === currentMonth) opt.selected = true;
        monthEl.appendChild(opt);
    }

    loadBtn.addEventListener('click', () => {
        loadDashboard();
    });

    setupDashboardTabs();
    setupDashboardCalendarNav();

    if (catModeEl) {
        catModeEl.addEventListener('change', () => {
            if (window.__lastDashboardCategoryResponse) {
                renderDashboardCategoryIncomeExpense(window.__lastDashboardCategoryResponse);
            }
        });
    }
}

async function loadDashboard() {
    const yearEl = document.getElementById('dashboard-year');
    const monthEl = document.getElementById('dashboard-month');
    const providerEl = document.getElementById('dashboard-provider');
    const chartsWrap = document.getElementById('dashboard-charts');
    const placeholder = document.getElementById('dashboard-placeholder');
    const providerWrap = document.getElementById('chart-provider-expense-wrap');
    const topChartsWrap = document.getElementById('dashboard-top-charts');
    const summaryWrap = document.getElementById('dashboard-summary');
    const categoryWrap = document.getElementById('chart-category-wrap');

    if (!yearEl || !monthEl || !providerEl || !chartsWrap || !placeholder) return;

    const year = Number(yearEl.value);
    const month = Number(monthEl.value);
    const provider = String(providerEl.value || 'ALL').trim().toUpperCase();

    try {
        showMessage(null, '대시보드 데이터를 불러오는 중...', 'loading');

        const monthlyRes = await apiFetch(`/api/dashboard/monthly?year=${encodeURIComponent(year)}&provider=${encodeURIComponent(provider)}`);
        const monthly = await monthlyRes.json();
        if (!monthlyRes.ok || !monthly?.success) {
            throw new Error(monthly?.error || '월별 데이터를 불러오지 못했습니다.');
        }

        hideLoading();
        chartsWrap.style.display = '';
        placeholder.style.display = 'none';

        if (summaryWrap) {
            summaryWrap.style.display = '';
        }

        renderDashboardMonthlyIncomeExpense(monthly);
        renderDashboardSummaryNumbers(monthly, month);

        const catRes = await apiFetch(`/api/dashboard/categories?year=${encodeURIComponent(year)}&month=${encodeURIComponent(month)}&provider=${encodeURIComponent(provider)}`);
        const cat = await catRes.json();
        if (!catRes.ok || !cat?.success) {
            throw new Error(cat?.error || '카테고리 데이터를 불러오지 못했습니다.');
        }
        if (categoryWrap) categoryWrap.style.display = '';
        window.__lastDashboardCategoryResponse = cat;
        renderDashboardCategoryIncomeExpense(cat);

        const providerKeys = monthly?.providerBreakdown ? Object.keys(monthly.providerBreakdown) : [];
        const showProviderChart = (provider === 'ALL') && Array.isArray(providerKeys) && providerKeys.length >= 1;

        if (providerWrap) {
            providerWrap.style.display = showProviderChart ? '' : 'none';
        }
        if (topChartsWrap) {
            if (showProviderChart) {
                topChartsWrap.classList.remove('single');
            } else {
                topChartsWrap.classList.add('single');
            }
        }
        if (showProviderChart) {
            renderDashboardProviderMonthlyExpense(monthly);
        }

        // 달력 탭이 활성화되어 있으면 같이 갱신
        const calendarPanel = document.querySelector('#dashboard-content .dashboard-tab-content[data-tab-panel="calendar"]');
        if (calendarPanel && calendarPanel.classList.contains('active')) {
            loadDashboardCalendar();
        }
    } catch (e) {
        hideLoading();
        chartsWrap.style.display = 'none';
        if (summaryWrap) summaryWrap.style.display = 'none';
        placeholder.style.display = '';
        if (String(e?.message || '') === 'UNAUTHORIZED') {
            return;
        }
        showToast(e?.message || '대시보드 로딩 실패', 'error');
    }
}

function renderDashboardSummaryNumbers(monthly, selectedMonth) {
    const incomeEl = document.getElementById('dashboard-summary-income');
    const expenseEl = document.getElementById('dashboard-summary-expense');
    const balanceEl = document.getElementById('dashboard-summary-balance');
    const momEl = document.getElementById('dashboard-summary-mom');
    if (!incomeEl || !expenseEl || !balanceEl) return;

    const incomeArr = Array.isArray(monthly.incomeByMonth) ? monthly.incomeByMonth : [];
    const expenseArr = Array.isArray(monthly.expenseByMonth) ? monthly.expenseByMonth : [];

    const m = Number(selectedMonth);
    let totalIncome = 0;
    let totalExpense = 0;
    if (m === 0) {
        totalIncome = incomeArr.reduce((a, b) => a + (Number(b) || 0), 0);
        totalExpense = expenseArr.reduce((a, b) => a + (Number(b) || 0), 0);
    } else if (Number.isFinite(m) && m >= 1 && m <= 12) {
        totalIncome = Number(incomeArr[m - 1]) || 0;
        totalExpense = Number(expenseArr[m - 1]) || 0;
    } else {
        totalIncome = incomeArr.reduce((a, b) => a + (Number(b) || 0), 0);
        totalExpense = expenseArr.reduce((a, b) => a + (Number(b) || 0), 0);
    }
    const balance = totalIncome - totalExpense;

    incomeEl.textContent = Math.round(totalIncome).toLocaleString();
    expenseEl.textContent = Math.round(totalExpense).toLocaleString();
    balanceEl.textContent = Math.round(balance).toLocaleString();

    if (!momEl) return;

    const fmtPct = (cur, prev) => {
        const c = Number(cur) || 0;
        const p = Number(prev) || 0;
        if (p === 0) {
            if (c === 0) return '0%';
            return '-';
        }
        const pct = ((c - p) / p) * 100;
        const sign = pct > 0 ? '+' : '';
        return `${sign}${pct.toFixed(1)}%`;
    };

    const mPrevLine = () => {
        const m = Number(selectedMonth);
        if (!Number.isFinite(m) || m < 1 || m > 12) return null;
        if (m === 1) return null;
        const curIncome = Number(incomeArr[m - 1]) || 0;
        const curExpense = Number(expenseArr[m - 1]) || 0;
        const prevIncome = Number(incomeArr[m - 2]) || 0;
        const prevExpense = Number(expenseArr[m - 2]) || 0;
        const curBal = curIncome - curExpense;
        const prevBal = prevIncome - prevExpense;

        const incomePct = fmtPct(curIncome, prevIncome);
        const expensePct = fmtPct(curExpense, prevExpense);
        const balPct = fmtPct(curBal, prevBal);
        return `전월 대비: 수입 ${incomePct} / 지출 ${expensePct} / 순이익 ${balPct}`;
    };

    if (Number(selectedMonth) === 0) {
        momEl.textContent = '전월 대비: -';
        return;
    }
    const line = mPrevLine();
    momEl.textContent = line || '전월 대비: -';
}

function renderDashboardMonthlyIncomeExpense(monthly) {
    const canvas = document.getElementById('chart-monthly-income-expense');
    if (!canvas || typeof window.Chart === 'undefined') return;

    const labels = Array.isArray(monthly.months) ? monthly.months.map(m => `${m}월`) : [];
    const income = Array.isArray(monthly.incomeByMonth) ? monthly.incomeByMonth : [];
    const expense = Array.isArray(monthly.expenseByMonth) ? monthly.expenseByMonth : [];

    if (dashboardMonthlyChart) {
        dashboardMonthlyChart.destroy();
        dashboardMonthlyChart = null;
    }

    dashboardMonthlyChart = new window.Chart(canvas.getContext('2d'), {
        type: 'line',
        data: {
            labels,
            datasets: [
                {
                    label: '수입',
                    data: income,
                    borderColor: '#22c55e',
                    backgroundColor: 'rgba(34, 197, 94, 0.15)',
                    tension: 0.25
                },
                {
                    label: '지출',
                    data: expense,
                    borderColor: '#ef4444',
                    backgroundColor: 'rgba(239, 68, 68, 0.15)',
                    tension: 0.25
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: 'top' } },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { callback: (v) => Number(v).toLocaleString() }
                }
            }
        }
    });
}

function renderDashboardProviderMonthlyExpense(monthly) {
    const canvas = document.getElementById('chart-provider-monthly-expense');
    if (!canvas || typeof window.Chart === 'undefined') return;

    const labels = Array.isArray(monthly.months) ? monthly.months.map(m => `${m}월`) : [];
    const breakdown = monthly.providerBreakdown || {};

    const palette = ['#3b82f6', '#f59e0b', '#a855f7', '#06b6d4', '#10b981', '#ef4444', '#64748b'];
    let colorIdx = 0;

    const datasets = Object.keys(breakdown).sort().map(provider => {
        const byMonth = breakdown[provider] || {};
        const data = (Array.isArray(monthly.months) ? monthly.months : []).map(m => {
            const ma = byMonth[m];
            const expense = ma && typeof ma.expense === 'number' ? ma.expense : 0;
            return expense;
        });
        const c = palette[colorIdx++ % palette.length];
        return {
            label: provider,
            data,
            borderColor: c,
            backgroundColor: c,
            tension: 0.25
        };
    });

    if (dashboardProviderExpenseChart) {
        dashboardProviderExpenseChart.destroy();
        dashboardProviderExpenseChart = null;
    }

    dashboardProviderExpenseChart = new window.Chart(canvas.getContext('2d'), {
        type: 'line',
        data: { labels, datasets },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: 'bottom' } },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { callback: (v) => Number(v).toLocaleString() }
                }
            }
        }
    });
}

function renderDashboardCategoryIncomeExpense(cat) {
    const canvas = document.getElementById('chart-category-income-expense');
    if (!canvas || typeof window.Chart === 'undefined') return;

    const modeEl = document.getElementById('dashboard-category-mode');
    const mode = modeEl ? String(modeEl.value || 'EXPENSE').toUpperCase() : 'EXPENSE';

    const titleEl = document.querySelector('#chart-category-wrap .chart-title');
    if (titleEl) {
        const m = Number(cat?.month);
        if (m === 0) {
            titleEl.textContent = '카테고리별 합산 (연간)';
        } else if (Number.isFinite(m) && m >= 1 && m <= 12) {
            titleEl.textContent = `카테고리별 수입/지출 (${m}월)`;
        } else {
            titleEl.textContent = '카테고리별 수입/지출';
        }
    }

    const incomeMap = cat.incomeByCategory || {};
    const expenseMap = cat.expenseByCategory || {};

    const baseMap = (mode === 'INCOME') ? incomeMap : expenseMap;
    let keys = Object.keys(baseMap);
    keys = keys.filter(k => typeof baseMap[k] === 'number' && Number(baseMap[k]) > 0);
    keys.sort((a, b) => a.localeCompare(b, 'ko'));

    const values = keys.map(k => (typeof baseMap[k] === 'number') ? baseMap[k] : 0);

    if (keys.length === 0) {
        keys = ['데이터 없음'];
        values.length = 0;
        values.push(1);
    }

    if (dashboardCategoryChart) {
        dashboardCategoryChart.destroy();
        dashboardCategoryChart = null;
    }

    const palette = [
        '#2563eb', '#16a34a', '#f97316', '#dc2626', '#7c3aed',
        '#0891b2', '#ca8a04', '#0f766e', '#be185d', '#4b5563'
    ];
    const colors = keys.map((_, i) => palette[i % palette.length]);

    dashboardCategoryChart = new window.Chart(canvas.getContext('2d'), {
        type: 'doughnut',
        data: {
            labels: keys,
            datasets: [
                {
                    label: (mode === 'INCOME') ? '수입' : '지출',
                    data: values,
                    backgroundColor: keys[0] === '데이터 없음' ? ['rgba(156, 163, 175, 0.6)'] : colors,
                    borderColor: keys[0] === '데이터 없음' ? ['rgba(156, 163, 175, 1)'] : colors,
                    borderWidth: 1
                }
            ]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom' },
                tooltip: {
                    callbacks: {
                        label: (ctx) => {
                            const label = ctx.label || '';
                            const value = Number(ctx.parsed || 0);
                            return `${label}: ${Math.round(value).toLocaleString()}`;
                        }
                    }
                }
            }
        }
    });
}
let previewSortKey = 'date';
let previewSortDir = 'desc';
let isCurrentMonthConfirmed = false;

let previewPageSize = 15;
let previewCurrentPage = 1;

let dashboardMonthlyChart = null;
let dashboardProviderExpenseChart = null;
let dashboardCategoryChart = null;

const supportedProviders = ['TOSS', 'NH', 'KB', 'HYUNDAI', 'FIXED'];

function hasAnyPreviewDataRow(rows) {
    if (!Array.isArray(rows) || rows.length === 0) return false;
    return rows.some(r => {
        if (!r) return false;
        const hasCore = (r.date && String(r.date).trim() !== '')
            || (r.description && String(r.description).trim() !== '')
            || (typeof r.amount === 'number' && !isNaN(r.amount));
        return hasCore;
    });
}

function setPreviewDataVisible(visible) {
    const area = document.getElementById('preview-data-area');
    if (!area) return;
    area.style.display = visible ? '' : 'none';
}

function syncCurrentYearFromRows(rows) {
    if (!Array.isArray(rows)) return;
    const firstDate = rows.map(r => r?.date).find(d => typeof d === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(d));
    if (!firstDate) return;
    const y = Number(firstDate.slice(0, 4));
    if (!Number.isFinite(y) || y < 1900 || y > 2100) return;
    currentYear = y;
}

function inferYearMonthFromRows(rows) {
    if (!Array.isArray(rows) || rows.length === 0) return null;

    const counts = new Map();
    for (const r of rows) {
        const d = r?.date;
        if (typeof d !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(d)) continue;
        const y = Number(d.slice(0, 4));
        const m = Number(d.slice(5, 7));
        if (!Number.isFinite(y) || y < 1900 || y > 2100) continue;
        if (!Number.isFinite(m) || m < 1 || m > 12) continue;
        const key = `${y}-${m}`;
        counts.set(key, (counts.get(key) || 0) + 1);
    }
    // 확정 저장은 "한 달" 단위로 동작해야 하므로,
    // 여러 달이 섞여있으면 월을 임의로 고르지 않는다.
    if (counts.size !== 1) return null;

    const bestKey = Array.from(counts.keys())[0];
    const [yy, mm] = bestKey.split('-');
    const year = Number(yy);
    const month = Number(mm);
    if (!Number.isFinite(year) || !Number.isFinite(month)) return null;
    return { year, month };
}

function setConfirmedLock(confirmed) {
    isCurrentMonthConfirmed = !!confirmed;

    const badge = document.getElementById('confirmed-badge');
    if (!badge) {
        const infoEl = document.getElementById('preview-provider-info');
        if (infoEl) {
            const span = document.createElement('span');
            span.id = 'confirmed-badge';
            span.className = 'confirmed-badge';
            infoEl.appendChild(span);
        }
    }

    const badgeEl = document.getElementById('confirmed-badge');
    if (badgeEl) {
        if (confirmed) {
            badgeEl.textContent = '확정 완료';
            badgeEl.style.display = 'inline-block';
        } else {
            badgeEl.style.display = 'none';
        }
    }

    syncUnconfirmButtonVisibility();

    const uploadBtn = document.getElementById('upload-next-btn');
    const saveBtn = document.getElementById('preview-save-btn');
    const dropzone = document.getElementById('upload-dropzone');
    const fileInput = document.getElementById('upload-file-input');
    const fileRemoveBtn = document.getElementById('file-remove-btn');
    const bulkSelect = document.getElementById('preview-bulk-category');
    const bulkApplyBtn = document.getElementById('preview-bulk-apply-btn');
    const bulkClearBtn = document.getElementById('preview-bulk-clear-btn');
    const miscOnlyEl = document.getElementById('preview-filter-misc-only');

    // 업로드/파일 변경/삭제 잠금
    if (uploadBtn) {
        uploadBtn.disabled = !!confirmed;
        uploadBtn.style.opacity = confirmed ? '0.5' : '1';
        uploadBtn.style.cursor = confirmed ? 'not-allowed' : 'pointer';
    }

    if (dropzone) {
        dropzone.style.pointerEvents = confirmed ? 'none' : '';
        dropzone.style.opacity = confirmed ? '0.5' : '';
    }
    if (fileInput) {
        fileInput.disabled = !!confirmed;
    }
    if (fileRemoveBtn) {
        fileRemoveBtn.disabled = !!confirmed;
        fileRemoveBtn.style.opacity = confirmed ? '0.5' : '';
        fileRemoveBtn.style.cursor = confirmed ? 'not-allowed' : '';
    }

    // 테이블 수정 버튼 잠금
    document.querySelectorAll('.edit-btn').forEach(btn => {
        btn.disabled = !!confirmed;
        btn.style.opacity = confirmed ? '0.5' : '';
        btn.style.cursor = confirmed ? 'not-allowed' : '';
        btn.style.pointerEvents = confirmed ? 'none' : '';
    });

    // 일괄 편집(카테고리 변경) 잠금
    if (bulkSelect) {
        bulkSelect.disabled = !!confirmed;
        bulkSelect.style.opacity = confirmed ? '0.5' : '';
        bulkSelect.style.cursor = confirmed ? 'not-allowed' : '';
        bulkSelect.style.pointerEvents = confirmed ? 'none' : '';
    }
    if (bulkApplyBtn) {
        bulkApplyBtn.disabled = !!confirmed;
        bulkApplyBtn.style.opacity = confirmed ? '0.5' : '';
        bulkApplyBtn.style.cursor = confirmed ? 'not-allowed' : '';
        bulkApplyBtn.style.pointerEvents = confirmed ? 'none' : '';
    }
    if (bulkClearBtn) {
        bulkClearBtn.disabled = !!confirmed;
        bulkClearBtn.style.opacity = confirmed ? '0.5' : '';
        bulkClearBtn.style.cursor = confirmed ? 'not-allowed' : '';
        bulkClearBtn.style.pointerEvents = confirmed ? 'none' : '';
    }
    if (miscOnlyEl) {
        miscOnlyEl.disabled = !!confirmed;
        miscOnlyEl.style.opacity = confirmed ? '0.5' : '';
        miscOnlyEl.style.cursor = confirmed ? 'not-allowed' : '';
        miscOnlyEl.style.pointerEvents = confirmed ? 'none' : '';
    }

    // 확정 저장 버튼 잠금
    if (saveBtn) {
        saveBtn.disabled = !!confirmed;
        saveBtn.style.opacity = confirmed ? '0.5' : '1';
        saveBtn.style.cursor = confirmed ? 'not-allowed' : 'pointer';
    }
}

async function loadConfirmedMonthIfAny(provider, month) {
    const p = String(provider || '').trim().toUpperCase();
    const m = Number(month);
    if (!p || !m || m < 1 || m > 12) {
        return;
    }

    try {
        const yearsFromRows = Array.isArray(previewRows)
            ? previewRows
                .map(r => r?.date)
                .filter(d => typeof d === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(d))
                .map(d => Number(String(d).slice(0, 4)))
                .filter(y => Number.isFinite(y))
            : [];

        const yearsToTry = Array.from(new Set([
            currentYear - 2,
            currentYear - 1,
            currentYear,
            currentYear + 1,
            currentYear + 2,
            ...yearsFromRows
        ])).filter(y => Number.isFinite(y) && y >= 1900 && y <= 2100);

        for (const y of yearsToTry) {
            const res = await apiFetch(`/api/import/confirmed?provider=${encodeURIComponent(p)}&year=${y}&month=${m}`);
            const result = await res.json();
            if (!res.ok || !result?.success) {
                continue;
            }

            if (result.confirmed) {
                currentYear = y;
                syncPreviewYearTabs();
                previewRows = (result.rows || []).map(r => ({
                    date: r.date ?? '',
                    description: r.description ?? '',
                    amount: (typeof r.amount === 'number') ? r.amount : null,
                    category: r.category ?? '',
                    _errors: []
                }));
                renderPreviewTable(previewRows);
                updatePreviewProviderInfo();
                setPreviewDataVisible(true);
                setConfirmedLock(true);
                return;
            }
        }

        // 미확정 상태면 잠금 해제 (기존 월 state 복원 로직 유지)
        setConfirmedLock(false);
    } catch (e) {
        // ignore
    }
}

function saveBankMonthState(provider, year, month) {
    const p = String(provider || '').trim().toUpperCase();
    const y = Number(year);
    if (!p) return;
    if (!y || y < 1900 || y > 2100) return;
    if (!month || month < 1 || month > 12) return;

    const pdfPasswordGroup = document.getElementById('pdf-password-group');
    const passwordRequired = !!(pdfPasswordGroup && pdfPasswordGroup.style.display !== 'none');
    const rows = Array.isArray(previewRows) ? previewRows.map(r => ({ ...r })) : [];
    const hasData = hasAnyPreviewDataRow(rows);
    const errorMessage = (!hasData)
        ? rows.flatMap(r => Array.isArray(r?._errors) ? r._errors : []).filter(Boolean).join(' / ')
        : '';

    const hasAny = hasData || (errorMessage && errorMessage.trim() !== '') || passwordRequired;
    if (!hasAny) {
        if (bankMonthState?.[p]?.[y]) {
            delete bankMonthState[p][y][month];
        }
        return;
    }

    if (!bankMonthState[p]) {
        bankMonthState[p] = {};
    }
    if (!bankMonthState[p][y]) {
        bankMonthState[p][y] = {};
    }
    bankMonthState[p][y][month] = {
        rows: hasData ? rows : [],
        errorMessage: hasData ? '' : errorMessage,
        passwordRequired
    };
}

function restoreBankMonthState(provider, year, month) {
    const p = String(provider || '').trim().toUpperCase();
    const y = Number(year);
    if (!p) return;
    if (!y || y < 1900 || y > 2100) return;
    if (!month || month < 1 || month > 12) return;

    const selectedFileDiv = document.getElementById('selected-file');
    const selectedFileName = document.getElementById('selected-file-name');
    const pdfPasswordGroup = document.getElementById('pdf-password-group');
    const pdfPasswordInput = document.getElementById('pdf-password-input');
    const uploadSection = document.getElementById('upload-dropzone')?.closest('.upload-section');

    // 같은 세션에서는 File 객체를 메모리에 유지할 수 있으므로 복원
    const fileState = getUploadFileState(p, y, month);
    selectedUploadFile = fileState?.file || null;
    if (selectedFileDiv && selectedFileName && selectedUploadFile) {
        selectedFileName.textContent = selectedUploadFile.name || '';
        selectedFileDiv.style.display = 'block';
    } else {
        if (selectedFileDiv) selectedFileDiv.style.display = 'none';
        if (selectedFileName) selectedFileName.textContent = '';
    }

    const state = bankMonthState?.[p]?.[y]?.[month];
    if (!state) {
        previewRows = [];
        renderPreviewTable([]);
        setPreviewDataVisible(false);

        updateUploadButton();
        updatePreviewProviderInfo();

        // 확정 데이터가 있으면 DB에서 로드 (async)
        loadConfirmedMonthForYear(p, y, month);
        return;
    }

    previewRows = Array.isArray(state.rows) ? state.rows.map(r => ({ ...r })) : [];
    previewCurrentPage = 1;
    renderPreviewTable(previewRows);
    setPreviewDataVisible(hasAnyPreviewDataRow(previewRows));

    if (state.errorMessage && state.errorMessage.trim() !== '' && uploadSection) {
        showMessage(uploadSection, state.errorMessage, 'error');
    }

    if (pdfPasswordGroup) {
        pdfPasswordGroup.style.display = state.passwordRequired ? '' : 'none';
    }
    if (!state.passwordRequired && pdfPasswordInput) {
        pdfPasswordInput.value = '';
    }

    updateUploadButton();
    updatePreviewProviderInfo();

    // 확정 데이터가 있으면 DB에서 로드 (async)
    loadConfirmedMonthForYear(p, y, month);
}

function getProviderLabel(provider) {
    const p = String(provider || '').trim().toUpperCase();
    const labels = {
        'TOSS': '토스',
        'NH': '농협',
        'KB': '국민',
        'HYUNDAI': '현대카드'
    };
    return labels[p] || p;
}

function initBankTabs() {
    const container = document.getElementById('bank-tabs');
    if (!container) return;

    const tabs = container.querySelectorAll('.bank-tab');
    if (!tabs || tabs.length === 0) return;

    // 현재 활성 탭 기준으로 provider 초기화
    const active = container.querySelector('.bank-tab.active');
    if (active && active.dataset.provider) {
        currentProvider = String(active.dataset.provider).trim().toUpperCase();
    }

    tabs.forEach(btn => {
        btn.addEventListener('click', () => {
            const p = String(btn.dataset.provider || '').trim().toUpperCase();
            if (!p) return;
            if (p === String(currentProvider || '').trim().toUpperCase()) return;

            // 현재 은행/월 상태 저장 후 이동
            saveBankMonthState(currentProvider, currentYear, currentMonth);

            currentProvider = p;
            tabs.forEach(t => t.classList.toggle('active', t === btn));

            // 선택된 은행/월 상태 복원 (없으면 초기화)
            restoreBankMonthState(currentProvider, currentYear, currentMonth);
        });
    });
}

function syncPreviewYearTabs() {
    const container = document.getElementById('preview-year-tabs');
    if (!container) return;

    const nowY = new Date().getFullYear();
    const minY = nowY - 5;
    const maxY = nowY;
    const y = Number(currentYear);

    container.innerHTML = '';
    for (let year = minY; year <= maxY; year++) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'year-tab';
        btn.textContent = String(year);
        btn.dataset.year = String(year);
        btn.classList.toggle('active', year === y);
        btn.addEventListener('click', () => {
            const nextY = Number(btn.dataset.year);
            if (!Number.isFinite(nextY) || nextY < 1900 || nextY > 2100) return;
            if (nextY === currentYear) return;
            saveBankMonthState(currentProvider, currentYear, currentMonth);
            currentYear = nextY;
            syncPreviewYearTabs();
            updatePreviewProviderInfo();
            restoreBankMonthState(currentProvider, currentYear, currentMonth);
        });
        container.appendChild(btn);
    }
}

function initSidebarMenu() {
    const menuItems = document.querySelectorAll('.menu-item');
    const subItems = document.querySelectorAll('.sub-menu-item');

    if (subItems.length > 0) {
        menuItems.forEach(item => {
            item.addEventListener('click', (e) => {
                // 상위 메뉴는 펼침/접힘만 담당
                if (e.target && e.target.closest('.sub-menu-item')) {
                    return;
                }
                e.preventDefault();
                e.stopPropagation();
                const hasSub = !!item.querySelector('.sub-menu');
                if (!hasSub) return;
                menuItems.forEach(mi => {
                    if (mi !== item) mi.classList.remove('active');
                });
                item.classList.toggle('active');
            });
        });

        subItems.forEach(sub => {
            sub.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                const targetMenu = sub.dataset.menu;
                if (!targetMenu) return;

                // 하위 메뉴 active 처리
                subItems.forEach(si => si.classList.remove('active'));
                sub.classList.add('active');

                // 상위 메뉴는 열어두기
                const parentMenu = sub.closest('.menu-item');
                if (parentMenu) {
                    menuItems.forEach(mi => {
                        if (mi !== parentMenu) mi.classList.remove('active');
                    });
                    parentMenu.classList.add('active');
                }

                showContentPanel(targetMenu);
            });
        });

        // 초기 상태: active sub-menu-item 기준으로 패널 표시
        const activeSub = document.querySelector('.sub-menu-item.active');
        if (activeSub && activeSub.dataset.menu) {
            showContentPanel(activeSub.dataset.menu);
        }
        return;
    }

    // 하위 메뉴가 없는(구버전) 구조 fallback
    menuItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            const targetMenu = item.dataset.menu;
            if (!targetMenu) return;
            menuItems.forEach(mi => mi.classList.remove('active'));
            item.classList.add('active');
            showContentPanel(targetMenu);
        });
    });
}

function initMonthTabs() {
    const container = document.getElementById('month-tabs');
    if (!container) return;
    container.innerHTML = '';
    for (let m = 1; m <= 12; m++) {
        const tab = document.createElement('div');
        tab.className = 'month-tab';
        tab.textContent = `${m}월`;
        tab.dataset.month = m;
        if (m === currentMonth) tab.classList.add('active');
        tab.addEventListener('click', () => selectMonth(m));
        container.appendChild(tab);
    }
}

function selectMonth(month) {
    // 현재 은행/월 상태 저장 후 이동
    saveBankMonthState(currentProvider, currentYear, currentMonth);

    currentMonth = month;
    document.querySelectorAll('.month-tab').forEach(tab => {
        tab.classList.toggle('active', parseInt(tab.dataset.month) === month);
    });

    // 선택된 은행/월 상태 복원 (없으면 초기화)
    restoreBankMonthState(currentProvider, currentYear, month);
}

function showContentPanel(panelId) {
    const contentPanels = document.querySelectorAll('.content-panel');
    contentPanels.forEach(panel => {
        panel.classList.remove('active');
        if (panel.id === `${panelId}-content`) {
            panel.classList.add('active');
        }
    });

    if (panelId === 'fixed-expense') {
        loadFixedExpenses();
    }

    if (panelId === 'admin') {
        loadAdminUsers();
    }
}

async function refreshAuthUI() {
    const userEl = document.getElementById('auth-user');
    const openBtn = document.getElementById('auth-open-btn');
    const logoutBtn = document.getElementById('auth-logout-btn');
    if (!userEl || !openBtn || !logoutBtn) return;

    try {
        const res = await fetch('/api/auth/me', { credentials: 'same-origin' });
        const data = await res.json();
        if (!res.ok || !data?.success) {
            isLoggedIn = false;
            currentUserRole = '';
            userEl.textContent = '로그인 필요';
            userEl.style.display = '';
            openBtn.style.display = '';
            logoutBtn.style.display = 'none';
            const adminMenu = document.getElementById('admin-menu');
            if (adminMenu) adminMenu.style.display = 'none';
            syncUnconfirmButtonVisibility();
            return;
        }

        isLoggedIn = true;
        currentUserRole = String(data.role || '').trim().toUpperCase();
        userEl.textContent = `${data.username}`;
        userEl.style.display = '';
        openBtn.style.display = 'none';
        logoutBtn.style.display = '';
        const adminMenu = document.getElementById('admin-menu');
        if (adminMenu) adminMenu.style.display = (currentUserRole === 'ADMIN') ? '' : 'none';
        syncUnconfirmButtonVisibility();
    } catch {
        isLoggedIn = false;
        currentUserRole = '';
        userEl.textContent = '로그인 필요';
        userEl.style.display = '';
        openBtn.style.display = '';
        logoutBtn.style.display = 'none';
        const adminMenu = document.getElementById('admin-menu');
        if (adminMenu) adminMenu.style.display = 'none';
        syncUnconfirmButtonVisibility();
    }
}

function formatAdminDateTime(v) {
    if (!v) return '';
    const s = String(v);
    return s.replace('T', ' ').slice(0, 19);
}

async function loadAdminUsers() {
    const tbody = document.getElementById('admin-user-tbody');
    const qEl = document.getElementById('admin-user-query');
    if (!tbody) return;

    if (!isLoggedIn || String(currentUserRole).toUpperCase() !== 'ADMIN') {
        tbody.innerHTML = `<tr class="empty-row"><td colspan="7">권한이 없습니다.</td></tr>`;
        return;
    }

    const q = qEl ? String(qEl.value || '').trim() : '';
    const qs = q ? `?query=${encodeURIComponent(q)}` : '';

    tbody.innerHTML = `<tr class="empty-row"><td colspan="7">불러오는 중...</td></tr>`;
    try {
        const res = await apiFetch(`/api/admin/users${qs}`);
        const data = await res.json();
        if (!res.ok || !data?.success) {
            throw new Error(data?.error || '사용자 목록을 불러오지 못했습니다.');
        }

        const users = Array.isArray(data.users) ? data.users : [];
        if (!users.length) {
            tbody.innerHTML = `<tr class="empty-row"><td colspan="7">사용자가 없습니다.</td></tr>`;
            return;
        }

        tbody.innerHTML = users.map(u => {
            const id = Number(u.id);
            const username = escapeHtml(u.username || '');
            const role = String(u.role || '').toUpperCase();
            const enabled = (u.enabled === null || u.enabled === undefined) ? true : !!u.enabled;
            const statusText = enabled ? '활성' : '비활성';
            const createdAt = escapeHtml(formatAdminDateTime(u.createdAt));
            const lastLoginAt = escapeHtml(formatAdminDateTime(u.lastLoginAt));
            const nextEnabled = enabled ? 'false' : 'true';

            return `
                <tr>
                    <td>${Number.isFinite(id) ? id : ''}</td>
                    <td>${username}</td>
                    <td>${escapeHtml(role || '')}</td>
                    <td>${escapeHtml(statusText)}</td>
                    <td>${createdAt}</td>
                    <td>${lastLoginAt}</td>
                    <td>
                        <div class="admin-user-actions">
                            <select class="admin-role-select" data-admin-role-select="1" data-user-id="${id}" aria-label="권한 선택">
                                <option value="USER" ${role === 'USER' ? 'selected' : ''}>USER</option>
                                <option value="ADMIN" ${role === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                            </select>
                            <button type="button" class="execute-btn outline compact-btn" data-admin-action="apply-role" data-user-id="${id}">권한 변경</button>
                            <button type="button" class="execute-btn outline compact-btn" data-admin-action="enabled" data-user-id="${id}" data-enabled="${nextEnabled}">${enabled ? '비활성화' : '활성화'}</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (e) {
        tbody.innerHTML = `<tr class="empty-row"><td colspan="7">${escapeHtml(e?.message || '오류가 발생했습니다.')}</td></tr>`;
    }
}

async function adminUpdateUser(id, patch) {
    const res = await apiFetch(`/api/admin/users/${encodeURIComponent(id)}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(patch || {})
    });
    const data = await res.json();
    if (!res.ok || !data?.success) {
        throw new Error(data?.error || '변경에 실패했습니다.');
    }
    return data;
}

function initAdminUI() {
    const panel = document.getElementById('admin-content');
    if (!panel || panel.dataset.bound) return;
    panel.dataset.bound = '1';

    const tbody = document.getElementById('admin-user-tbody');
    const searchBtn = document.getElementById('admin-user-search-btn');
    const refreshBtn = document.getElementById('admin-user-refresh-btn');
    const qEl = document.getElementById('admin-user-query');

    if (searchBtn) {
        searchBtn.addEventListener('click', () => {
            loadAdminUsers();
        });
    }
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            if (qEl) qEl.value = '';
            loadAdminUsers();
        });
    }
    if (qEl) {
        qEl.addEventListener('keydown', (e) => {
            if (e && e.key === 'Enter') {
                e.preventDefault();
                loadAdminUsers();
            }
        });
    }

    if (tbody && !tbody.dataset.bound) {
        tbody.dataset.bound = '1';
        tbody.addEventListener('click', async (e) => {
            const btn = e?.target?.closest('button[data-admin-action]');
            if (!btn) return;
            const action = String(btn.dataset.adminAction || '').trim();
            const id = btn.dataset.userId;
            if (!id) return;

            try {
                btn.disabled = true;
                if (action === 'apply-role') {
                    const row = btn.closest('tr');
                    const sel = row ? row.querySelector('select[data-admin-role-select="1"]') : null;
                    const role = sel ? String(sel.value || '').trim().toUpperCase() : '';
                    if (!role) return;
                    await adminUpdateUser(id, { role });
                    showToast('권한 변경 완료', 'success');
                    await refreshAuthUI();
                    await loadAdminUsers();
                    return;
                }
                if (action === 'enabled') {
                    const enabled = String(btn.dataset.enabled || '').trim().toLowerCase() === 'true';
                    await adminUpdateUser(id, { enabled });
                    showToast('계정 상태 변경 완료', 'success');
                    await refreshAuthUI();
                    await loadAdminUsers();
                    return;
                }
            } catch (err) {
                showToast(err?.message || '변경에 실패했습니다.', 'error');
            } finally {
                btn.disabled = false;
            }
        });
    }
}

function ensureUnconfirmButton() {
    const infoEl = document.getElementById('preview-provider-info');
    if (!infoEl) return null;
    let btn = document.getElementById('unconfirm-btn');
    if (btn) return btn;

    btn = document.createElement('button');
    btn.type = 'button';
    btn.id = 'unconfirm-btn';
    btn.className = 'unconfirm-btn';
    btn.textContent = '확정 취소';
    btn.addEventListener('click', async () => {
        if (!isLoggedIn) return;
        if (!isCurrentMonthConfirmed) return;
        if (!confirm('정말 확정 취소하시겠습니까?\n해당 월 데이터가 DB에서 삭제됩니다.')) return;

        const provider = String(currentProvider || '').trim().toUpperCase();
        const year = Number(currentYear);
        const month = Number(currentMonth);
        if (!provider || !Number.isFinite(year) || !Number.isFinite(month)) return;

        btn.disabled = true;
        const previewSection = document.getElementById('preview-data-area') || document.querySelector('#preview-content .form-section');
        showMessage(previewSection, '확정 취소(삭제) 중...', 'loading');

        try {
            const res = await apiFetch('/api/import/unconfirm', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ provider, year, month })
            });
            const data = await res.json();
            if (!res.ok || !data?.success) {
                throw new Error(data?.error || '확정 취소에 실패했습니다.');
            }

            // 현재 월 상태 초기화 + 잠금 해제
            const p = String(currentProvider || '').trim().toUpperCase();
            if (bankMonthState?.[p]?.[currentYear]) {
                delete bankMonthState[p][currentYear][currentMonth];
            }
            previewRows = [];
            renderPreviewTable([]);
            setPreviewDataVisible(false);
            setConfirmedLock(false);

            showMessage(previewSection, `확정 취소 완료 (삭제 ${data.deleted || 0}건)`, 'success');

            // 상태 재조회
            await loadConfirmedMonthForYear(provider, year, month);
        } catch (e) {
            showMessage(previewSection, e?.message || '확정 취소에 실패했습니다.', 'error');
        } finally {
            btn.disabled = false;
            syncUnconfirmButtonVisibility();
        }
    });

    infoEl.appendChild(btn);
    return btn;
}

function syncUnconfirmButtonVisibility() {
    const btn = ensureUnconfirmButton();
    if (!btn) return;
    btn.style.display = (isLoggedIn && !!isCurrentMonthConfirmed) ? 'inline-block' : 'none';
}

function requireLoginForUpload(elementForMessage) {
    if (isLoggedIn) {
        return true;
    }
    showMessage(elementForMessage || null, '로그인이 필요합니다.', 'error');
    openAuthModal('login');
    return false;
}

function openAuthModal(tab) {
    const modal = document.getElementById('auth-modal');
    if (!modal) return;
    modal.style.display = '';
    setAuthTab(tab || 'login');
}

function closeAuthModal() {
    const modal = document.getElementById('auth-modal');
    if (!modal) return;
    modal.style.display = 'none';
}

function setAuthTab(tab) {
    const titleEl = document.getElementById('auth-modal-title');
    const tabLogin = document.getElementById('auth-tab-login');
    const tabSignup = document.getElementById('auth-tab-signup');
    const panelLogin = document.getElementById('auth-panel-login');
    const panelSignup = document.getElementById('auth-panel-signup');

    const t = String(tab || 'login').toLowerCase();
    const isLogin = t !== 'signup';

    if (titleEl) titleEl.textContent = isLogin ? '로그인' : '회원가입';
    if (tabLogin) tabLogin.classList.toggle('active', isLogin);
    if (tabSignup) tabSignup.classList.toggle('active', !isLogin);
    if (panelLogin) panelLogin.style.display = isLogin ? '' : 'none';
    if (panelSignup) panelSignup.style.display = isLogin ? 'none' : '';
}

async function doAuthLogin() {
    const usernameEl = document.getElementById('auth-login-username');
    const passwordEl = document.getElementById('auth-login-password');
    const username = usernameEl ? String(usernameEl.value || '').trim() : '';
    const password = passwordEl ? String(passwordEl.value || '') : '';
    if (!username) {
        showToast('이름을 입력해주세요.', 'error');
        return;
    }
    if (!password) {
        showToast('비밀번호를 입력해주세요.', 'error');
        return;
    }

    const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ username, password })
    });
    const data = await res.json();
    if (!res.ok || !data?.success) {
        showToast(data?.error || '로그인 실패', 'error');
        return;
    }

    await refreshAuthUI();
    closeAuthModal();
    // 로그인 성공 시 대시보드와 확정 상태 자동 로드
    await loadDashboard();
    await loadConfirmedMonthForYear(currentProvider, currentYear, currentMonth);
    await loadFixedExpenses();
}

async function doAuthSignup() {
    const usernameEl = document.getElementById('auth-signup-username');
    const passwordEl = document.getElementById('auth-signup-password');
    const username = usernameEl ? String(usernameEl.value || '').trim() : '';
    const password = passwordEl ? String(passwordEl.value || '') : '';
    if (!username) {
        showToast('이름을 입력해주세요.', 'error');
        return;
    }
    if (!password) {
        showToast('비밀번호를 입력해주세요.', 'error');
        return;
    }

    const res = await fetch('/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify({ username, password })
    });
    const data = await res.json();
    if (!res.ok || !data?.success) {
        showToast(data?.error || '회원가입 실패', 'error');
        return;
    }

    await refreshAuthUI();
    closeAuthModal();
    await loadDashboard();
    await loadConfirmedMonthForYear(currentProvider, currentYear, currentMonth);
    await loadFixedExpenses();
}

async function doAuthLogout() {
    const res = await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
    if (!res.ok) {
        const data = await res.json().catch(() => null);
        showToast(data?.error || '로그아웃 실패', 'error');
        return;
    }
    await refreshAuthUI();
    window.location.reload();
}

async function initAuth() {
    const openBtn = document.getElementById('auth-open-btn');
    const logoutBtn = document.getElementById('auth-logout-btn');
    const modal = document.getElementById('auth-modal');
    const closeBtn = document.getElementById('auth-modal-close');
    const tabLogin = document.getElementById('auth-tab-login');
    const tabSignup = document.getElementById('auth-tab-signup');
    const loginBtn = document.getElementById('auth-login-btn');
    const signupBtn = document.getElementById('auth-signup-btn');

    if (openBtn) {
        openBtn.addEventListener('click', () => openAuthModal('login'));
    }
    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => doAuthLogout());
    }
    if (closeBtn) {
        closeBtn.addEventListener('click', () => closeAuthModal());
    }
    if (tabLogin) {
        tabLogin.addEventListener('click', () => setAuthTab('login'));
    }
    if (tabSignup) {
        tabSignup.addEventListener('click', () => setAuthTab('signup'));
    }
    if (loginBtn) {
        loginBtn.addEventListener('click', () => doAuthLogin());
    }
    if (signupBtn) {
        signupBtn.addEventListener('click', () => doAuthSignup());
    }

    initDashboardDayModal();
    setPreviewDataVisible(false);

    if (isLoggedIn) {
        loadDashboard();
        loadConfirmedMonthForYear(currentProvider, currentYear, currentMonth);
    }

    // 초기 은행/월 상태에서도 파일/데이터 제거
    const p = String(currentProvider || '').trim().toUpperCase();
    if (bankMonthState?.[p]?.[currentYear]) {
        delete bankMonthState[p][currentYear][currentMonth];
    }
}

async function initApp() {
    initSidebarMenu();
    initMonthTabs();
    syncPreviewYearTabs();
    initBankTabs();
    setupUploadStep();
    setupPreviewStep();
    setupPreviewActions();
    initPreviewBulkControls();
    initTxSearchUI();
    initDashboardControls();
    setupFixedItemTabs();
    initAdminUI();
    await refreshAuthUI();
    await initAuth();
    initFixedExpenseUI();
    initFixedIncomeUI();
    setupFixedIncomeAutoModal();
    setupFixedExpenseAutoModal();
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', async () => {
    initApp();
});

function initTxSearchUI() {
    const section = document.getElementById('tx-search-section');
    if (!section || section.dataset.bound) return;
    section.dataset.bound = '1';

    const startEl = document.getElementById('tx-search-start');
    const endEl = document.getElementById('tx-search-end');
    const sizeEl = document.getElementById('tx-search-size');
    const resetBtn = document.getElementById('tx-search-reset');
    const searchBtn = document.getElementById('tx-search-btn');

    if (!startEl || !endEl || !sizeEl || !resetBtn || !searchBtn) {
        return;
    }

    const syncDateEmptyClass = (el) => {
        if (!el) return;
        const v = String(el.value || '').trim();
        el.classList.toggle('is-empty', v === '');
    };

    syncDateEmptyClass(startEl);
    syncDateEmptyClass(endEl);

    startEl.addEventListener('input', () => syncDateEmptyClass(startEl));
    startEl.addEventListener('change', () => syncDateEmptyClass(startEl));
    endEl.addEventListener('input', () => syncDateEmptyClass(endEl));
    endEl.addEventListener('change', () => syncDateEmptyClass(endEl));

    const doReset = () => {
        startEl.value = '';
        endEl.value = '';
        syncDateEmptyClass(startEl);
        syncDateEmptyClass(endEl);
        sizeEl.value = '20';
        txSearchCurrentPage = 0;
        txSearchLastTotalPages = 0;
        txSearchLastQueryKey = '';
        txSearchLastTotalElements = 0;
        txSearchLastStartDate = '';
        txSearchLastEndDate = '';
        txSearchLastSize = 20;
        txSearchActive = false;

        previewCurrentPage = 1;
        previewPageSize = 20;

        // 미확정(프리뷰) 상태에서는 로컬 필터만 초기화
        if (!isCurrentMonthConfirmed) {
            clearPreviewDateFilter();
            renderPreviewTable(previewRows);
            return;
        }

        // 확정 데이터 상태에서는 현재 월 확정 내역 표시
        loadConfirmedMonthForYear(currentProvider, currentYear, currentMonth);
    };

    resetBtn.addEventListener('click', () => {
        doReset();
    });

    const runSearch = async (page = 0) => {
        const size = Number(sizeEl.value) || 20;
        const rawStart = String(startEl.value || '').trim();
        const rawEnd = String(endEl.value || '').trim();

        // 미확정(파일 프리뷰) 상태에서는 서버 검색 대신 로컬 날짜 필터로 동작
        if (!isCurrentMonthConfirmed) {
            setPreviewDateFilter(rawStart, rawEnd);
            previewCurrentPage = 1;
            previewPageSize = size;
            renderPreviewTable(previewRows);
            return;
        }

        // 날짜를 비워두고 "검색"을 누르면 사용자 기대는 "전체"인데,
        // 검색 결과로 프리뷰가 비어버리면 UX가 나쁨.
        // 따라서 날짜가 둘 다 비어있으면 검색 모드 종료 + 현재 월 확정 내역으로 복귀.
        if (!rawStart && !rawEnd) {
            txSearchActive = false;
            txSearchCurrentPage = 0;
            txSearchLastTotalPages = 0;
            txSearchLastTotalElements = 0;
            renderPreviewSearchPagination();

            previewCurrentPage = 1;
            previewPageSize = size;
            await loadConfirmedMonthForYear(currentProvider, currentYear, currentMonth);
            return;
        }

        await loadTxSearchIntoPreview({
            provider: currentProvider,
            startDate: rawStart,
            endDate: rawEnd,
            page,
            size
        });
    };

    searchBtn.addEventListener('click', () => {
        txSearchCurrentPage = 0;
        runSearch(0);
    });

    sizeEl.addEventListener('change', () => {
        txSearchCurrentPage = 0;
        runSearch(0);
    });

    // 초기 렌더
    doReset();
}

function buildTxSearchQueryKey(params) {
    return JSON.stringify({
        provider: String(params.provider || 'ALL').toUpperCase(),
        startDate: params.startDate || '',
        endDate: params.endDate || '',
        size: Number(params.size) || 20
    });
}

async function loadTxSearchIntoPreview(params) {
    const provider = String(params?.provider || 'ALL').trim();
    let startDate = String(params?.startDate || '').trim();
    let endDate = String(params?.endDate || '').trim();
    const page = Number(params?.page) || 0;
    const size = Number(params?.size) || 20;
    const noFallback = !!params?.__noFallback;

    // 시작/종료일을 비워두면 사용자 기대는 "전체"이므로 넓은 범위로 검색
    if (!startDate) startDate = '1900-01-01';
    if (!endDate) endDate = '2999-12-31';

    const queryKey = buildTxSearchQueryKey({ provider, startDate, endDate, size });
    txSearchLastQueryKey = queryKey;

    const qs = new URLSearchParams();
    if (provider) qs.set('provider', provider);
    if (startDate) qs.set('startDate', startDate);
    if (endDate) qs.set('endDate', endDate);
    qs.set('page', String(page));
    qs.set('size', String(size));

    console.debug('[tx-search]', qs.toString());

    const btn = document.getElementById('tx-search-btn');
    const done = btn ? setButtonLoading(btn, '검색 중...') : () => {};
    try {
        const res = await apiFetch(`/api/transactions/search?${qs.toString()}`);
        const data = await res.json();
        if (!res.ok || !data?.success) {
            throw new Error(data?.error || '검색에 실패했습니다.');
        }

        // 사용자 입력이 바뀐 뒤 늦게 도착한 응답은 무시
        if (txSearchLastQueryKey !== queryKey) {
            return;
        }

        const items = Array.isArray(data.items) ? data.items : [];
        if (!items.length) {
            const provUpper = String(provider || '').trim().toUpperCase();
            if (!noFallback && provUpper && provUpper !== 'ALL') {
                // provider mismatch 가능성(저장된 provider 값과 UI 선택값이 다를 때)을 대비해 1회 전체(ALL)로 재시도
                return await loadTxSearchIntoPreview({
                    provider: 'ALL',
                    startDate,
                    endDate,
                    page,
                    size,
                    __noFallback: true
                });
            }
            // 검색 결과가 0건이면 기존 프리뷰를 날리지 말고 안내만
            txSearchActive = false;
            renderPreviewSearchPagination();
            showToast('검색 결과가 없습니다.', 'info');
            return;
        }

        txSearchCurrentPage = Number(data.page ?? 0);
        txSearchLastTotalPages = Number(data.totalPages ?? 0);
        txSearchLastTotalElements = Number(data.totalElements ?? 0);
        txSearchLastStartDate = startDate;
        txSearchLastEndDate = endDate;
        txSearchLastSize = size;
        txSearchActive = true;

        previewRows = items.map(it => ({
            date: it?.txDate ?? '',
            description: it?.description ?? '',
            txType: it?.txType ?? '',
            txDetail: it?.txDetail ?? '',
            amount: (typeof it?.amount === 'number') ? it.amount : null,
            postBalance: (typeof it?.postBalance === 'number') ? it.postBalance : null,
            category: it?.category ?? '',
            _errors: []
        }));

        previewCurrentPage = 1;
        previewPageSize = previewRows.length || size;
        renderPreviewTable(previewRows);
        updatePreviewProviderInfo();
        setPreviewDataVisible(true);
        setConfirmedLock(true);

        renderPreviewSearchPagination();
    } catch (err) {
        if (String(err?.message || '') === 'UNAUTHORIZED') return;
        const msg = err?.message || '검색에 실패했습니다.';
        showToast(msg, 'error');
        // 실패 시에도 기존 프리뷰 유지
        txSearchActive = false;
        renderPreviewSearchPagination();
    } finally {
        done();
    }
}

function renderPreviewSearchPagination() {
    const wrap = document.getElementById('preview-pagination');
    if (!wrap) return;

    if (!txSearchActive) {
        // 일반 모드에서는 기존 페이징 로직 사용(업로드/월확정 내역)
        return;
    }

    const p = Number(txSearchCurrentPage) || 0;
    const tp = Number(txSearchLastTotalPages) || 0;
    const te = Number(txSearchLastTotalElements) || 0;

    if (tp <= 1) {
        wrap.innerHTML = te > 0 ? `<span class="page-info">총 ${te.toLocaleString('ko-KR')}건</span>` : '';
        return;
    }

    wrap.innerHTML = `
        <button id="preview-search-prev">이전</button>
        <span class="page-info">${p + 1} / ${tp} (총 ${te.toLocaleString('ko-KR')}건)</span>
        <button id="preview-search-next">다음</button>
    `;

    const prevBtn = document.getElementById('preview-search-prev');
    const nextBtn = document.getElementById('preview-search-next');
    if (prevBtn) {
        prevBtn.disabled = p <= 0;
        prevBtn.onclick = async () => {
            await loadTxSearchIntoPreview({
                provider: currentProvider,
                startDate: txSearchLastStartDate,
                endDate: txSearchLastEndDate,
                page: Math.max(0, p - 1),
                size: txSearchLastSize
            });
        };
    }
    if (nextBtn) {
        nextBtn.disabled = p >= tp - 1;
        nextBtn.onclick = async () => {
            await loadTxSearchIntoPreview({
                provider: currentProvider,
                startDate: txSearchLastStartDate,
                endDate: txSearchLastEndDate,
                page: Math.min(tp - 1, p + 1),
                size: txSearchLastSize
            });
        };
    }
}

function initFixedIncomeUI() {
    const modal = document.getElementById('fixed-income-modal');
    if (!modal || modal.dataset.bound) return;
    modal.dataset.bound = '1';

    const openBtn = document.getElementById('fixed-income-open-btn');
    const closeBtn = document.getElementById('fixed-income-modal-close');
    const addBtn = document.getElementById('fixed-income-add-btn');
    const resetBtn = document.getElementById('fixed-income-reset-btn');
    const filterEl = document.getElementById('fixed-income-filter');

    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            resetFixedIncomeForm();
        });
    }

    if (openBtn) {
        openBtn.addEventListener('click', () => {
            resetFixedIncomeForm();
            openFixedIncomeModal('add');
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', () => closeFixedIncomeModal());
    }

    if (addBtn) {
        addBtn.addEventListener('click', async () => {
            const payload = getFixedIncomeFormPayload();
            if (!payload) return;

            const done = setButtonLoading(addBtn, fixedIncomeEditingId ? '저장 중...' : '등록 중...');
            try {
                if (fixedIncomeEditingId) {
                    payload.id = fixedIncomeEditingId;
                    const res = await apiFetch('/api/fixed-incomes/update', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });
                    const data = await res.json();
                    if (!res.ok || !data?.success) {
                        throw new Error(data?.error || '수정에 실패했습니다.');
                    }
                    showToast('수정 완료', 'success');
                } else {
                    const res = await apiFetch('/api/fixed-incomes', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });
                    const data = await res.json();
                    if (!res.ok || !data?.success) {
                        throw new Error(data?.error || '등록에 실패했습니다.');
                    }
                    showToast('등록 완료', 'success');
                }

                resetFixedIncomeForm();
                await loadFixedIncomes();
                await loadFixedIncomeAutoSetting();
                closeFixedIncomeModal();
            } catch (err) {
                if (String(err?.message || '') === 'UNAUTHORIZED') return;
                const msg = err?.message || '요청에 실패했습니다.';
                showToast(msg, 'error');
            } finally {
                done();
            }
        });
    }

    if (filterEl) {
        filterEl.addEventListener('change', () => {
            loadFixedIncomes();
        });
    }

    const typeTabs = document.getElementById('fixed-income-type-tabs');
    if (typeTabs) {
        typeTabs.addEventListener('click', (e) => {
            const btn = e.target && e.target.closest ? e.target.closest('button[data-type]') : null;
            const nextType = btn ? btn.getAttribute('data-type') : null;
            if (!nextType) return;
            setFixedIncomeTypeFilter(nextType);
            renderFixedIncomeTable(fixedIncomeItems);
        });
    }

    setupFixedIncomeSortHeaders();
    loadFixedIncomes();
    loadFixedIncomeAutoSetting();
}

function setFixedIncomeTypeFilter(type) {
    const t = String(type || 'ALL').trim().toUpperCase();
    fixedIncomeTypeFilter = (t === 'EXTRA') ? 'EXTRA' : 'ALL';
    const tabs = document.querySelectorAll('#fixed-income-type-tabs .type-tab');
    tabs.forEach(el => {
        const v = String(el.getAttribute('data-type') || '').trim().toUpperCase();
        if (v === fixedIncomeTypeFilter) {
            el.classList.add('active');
        } else {
            el.classList.remove('active');
        }
    });
}

function getFixedIncomeFormPayload() {
    const titleEl = document.getElementById('fixed-income-title');
    const isExtraEl = document.getElementById('fixed-income-is-extra');
    const accountEl = document.getElementById('fixed-income-account');
    const amountEl = document.getElementById('fixed-income-amount');
    const categoryEl = document.getElementById('fixed-income-category');
    const dayEl = document.getElementById('fixed-income-day');
    const memoEl = document.getElementById('fixed-income-memo');

    const title = titleEl ? String(titleEl.value || '').trim() : '';
    const isExtra = !!(isExtraEl && isExtraEl.checked);
    const account = accountEl ? String(accountEl.value || '').trim() : '';
    const amount = amountEl ? Number(amountEl.value) : NaN;
    const category = categoryEl ? String(categoryEl.value || '').trim() : '';
    const payday = dayEl ? Number(dayEl.value) : NaN;
    const memo = memoEl ? String(memoEl.value || '').trim() : '';

    if (!title) {
        showToast('항목명을 입력해주세요.', 'error');
        return null;
    }
    if (!Number.isFinite(amount) || amount <= 0) {
        showToast('금액을 입력해주세요.', 'error');
        return null;
    }
    if (!Number.isFinite(payday) || payday < 1 || payday > 31) {
        showToast('입금일을 선택해주세요.', 'error');
        return null;
    }

    return {
        title,
        account: account || null,
        amount,
        category: category || null,
        payday,
        memo: memo || null,
        type: isExtra ? 'EXTRA' : 'NORMAL',
        status: 'ACTIVE'
    };
}

function resetFixedIncomeForm() {
    const titleEl = document.getElementById('fixed-income-title');
    const isExtraEl = document.getElementById('fixed-income-is-extra');
    const accountEl = document.getElementById('fixed-income-account');
    const amountEl = document.getElementById('fixed-income-amount');
    const categoryEl = document.getElementById('fixed-income-category');
    const dayEl = document.getElementById('fixed-income-day');
    const memoEl = document.getElementById('fixed-income-memo');
    const addBtn = document.getElementById('fixed-income-add-btn');

    if (titleEl) titleEl.value = '';
    if (isExtraEl) isExtraEl.checked = false;
    if (accountEl) accountEl.value = '';
    if (amountEl) amountEl.value = '';
    if (categoryEl) categoryEl.value = '';
    if (dayEl) dayEl.value = '';
    if (memoEl) memoEl.value = '';

    fixedIncomeEditingId = null;
    if (addBtn) addBtn.textContent = '추가';
    setFixedIncomeModalTitle('add');
}

function openFixedIncomeModal(mode) {
    const modal = document.getElementById('fixed-income-modal');
    if (!modal) return;
    modal.style.display = '';
    setFixedIncomeModalTitle(mode || 'add');
}

function closeFixedIncomeModal() {
    const modal = document.getElementById('fixed-income-modal');
    if (!modal) return;
    modal.style.display = 'none';
}

function setFixedIncomeModalTitle(mode) {
    const titleEl = document.getElementById('fixed-income-modal-title');
    if (!titleEl) return;
    titleEl.textContent = mode === 'edit' ? '고정 수입 수정' : '고정 수입 등록';
}

function setupFixedIncomeSortHeaders() {
    const headers = document.querySelectorAll('#fixed-income-table thead th.sortable');
    headers.forEach(th => {
        th.addEventListener('click', () => {
            const key = th.getAttribute('data-sort-key');
            if (!key) return;
            if (key === fixedIncomeSortKey) {
                fixedIncomeSortDir = fixedIncomeSortDir === 'asc' ? 'desc' : 'asc';
            } else {
                fixedIncomeSortKey = key;
                fixedIncomeSortDir = (key === 'amount' || key === 'payday') ? 'desc' : 'asc';
            }
            updateFixedIncomeSortIndicators();
            renderFixedIncomeTable(fixedIncomeItems);
        });
    });
    updateFixedIncomeSortIndicators();
}

function updateFixedIncomeSortIndicators() {
    const headers = document.querySelectorAll('#fixed-income-table thead th.sortable');
    headers.forEach(th => {
        const key = th.getAttribute('data-sort-key');
        if (key === fixedIncomeSortKey) {
            th.setAttribute('data-sort-dir', fixedIncomeSortDir);
        } else {
            th.removeAttribute('data-sort-dir');
        }
    });
}

async function loadFixedIncomes() {
    const filterEl = document.getElementById('fixed-income-filter');
    const status = filterEl ? String(filterEl.value || 'ALL') : 'ALL';
    try {
        const res = await apiFetch(`/api/fixed-incomes?status=${encodeURIComponent(status)}`);
        const data = await res.json();
        if (!res.ok || !data?.success) {
            throw new Error(data?.error || '목록을 불러오지 못했습니다.');
        }
        fixedIncomeItems = Array.isArray(data.items) ? data.items : [];
        renderFixedIncomeTable(fixedIncomeItems);
    } catch (err) {
        if (String(err?.message || '') === 'UNAUTHORIZED') return;
        showToast(err?.message || '목록을 불러오지 못했습니다.', 'error');
    }
}

async function updateFixedIncomeStatus(id, nextStatus) {
    const item = fixedIncomeItems.find(e => e.id === id);
    if (!item) {
        throw new Error('해당 항목을 찾을 수 없습니다.');
    }
    const payload = {
        id,
        title: item.title,
        account: item.account,
        amount: item.amount,
        category: item.category,
        payday: item.payday,
        memo: item.memo,
        type: item.type,
        status: nextStatus
    };

    const res = await apiFetch('/api/fixed-incomes/update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok || !data?.success) {
        throw new Error(data?.error || '상태 변경에 실패했습니다.');
    }
}

function renderFixedIncomeSummary(allItems, viewItems) {
    const totalEl = document.getElementById('fixed-income-summary-total');
    const activeEl = document.getElementById('fixed-income-summary-active');
    const nextEl = document.getElementById('fixed-income-summary-next');
    const extraEl = document.getElementById('fixed-income-summary-extra-total');
    if (!totalEl && !activeEl && !nextEl && !extraEl) return;

    const list = Array.isArray(allItems) ? allItems : [];
    const view = Array.isArray(viewItems) ? viewItems : [];

    const extraActive = list.filter(it => {
        const st = String(it?.status || 'ACTIVE').toUpperCase();
        const tp = String(it?.type || 'NORMAL').toUpperCase();
        return st !== 'PAUSED' && tp === 'EXTRA';
    });
    const extraTotal = extraActive.reduce((sum, it) => {
        const v = typeof it?.amount === 'number' ? it.amount : Number(it?.amount);
        return sum + (Number.isFinite(v) ? v : 0);
    }, 0);
    if (extraEl) extraEl.textContent = `${extraTotal.toLocaleString()}원`;

    const activeItems = view.filter(it => String(it?.status || 'ACTIVE') !== 'PAUSED');

    const totalAmount = activeItems.reduce((sum, it) => {
        const v = typeof it?.amount === 'number' ? it.amount : Number(it?.amount);
        return sum + (Number.isFinite(v) ? v : 0);
    }, 0);

    if (totalEl) totalEl.textContent = `${totalAmount.toLocaleString()}원`;
    if (activeEl) activeEl.textContent = `${activeItems.length}건`;

    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();

    let bestDiff = null;
    let bestDay = null;
    let bestCount = 0;

    const calcDiff = (payday) => {
        const day = Number(payday);
        if (!Number.isFinite(day) || day <= 0) return null;

        const daysInThisMonth = new Date(year, month + 1, 0).getDate();
        const safeDayThisMonth = Math.min(day, daysInThisMonth);
        let due = new Date(year, month, safeDayThisMonth);

        if (due < new Date(year, month, today.getDate())) {
            const nextMonthDays = new Date(year, month + 2, 0).getDate();
            const safeDayNext = Math.min(day, nextMonthDays);
            due = new Date(year, month + 1, safeDayNext);
        }

        const start = new Date(year, month, today.getDate());
        const diffMs = due.getTime() - start.getTime();
        return Math.round(diffMs / (1000 * 60 * 60 * 24));
    };

    activeItems.forEach(it => {
        const diff = calcDiff(it?.payday);
        if (diff === null) return;

        if (bestDiff === null || diff < bestDiff) {
            bestDiff = diff;
            bestDay = Number(it?.payday);
            bestCount = 1;
            return;
        }

        if (diff === bestDiff && Number(it?.payday) === bestDay) {
            bestCount += 1;
        }
    });

    if (nextEl) {
        if (bestDiff === null) {
            nextEl.textContent = '-';
        } else if (bestDiff === 0) {
            nextEl.textContent = `오늘(${bestDay}일) ${bestCount}건`;
        } else {
            nextEl.textContent = `${bestDiff}일 뒤(${bestDay}일) ${bestCount}건`;
        }
    }
}

function renderFixedIncomeTable(items) {
    const tbody = document.querySelector('#fixed-income-table tbody');
    const countEl = document.getElementById('fixed-income-count');
    if (!tbody) return;

    const allList = Array.isArray(items) ? [...items] : [];
    const filtered = fixedIncomeTypeFilter === 'EXTRA'
        ? allList.filter(it => String(it?.type || 'NORMAL').toUpperCase() === 'EXTRA')
        : allList;

    renderFixedIncomeSummary(allList, filtered);

    const list = [...filtered];
    list.sort((a, b) => {
        const dir = fixedIncomeSortDir === 'desc' ? -1 : 1;
        if (fixedIncomeSortKey === 'account') {
            const as = String(a?.account ?? '').trim();
            const bs = String(b?.account ?? '').trim();
            return as.localeCompare(bs, 'ko') * dir;
        }
        if (fixedIncomeSortKey === 'amount') {
            const av = typeof a?.amount === 'number' ? a.amount : -Infinity;
            const bv = typeof b?.amount === 'number' ? b.amount : -Infinity;
            return (av - bv) * dir;
        }
        if (fixedIncomeSortKey === 'payday') {
            const av = Number(a?.payday ?? -1);
            const bv = Number(b?.payday ?? -1);
            return (av - bv) * dir;
        }
        if (fixedIncomeSortKey === 'status') {
            const as = String(a?.status ?? '').trim();
            const bs = String(b?.status ?? '').trim();
            return as.localeCompare(bs, 'ko') * dir;
        }
        if (fixedIncomeSortKey === 'memo') {
            const as = String(a?.memo ?? '').trim();
            const bs = String(b?.memo ?? '').trim();
            return as.localeCompare(bs, 'ko') * dir;
        }
        if (fixedIncomeSortKey === 'category') {
            const as = String(a?.category ?? '').trim();
            const bs = String(b?.category ?? '').trim();
            return as.localeCompare(bs, 'ko') * dir;
        }
        const as = String(a?.title ?? '').trim();
        const bs = String(b?.title ?? '').trim();
        return as.localeCompare(bs, 'ko') * dir;
    });
    if (countEl) countEl.textContent = String(list.length);

    if (!list.length) {
        tbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="8">등록된 고정 수입이 없습니다.</td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = list.map(item => {
        const amountText = (typeof item.amount === 'number') ? item.amount.toLocaleString() : '';
        const isActive = item.status !== 'PAUSED';
        const statusText = isActive ? '활성' : '중지';
        const accountText = String(item.account ?? '');
        const disabledAttr = fixedIncomeMonthLocked ? 'disabled' : '';
        const disabledTitle = fixedIncomeMonthLocked ? 'title="선택한 월이 확정되어 수정/삭제할 수 없습니다. 확정 취소 후 변경하세요."' : '';
        return `
            <tr data-id="${item.id}">
                <td>${item.title ?? ''}</td>
                <td title="${accountText}">${item.account ?? '-'}</td>
                <td>${item.category ?? '-'}</td>
                <td>${item.payday ?? ''}일</td>
                <td>${amountText}</td>
                <td class="memo-col" title="${String(item.memo ?? '')}">${item.memo ?? '-'}</td>
                <td>
                    <span class="status-switch" title="${statusText}">
                        <label class="switch">
                            <input type="checkbox" class="fixed-income-status-toggle" data-id="${item.id}" ${isActive ? 'checked' : ''} ${disabledAttr}>
                            <span class="slider"></span>
                        </label>
                    </span>
                </td>
                <td>
                    <button class="edit-btn" data-action="edit" ${disabledAttr} ${disabledTitle}>수정</button>
                    <button class="edit-btn" data-action="delete" ${disabledAttr} ${disabledTitle}>삭제</button>
                </td>
            </tr>
        `;
    }).join('');

    tbody.querySelectorAll('input.fixed-income-status-toggle').forEach(toggle => {
        toggle.addEventListener('change', async (e) => {
            const el = e.currentTarget;
            const id = Number(el.getAttribute('data-id'));
            if (!id) return;
            const prevChecked = !el.checked;
            const nextStatus = el.checked ? 'ACTIVE' : 'PAUSED';
            el.disabled = true;
            try {
                await updateFixedIncomeStatus(id, nextStatus);
                showToast(nextStatus === 'ACTIVE' ? '활성으로 변경' : '중지로 변경', 'success');
                await loadFixedIncomes();
                await loadFixedIncomeAutoSetting();
            } catch (err) {
                el.checked = prevChecked;
                const msg = err?.message || '상태 변경에 실패했습니다.';
                showToast(msg, 'error');
            } finally {
                el.disabled = fixedIncomeMonthLocked;
            }
        });
    });

    tbody.querySelectorAll('button[data-action]').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const action = e.currentTarget.getAttribute('data-action');
            const row = e.currentTarget.closest('tr');
            const id = row ? Number(row.dataset.id) : null;
            if (!id) return;
            const item = fixedIncomeItems.find(entry => entry.id === id);
            if (!item) return;

            if (action === 'edit') {
                fillFixedIncomeForm(item);
                openFixedIncomeModal('edit');
                return;
            }

            if (action === 'delete') {
                if (!confirm('선택한 고정 수입을 삭제할까요?')) return;
                const btn = e.currentTarget;
                const done = setButtonLoading(btn, '삭제 중...');
                try {
                    const res = await apiFetch(`/api/fixed-incomes/${id}`, { method: 'DELETE' });
                    const data = await res.json();
                    if (!res.ok || !data?.success) {
                        throw new Error(data?.error || '삭제에 실패했습니다.');
                    }
                    await loadFixedIncomes();
                    await loadFixedIncomeAutoSetting();
                    showToast('삭제 완료', 'success');
                } catch (err) {
                    if (String(err?.message || '') === 'UNAUTHORIZED') return;
                    const msg = err?.message || '삭제에 실패했습니다.';
                    showToast(msg, 'error');
                } finally {
                    done();
                }
            }
        });
    });
}

function fillFixedIncomeForm(item) {
    const titleEl = document.getElementById('fixed-income-title');
    const isExtraEl = document.getElementById('fixed-income-is-extra');
    const accountEl = document.getElementById('fixed-income-account');
    const amountEl = document.getElementById('fixed-income-amount');
    const categoryEl = document.getElementById('fixed-income-category');
    const dayEl = document.getElementById('fixed-income-day');
    const memoEl = document.getElementById('fixed-income-memo');
    const addBtn = document.getElementById('fixed-income-add-btn');

    if (titleEl) titleEl.value = item.title ?? '';
    if (isExtraEl) {
        isExtraEl.checked = String(item?.type || 'NORMAL').toUpperCase() === 'EXTRA';
    }
    if (accountEl) accountEl.value = item.account ?? '';
    if (amountEl) amountEl.value = item.amount ?? '';
    if (categoryEl) categoryEl.value = item.category ?? '';
    if (dayEl) dayEl.value = item.payday ?? '';
    if (memoEl) memoEl.value = item.memo ?? '';

    fixedIncomeEditingId = item.id;
    if (addBtn) addBtn.textContent = '수정';
    setFixedIncomeModalTitle('edit');
}

async function loadFixedIncomeAutoSetting() {
    const enabledEl = document.getElementById('fixed-income-auto-enabled');
    const viewBtn = document.getElementById('fixed-income-auto-view-btn');
    const genBtn = document.getElementById('fixed-income-auto-generate-btn');
    const confirmBtn = document.getElementById('fixed-income-auto-confirm-btn');
    const unconfirmBtn = document.getElementById('fixed-income-auto-unconfirm-btn');
    const yearSel = document.getElementById('fixed-income-auto-year');
    const monthSel = document.getElementById('fixed-income-auto-month');
    const selectedSummaryEl = null;

    if (!enabledEl || !viewBtn || !genBtn || !confirmBtn || !unconfirmBtn || !yearSel || !monthSel) {
        return;
    }

    initFixedExpenseAutoMonthSelectors(yearSel, monthSel);
    setupFixedIncomeAutoModal();

    const refreshSelectedMonth = async () => {
        const y = parseInt(yearSel.value, 10);
        const m = parseInt(monthSel.value, 10);
        if (!Number.isFinite(y) || !Number.isFinite(m)) return;
        await loadFixedIncomeAutoMonthTransactions(y, m, selectedSummaryEl);
    };

    try {
        const res = await apiFetch('/api/fixed-incomes/auto/setting');
        const data = await res.json();
        if (!res.ok || !data?.success) {
            throw new Error(data?.error || '설정을 불러오지 못했습니다.');
        }

        enabledEl.checked = !!data?.enabled;

        enabledEl.onchange = async () => {
            try {
                const r = await apiFetch('/api/fixed-incomes/auto/setting', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ enabled: enabledEl.checked })
                });
                const d = await r.json();
                if (!r.ok || !d?.success) {
                    throw new Error(d?.error || '설정 저장에 실패했습니다.');
                }
            } catch (err) {
                if (String(err?.message || '') === 'UNAUTHORIZED') {
                    enabledEl.checked = !enabledEl.checked;
                    return;
                }
                const msg = err?.message || '설정 저장에 실패했습니다.';
                showToast(msg, 'error');
                enabledEl.checked = !enabledEl.checked;
            }
        };

        viewBtn.onclick = async () => {
            const y = parseInt(yearSel.value, 10);
            const m = parseInt(monthSel.value, 10);
            if (!Number.isFinite(y) || !Number.isFinite(m)) return;
            openFixedIncomeAutoModal(y, m);
            await loadFixedIncomeAutoMonthTransactions(y, m, selectedSummaryEl);
        };

        genBtn.onclick = async () => {
            const done = setButtonLoading(genBtn, '생성 중...');
            try {
                const y = parseInt(yearSel.value, 10);
                const m = parseInt(monthSel.value, 10);
                const r = await apiFetch(`/api/fixed-incomes/auto/generate?year=${encodeURIComponent(y)}&month=${encodeURIComponent(m)}`, { method: 'POST' });
                const d = await r.json();
                if (!r.ok || !d?.success) {
                    throw new Error(d?.error || '생성에 실패했습니다.');
                }
                showToast(d?.message || '생성 완료', 'success');
                await loadFixedIncomeAutoSetting();
                await loadDashboard();
            } catch (err) {
                if (String(err?.message || '') === 'UNAUTHORIZED') return;
                const msg = err?.message || '생성에 실패했습니다.';
                showToast(msg, 'error');
            } finally {
                done();
            }
        };

        confirmBtn.onclick = async () => {
            const done = setButtonLoading(confirmBtn, '확정 중...');
            try {
                const y = parseInt(yearSel.value, 10);
                const m = parseInt(monthSel.value, 10);
                const r = await apiFetch(`/api/fixed-incomes/auto/confirm?year=${encodeURIComponent(y)}&month=${encodeURIComponent(m)}`, { method: 'POST' });
                const d = await r.json();
                if (!r.ok || !d?.success) {
                    throw new Error(d?.error || '확정에 실패했습니다.');
                }
                const confirmed = Number(d?.confirmedCount ?? 0);
                showToast(d?.message || (confirmed === 0 ? '확정할 내역이 없습니다.' : `${confirmed}건 확정 완료`), 'success');
                await loadFixedIncomeAutoSetting();
                await loadDashboard();
                await loadFixedIncomes();
            } catch (err) {
                if (String(err?.message || '') === 'UNAUTHORIZED') return;
                const msg = err?.message || '확정에 실패했습니다.';
                showToast(msg, 'error');
            } finally {
                done();
            }
        };

        unconfirmBtn.onclick = async () => {
            const done = setButtonLoading(unconfirmBtn, '취소 중...');
            try {
                const y = parseInt(yearSel.value, 10);
                const m = parseInt(monthSel.value, 10);
                const r = await apiFetch(`/api/fixed-incomes/auto/unconfirm?year=${encodeURIComponent(y)}&month=${encodeURIComponent(m)}`, { method: 'POST' });
                const d = await r.json();
                if (!r.ok || !d?.success) {
                    throw new Error(d?.error || '확정 취소에 실패했습니다.');
                }
                showToast(d?.message || '확정 취소 완료', 'success');
                await loadFixedIncomeAutoSetting();
                await loadDashboard();
                await loadFixedIncomes();
            } catch (err) {
                if (String(err?.message || '') === 'UNAUTHORIZED') return;
                const msg = err?.message || '확정 취소에 실패했습니다.';
                showToast(msg, 'error');
            } finally {
                done();
            }
        };

        yearSel.onchange = refreshSelectedMonth;
        monthSel.onchange = refreshSelectedMonth;

        await refreshSelectedMonth();
    } catch (err) {
        if (String(err?.message || '') === 'UNAUTHORIZED') return;
        enabledEl.checked = false;
        applyFixedIncomeAutoButtonState({ total: 0, unconfirmed: 0, confirmed: 0 });
    }
}

function applyFixedIncomeAutoButtonState(state) {
    const genBtn = document.getElementById('fixed-income-auto-generate-btn');
    const confirmBtn = document.getElementById('fixed-income-auto-confirm-btn');
    const unconfirmBtn = document.getElementById('fixed-income-auto-unconfirm-btn');
    if (!genBtn || !confirmBtn || !unconfirmBtn) return;

    const total = Number(state?.total ?? 0);
    const unconfirmed = Number(state?.unconfirmed ?? 0);
    const confirmed = Number(state?.confirmed ?? 0);

    genBtn.disabled = !(total === 0 || unconfirmed > 0) ? true : false;
    confirmBtn.disabled = !(unconfirmed > 0);
    unconfirmBtn.disabled = !(confirmed > 0);
}

function setupFixedIncomeAutoModal() {
    const modal = document.getElementById('fixed-income-auto-modal');
    const closeBtn = document.getElementById('fixed-income-auto-modal-close');
    if (!modal || modal.dataset.bound) return;
    modal.dataset.bound = '1';

    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            modal.style.display = 'none';
        });
    }
}

function openFixedIncomeAutoModal(year, month) {
    const modal = document.getElementById('fixed-income-auto-modal');
    const titleEl = document.getElementById('fixed-income-auto-modal-title');
    if (!modal) return;
    if (titleEl) titleEl.textContent = `${year}년 ${month}월 고정수입 생성 내역`;
    modal.style.display = '';
}

async function loadFixedIncomeAutoMonthTransactions(year, month, summaryEl) {
    const table = document.getElementById('fixed-income-auto-table');
    if (!table) return;
    const tbody = table.querySelector('tbody');
    if (!tbody) return;

    try {
        if (summaryEl) {
            summaryEl.textContent = `${year}년 ${month}월: 불러오는 중...`;
        }
        const r = await apiFetch(`/api/fixed-incomes/auto/transactions?year=${encodeURIComponent(year)}&month=${encodeURIComponent(month)}`);
        const d = await r.json();
        if (!r.ok || !d?.success) {
            throw new Error(d?.error || '내역을 불러오지 못했습니다.');
        }

        const items = Array.isArray(d.items) ? d.items : [];
        const unconfirmed = Number(d.unconfirmedCount ?? 0);
        const confirmed = Number(d.confirmedCount ?? 0);

        fixedIncomeMonthLocked = confirmed > 0;
        renderFixedIncomeTable(fixedIncomeItems);

        applyFixedIncomeAutoButtonState({ total: items.length, unconfirmed, confirmed });

        if (summaryEl) {
            summaryEl.textContent = `${year}년 ${month}월: 총 ${items.length}건 (미확정 ${unconfirmed} / 확정 ${confirmed})`;
        }

        tbody.innerHTML = '';
        if (items.length === 0) {
            const tr = document.createElement('tr');
            tr.className = 'empty-row';
            tr.innerHTML = '<td colspan="5">선택한 월의 고정수입 생성 내역이 없습니다.</td>';
            tbody.appendChild(tr);
            return;
        }

        for (const it of items) {
            const tr = document.createElement('tr');
            const amt = typeof it.amount === 'number' ? it.amount : null;
            const absAmt = amt == null ? '' : formatNumber(Math.abs(amt));
            const conf = String(it.confirmed || 'N').toUpperCase() === 'Y' ? 'Y' : 'N';
            tr.innerHTML = `
                <td>${it.date || ''}</td>
                <td>${escapeHtml(it.description || '')}</td>
                <td>${escapeHtml(it.category || '')}</td>
                <td style="text-align:right;">${absAmt}</td>
                <td style="text-align:center;">${conf}</td>
            `;
            tbody.appendChild(tr);
        }
    } catch (err) {
        if (String(err?.message || '') === 'UNAUTHORIZED') return;
        fixedIncomeMonthLocked = false;
        renderFixedIncomeTable(fixedIncomeItems);
        applyFixedIncomeAutoButtonState({ total: 0, unconfirmed: 0, confirmed: 0 });
        const msg = err?.message ? String(err.message) : '내역을 불러오지 못했습니다.';
        if (summaryEl) summaryEl.textContent = `${year}년 ${month}월: ${msg}`;
        tbody.innerHTML = `<tr class="empty-row"><td colspan="5">${escapeHtml(msg)}</td></tr>`;
    }
}

async function loadFixedExpenseAutoSetting() {
    const enabledEl = document.getElementById('fixed-expense-auto-enabled');
    const viewBtn = document.getElementById('fixed-expense-auto-view-btn');
    const genBtn = document.getElementById('fixed-expense-auto-generate-btn');
    const confirmBtn = document.getElementById('fixed-expense-auto-confirm-btn');
    const unconfirmBtn = document.getElementById('fixed-expense-auto-unconfirm-btn');
    const yearSel = document.getElementById('fixed-expense-auto-year');
    const monthSel = document.getElementById('fixed-expense-auto-month');
    const selectedSummaryEl = null;

    if (!enabledEl || !viewBtn || !genBtn || !confirmBtn || !unconfirmBtn || !yearSel || !monthSel) {
        return;
    }

    initFixedExpenseAutoMonthSelectors(yearSel, monthSel);

    const refreshSelectedMonth = async () => {
        const y = parseInt(yearSel.value, 10);
        const m = parseInt(monthSel.value, 10);
        if (!isFinite(y) || !isFinite(m)) return;
        await loadFixedExpenseAutoMonthTransactions(y, m, selectedSummaryEl);
    };

    setupFixedExpenseAutoModal();
    viewBtn.onclick = async () => {
        const y = parseInt(yearSel.value, 10);
        const m = parseInt(monthSel.value, 10);
        if (!isFinite(y) || !isFinite(m)) return;
        openFixedExpenseAutoModal(y, m);
        await loadFixedExpenseAutoMonthTransactions(y, m, selectedSummaryEl);
    };

    try {
        const res = await apiFetch('/api/fixed-expenses/auto/setting');
        const data = await res.json();
        if (!res.ok || !data?.success) {
            throw new Error(data?.error || '설정을 불러오지 못했습니다.');
        }

        enabledEl.checked = Boolean(data.enabled);

        await refreshSelectedMonth();

        if (!yearSel.dataset.bound) {
            yearSel.dataset.bound = '1';
            yearSel.addEventListener('change', () => {
                refreshSelectedMonth();
            });
        }
        if (!monthSel.dataset.bound) {
            monthSel.dataset.bound = '1';
            monthSel.addEventListener('change', () => {
                refreshSelectedMonth();
            });
        }

        enabledEl.onchange = async () => {
            try {
                const r = await apiFetch('/api/fixed-expenses/auto/setting', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ enabled: enabledEl.checked })
                });
                const d = await r.json();
                if (!r.ok || !d?.success) {
                    throw new Error(d?.error || '설정 저장에 실패했습니다.');
                }
                enabledEl.checked = Boolean(d.enabled);
                showToast('저장 완료', 'success');
            } catch (err) {
                if (String(err?.message || '') === 'UNAUTHORIZED') return;
                const msg = err?.message || '설정 저장에 실패했습니다.';
                showToast(msg, 'error');
                enabledEl.checked = !enabledEl.checked;
            }
        };

        genBtn.onclick = async () => {
            const done = setButtonLoading(genBtn, '생성 중...');
            try {
                const y = parseInt(yearSel.value, 10);
                const m = parseInt(monthSel.value, 10);
                const r = await apiFetch(`/api/fixed-expenses/auto/generate?year=${encodeURIComponent(y)}&month=${encodeURIComponent(m)}`, { method: 'POST' });
                const d = await r.json();
                if (!r.ok || !d?.success) {
                    throw new Error(d?.error || '생성에 실패했습니다.');
                }
                const created = Number(d?.createdCount ?? 0);
                const skipped = Number(d?.skippedCount ?? 0);
                if (created === 0 && skipped > 0) {
                    showToast('이미 이번달 고정지출 내역이 생성되어 있습니다.', 'success');
                } else {
                    const msg = d?.message || `${created}건 생성 완료`;
                    showToast(msg, 'success');
                }
                await loadFixedExpenseAutoSetting();
            } catch (err) {
                if (String(err?.message || '') === 'UNAUTHORIZED') {
                    showToast('로그인이 필요합니다.', 'error');
                    return;
                }
                const raw = String(err?.message || '생성에 실패했습니다.');
                const msg = raw === 'Failed to fetch' ? '서버에 연결할 수 없습니다.' : raw;
                showToast(msg, 'error');
            } finally {
                done();
            }
        };

        confirmBtn.onclick = async () => {
            const done = setButtonLoading(confirmBtn, '확정 중...');
            try {
                const y = parseInt(yearSel.value, 10);
                const m = parseInt(monthSel.value, 10);
                const r = await apiFetch(`/api/fixed-expenses/auto/confirm?year=${encodeURIComponent(y)}&month=${encodeURIComponent(m)}`, { method: 'POST' });
                const d = await r.json();
                if (!r.ok || !d?.success) {
                    throw new Error(d?.error || '확정에 실패했습니다.');
                }
                const confirmed = Number(d?.confirmedCount ?? 0);
                showToast(d?.message || (confirmed === 0 ? '확정할 내역이 없습니다.' : `${confirmed}건 확정 완료`), 'success');
                await loadFixedExpenseAutoSetting();
                await loadDashboard();
                await loadFixedExpenses();
            } catch (err) {
                if (String(err?.message || '') === 'UNAUTHORIZED') {
                    showToast('로그인이 필요합니다.', 'error');
                    return;
                }
                const raw = String(err?.message || '확정에 실패했습니다.');
                const msg = raw === 'Failed to fetch' ? '서버에 연결할 수 없습니다.' : raw;
                showToast(msg, 'error');
            } finally {
                done();
            }
        };

        unconfirmBtn.onclick = async () => {
            const done = setButtonLoading(unconfirmBtn, '취소 중...');
            try {
                const y = parseInt(yearSel.value, 10);
                const m = parseInt(monthSel.value, 10);
                const r = await apiFetch(`/api/fixed-expenses/auto/unconfirm?year=${encodeURIComponent(y)}&month=${encodeURIComponent(m)}`, { method: 'POST' });
                const d = await r.json();
                if (!r.ok || !d?.success) {
                    throw new Error(d?.error || '확정 취소에 실패했습니다.');
                }
                showToast(d?.message || '확정 취소 완료', 'success');
                await loadFixedExpenseAutoSetting();
                await loadDashboard();
                await loadFixedExpenses();
            } catch (err) {
                if (String(err?.message || '') === 'UNAUTHORIZED') {
                    showToast('로그인이 필요합니다.', 'error');
                    return;
                }
                const raw = String(err?.message || '확정 취소에 실패했습니다.');
                const msg = raw === 'Failed to fetch' ? '서버에 연결할 수 없습니다.' : raw;
                showToast(msg, 'error');
            } finally {
                done();
            }
        };
    } catch (err) {
        if (String(err?.message || '') === 'UNAUTHORIZED') return;
        showToast(err?.message || '설정을 불러오지 못했습니다.', 'error');
    }
}

function setupFixedExpenseAutoModal() {
    const modal = document.getElementById('fixed-expense-auto-modal');
    const closeBtn = document.getElementById('fixed-expense-auto-modal-close');
    if (!modal || modal.dataset.bound) return;
    modal.dataset.bound = '1';

    if (closeBtn) {
        closeBtn.addEventListener('click', () => {
            modal.style.display = 'none';
        });
    }
}

function openFixedExpenseAutoModal(year, month) {
    const modal = document.getElementById('fixed-expense-auto-modal');
    const titleEl = document.getElementById('fixed-expense-auto-modal-title');
    if (!modal) return;
    if (titleEl) titleEl.textContent = `${year}년 ${month}월 고정지출 생성 내역`;
    modal.style.display = '';
}

function setupPanelTabs(containerId) {
    const container = document.getElementById(containerId);
    if (!container) return;
    const tabs = container.querySelectorAll('.panel-tab');
    if (!tabs.length) return;
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            const key = tab.getAttribute('data-tab');
            if (!key) return;
            tabs.forEach(t => t.classList.toggle('active', t === tab));
            const panels = document.querySelectorAll('.panel-tab-content');
            panels.forEach(panel => {
                panel.classList.toggle('active', panel.getAttribute('data-tab-panel') === key);
            });
        });
    });
}

function setupFixedExpenseSortHeaders() {
    const headers = document.querySelectorAll('#fixed-expense-table thead th.sortable');
    if (!headers.length) return;
    headers.forEach(th => {
        th.addEventListener('click', () => {
            const key = th.getAttribute('data-sort-key');
            if (!key) return;
            if (key === fixedExpenseSortKey) {
                fixedExpenseSortDir = fixedExpenseSortDir === 'asc' ? 'desc' : 'asc';
            } else {
                fixedExpenseSortKey = key;
                fixedExpenseSortDir = (key === 'amount' || key === 'billingDay') ? 'desc' : 'asc';
            }
            updateFixedExpenseSortIndicators();
            renderFixedExpenseTable(fixedExpenseItems);
        });
    });
    updateFixedExpenseSortIndicators();
}

function updateFixedExpenseSortIndicators() {
    const headers = document.querySelectorAll('#fixed-expense-table thead th.sortable');
    headers.forEach(th => {
        const key = th.getAttribute('data-sort-key');
        if (key === fixedExpenseSortKey) {
            th.setAttribute('data-sort-dir', fixedExpenseSortDir);
        } else {
            th.removeAttribute('data-sort-dir');
        }
    });
}

function getFixedExpenseFormPayload() {
    const titleEl = document.getElementById('fixed-expense-title');
    const isSubscriptionEl = document.getElementById('fixed-expense-is-subscription');
    const accountEl = document.getElementById('fixed-expense-account');
    const amountEl = document.getElementById('fixed-expense-amount');
    const categoryEl = document.getElementById('fixed-expense-category');
    const dayEl = document.getElementById('fixed-expense-day');
    const memoEl = document.getElementById('fixed-expense-memo');

    const title = titleEl ? titleEl.value.trim() : '';
    const isSubscription = !!(isSubscriptionEl && isSubscriptionEl.checked);
    const account = accountEl ? accountEl.value.trim() : '';
    const amount = amountEl ? Number(amountEl.value) : NaN;
    const billingDay = dayEl ? Number(dayEl.value) : NaN;
    const category = categoryEl ? categoryEl.value.trim() : '';
    const memo = memoEl ? memoEl.value.trim() : '';

    if (!title) {
        showToast('항목명을 입력해주세요.', 'error');
        return null;
    }
    if (!Number.isFinite(amount) || amount <= 0) {
        showToast('금액을 입력해주세요.', 'error');
        return null;
    }
    if (!Number.isFinite(billingDay) || billingDay < 1 || billingDay > 31) {
        showToast('결제일을 선택해주세요.', 'error');
        return null;
    }

    return {
        title,
        account: account || null,
        amount,
        category: category || null,
        billingDay,
        memo: memo || null,
        type: isSubscription ? 'SUBSCRIPTION' : 'NORMAL',
        status: 'ACTIVE'
    };
}

function resetFixedExpenseForm() {
    const titleEl = document.getElementById('fixed-expense-title');
    const isSubscriptionEl = document.getElementById('fixed-expense-is-subscription');
    const accountEl = document.getElementById('fixed-expense-account');
    const amountEl = document.getElementById('fixed-expense-amount');
    const categoryEl = document.getElementById('fixed-expense-category');
    const dayEl = document.getElementById('fixed-expense-day');
    const memoEl = document.getElementById('fixed-expense-memo');
    const addBtn = document.getElementById('fixed-expense-add-btn');

    if (titleEl) titleEl.value = '';
    if (isSubscriptionEl) isSubscriptionEl.checked = false;
    if (accountEl) accountEl.value = '';
    if (amountEl) amountEl.value = '';
    if (categoryEl) categoryEl.value = '';
    if (dayEl) dayEl.value = '';
    if (memoEl) memoEl.value = '';

    fixedExpenseEditingId = null;
    if (addBtn) addBtn.textContent = '추가';
    setFixedExpenseModalTitle('add');
}

function openFixedExpenseModal(mode) {
    const modal = document.getElementById('fixed-expense-modal');
    if (!modal) return;
    modal.style.display = '';
    setFixedExpenseModalTitle(mode || 'add');
}

function closeFixedExpenseModal() {
    const modal = document.getElementById('fixed-expense-modal');
    if (!modal) return;
    modal.style.display = 'none';
}

function setFixedExpenseModalTitle(mode) {
    const titleEl = document.getElementById('fixed-expense-modal-title');
    if (!titleEl) return;
    titleEl.textContent = mode === 'edit' ? '고정 지출 수정' : '고정 지출 등록';
}

function initFixedExpenseUI() {
    const modal = document.getElementById('fixed-expense-modal');
    if (!modal || modal.dataset.bound) return;
    modal.dataset.bound = '1';

    const openBtn = document.getElementById('fixed-expense-open-btn');
    const closeBtn = document.getElementById('fixed-expense-modal-close');
    const addBtn = document.getElementById('fixed-expense-add-btn');
    const resetBtn = document.getElementById('fixed-expense-reset-btn');
    const filterEl = document.getElementById('fixed-expense-filter');

    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            resetFixedExpenseForm();
        });
    }

    if (openBtn) {
        openBtn.addEventListener('click', () => {
            resetFixedExpenseForm();
            openFixedExpenseModal('add');
        });
    }

    if (closeBtn) {
        closeBtn.addEventListener('click', () => closeFixedExpenseModal());
    }

    if (addBtn) {
        addBtn.addEventListener('click', async () => {
            const payload = getFixedExpenseFormPayload();
            if (!payload) return;

            const done = setButtonLoading(addBtn, fixedExpenseEditingId ? '저장 중...' : '등록 중...');
            try {
                if (fixedExpenseEditingId) {
                    payload.id = fixedExpenseEditingId;
                    const res = await apiFetch('/api/fixed-expenses/update', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });
                    const data = await res.json();
                    if (!res.ok || !data?.success) {
                        throw new Error(data?.error || '수정에 실패했습니다.');
                    }
                    showToast('수정 완료', 'success');
                } else {
                    const res = await apiFetch('/api/fixed-expenses', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    });
                    const data = await res.json();
                    if (!res.ok || !data?.success) {
                        throw new Error(data?.error || '등록에 실패했습니다.');
                    }
                    showToast('등록 완료', 'success');
                }

                resetFixedExpenseForm();
                await loadFixedExpenses();
                await loadFixedExpenseAutoSetting();
                closeFixedExpenseModal();
            } catch (err) {
                if (String(err?.message || '') === 'UNAUTHORIZED') return;
                const msg = err?.message || '요청에 실패했습니다.';
                showToast(msg, 'error');
            } finally {
                done();
            }
        });
    }

    if (filterEl) {
        filterEl.addEventListener('change', () => {
            loadFixedExpenses();
        });
    }

    const typeTabs = document.getElementById('fixed-expense-type-tabs');
    if (typeTabs) {
        typeTabs.addEventListener('click', (e) => {
            const btn = e.target && e.target.closest ? e.target.closest('button[data-type]') : null;
            const nextType = btn ? btn.getAttribute('data-type') : null;
            if (!nextType) return;
            setFixedExpenseTypeFilter(nextType);
            renderFixedExpenseTable(fixedExpenseItems);
        });
    }

    setupFixedExpenseSortHeaders();
    loadFixedExpenses();
    loadFixedExpenseAutoSetting();
}

function setFixedExpenseTypeFilter(type) {
    const t = String(type || 'ALL').trim().toUpperCase();
    fixedExpenseTypeFilter = (t === 'SUBSCRIPTION') ? 'SUBSCRIPTION' : 'ALL';
    const tabs = document.querySelectorAll('#fixed-expense-type-tabs .type-tab');
    tabs.forEach(el => {
        const v = String(el.getAttribute('data-type') || '').trim().toUpperCase();
        if (v === fixedExpenseTypeFilter) {
            el.classList.add('active');
        } else {
            el.classList.remove('active');
        }
    });
}

async function loadFixedExpenses() {
    const filterEl = document.getElementById('fixed-expense-filter');
    const status = filterEl ? String(filterEl.value || 'ALL') : 'ALL';
    try {
        const res = await apiFetch(`/api/fixed-expenses?status=${encodeURIComponent(status)}`);
        const data = await res.json();
        if (!res.ok || !data?.success) {
            throw new Error(data?.error || '목록을 불러오지 못했습니다.');
        }
        fixedExpenseItems = Array.isArray(data.items) ? data.items : [];
        renderFixedExpenseTable(fixedExpenseItems);
    } catch (err) {
        if (String(err?.message || '') === 'UNAUTHORIZED') return;
        showToast(err?.message || '목록을 불러오지 못했습니다.', 'error');
    }
}

function renderFixedExpenseSummary(allItems, viewItems) {
    const totalEl = document.getElementById('fixed-expense-summary-total');
    const activeEl = document.getElementById('fixed-expense-summary-active');
    const nextEl = document.getElementById('fixed-expense-summary-next');
    const subTotalEl = document.getElementById('fixed-expense-summary-subscription-total');
    if (!totalEl && !activeEl && !nextEl && !subTotalEl) return;

    const list = Array.isArray(allItems) ? allItems : [];
    const view = Array.isArray(viewItems) ? viewItems : [];

    const subscriptionActive = list.filter(it => {
        const st = String(it?.status || 'ACTIVE').toUpperCase();
        const tp = String(it?.type || 'NORMAL').toUpperCase();
        return st !== 'PAUSED' && tp === 'SUBSCRIPTION';
    });
    const subTotalAmount = subscriptionActive.reduce((sum, it) => {
        const v = typeof it?.amount === 'number' ? it.amount : Number(it?.amount);
        return sum + (Number.isFinite(v) ? v : 0);
    }, 0);
    if (subTotalEl) subTotalEl.textContent = `${subTotalAmount.toLocaleString()}원`;

    const activeItems = view.filter(it => String(it?.status || 'ACTIVE') !== 'PAUSED');

    const totalAmount = activeItems.reduce((sum, it) => {
        const v = typeof it?.amount === 'number' ? it.amount : Number(it?.amount);
        return sum + (Number.isFinite(v) ? v : 0);
    }, 0);

    if (totalEl) totalEl.textContent = `${totalAmount.toLocaleString()}원`;
    if (activeEl) activeEl.textContent = `${activeItems.length}건`;

    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth();

    let bestDiff = null;
    let bestDay = null;
    let bestCount = 0;

    const calcDiff = (billingDay) => {
        const day = Number(billingDay);
        if (!Number.isFinite(day) || day <= 0) return null;

        const daysInThisMonth = new Date(year, month + 1, 0).getDate();
        const safeDayThisMonth = Math.min(day, daysInThisMonth);
        let due = new Date(year, month, safeDayThisMonth);

        if (due < new Date(year, month, today.getDate())) {
            const nextMonthDays = new Date(year, month + 2, 0).getDate();
            const safeDayNext = Math.min(day, nextMonthDays);
            due = new Date(year, month + 1, safeDayNext);
        }

        const start = new Date(year, month, today.getDate());
        const diffMs = due.getTime() - start.getTime();
        return Math.round(diffMs / (1000 * 60 * 60 * 24));
    };

    activeItems.forEach(it => {
        const diff = calcDiff(it?.billingDay);
        if (diff === null) return;

        if (bestDiff === null || diff < bestDiff) {
            bestDiff = diff;
            bestDay = Number(it?.billingDay);
            bestCount = 1;
            return;
        }

        if (diff === bestDiff && Number(it?.billingDay) === bestDay) {
            bestCount += 1;
        }
    });

    if (nextEl) {
        if (bestDiff === null) {
            nextEl.textContent = '-';
        } else if (bestDiff === 0) {
            nextEl.textContent = `오늘(${bestDay}일) ${bestCount}건`;
        } else {
            nextEl.textContent = `${bestDiff}일 뒤(${bestDay}일) ${bestCount}건`;
        }
    }
}

function renderFixedExpenseTable(items) {
    const tbody = document.querySelector('#fixed-expense-table tbody');
    const countEl = document.getElementById('fixed-expense-count');
    if (!tbody) return;

    const allList = Array.isArray(items) ? [...items] : [];
    const filtered = fixedExpenseTypeFilter === 'SUBSCRIPTION'
        ? allList.filter(it => String(it?.type || 'NORMAL').toUpperCase() === 'SUBSCRIPTION')
        : allList;

    renderFixedExpenseSummary(allList, filtered);

    const list = [...filtered];
    list.sort((a, b) => {
        const dir = fixedExpenseSortDir === 'desc' ? -1 : 1;
        if (fixedExpenseSortKey === 'account') {
            const as = String(a?.account ?? '').trim();
            const bs = String(b?.account ?? '').trim();
            return as.localeCompare(bs, 'ko') * dir;
        }
        if (fixedExpenseSortKey === 'amount') {
            const av = typeof a?.amount === 'number' ? a.amount : -Infinity;
            const bv = typeof b?.amount === 'number' ? b.amount : -Infinity;
            return (av - bv) * dir;
        }
        if (fixedExpenseSortKey === 'billingDay') {
            const av = Number(a?.billingDay ?? -1);
            const bv = Number(b?.billingDay ?? -1);
            return (av - bv) * dir;
        }
        if (fixedExpenseSortKey === 'status') {
            const as = String(a?.status ?? '').trim();
            const bs = String(b?.status ?? '').trim();
            return as.localeCompare(bs, 'ko') * dir;
        }
        if (fixedExpenseSortKey === 'memo') {
            const as = String(a?.memo ?? '').trim();
            const bs = String(b?.memo ?? '').trim();
            return as.localeCompare(bs, 'ko') * dir;
        }
        if (fixedExpenseSortKey === 'category') {
            const as = String(a?.category ?? '').trim();
            const bs = String(b?.category ?? '').trim();
            return as.localeCompare(bs, 'ko') * dir;
        }
        const as = String(a?.title ?? '').trim();
        const bs = String(b?.title ?? '').trim();
        return as.localeCompare(bs, 'ko') * dir;
    });
    if (countEl) countEl.textContent = String(list.length);

    if (!list.length) {
        tbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="8">등록된 고정 지출이 없습니다.</td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = list.map(item => {
        const amountText = (typeof item.amount === 'number') ? item.amount.toLocaleString() : '';
        const isActive = item.status !== 'PAUSED';
        const statusText = isActive ? '활성' : '중지';
        const accountText = String(item.account ?? '');
        const disabledAttr = fixedExpenseMonthLocked ? 'disabled' : '';
        const disabledTitle = fixedExpenseMonthLocked ? 'title="선택한 월이 확정되어 수정/삭제할 수 없습니다. 확정 취소 후 변경하세요."' : '';
        return `
            <tr data-id="${item.id}">
                <td>${item.title ?? ''}</td>
                <td title="${accountText}">${item.account ?? '-'}</td>
                <td>${item.category ?? '-'}</td>
                <td>${item.billingDay ?? ''}일</td>
                <td>${amountText}</td>
                <td class="memo-col" title="${String(item.memo ?? '')}">${item.memo ?? '-'}</td>
                <td>
                    <span class="status-switch" title="${statusText}">
                        <label class="switch">
                            <input type="checkbox" class="fixed-expense-status-toggle" data-id="${item.id}" ${isActive ? 'checked' : ''} ${disabledAttr}>
                            <span class="slider"></span>
                        </label>
                    </span>
                </td>
                <td>
                    <button class="edit-btn" data-action="edit" ${disabledAttr} ${disabledTitle}>수정</button>
                    <button class="edit-btn" data-action="delete" ${disabledAttr} ${disabledTitle}>삭제</button>
                </td>
            </tr>
        `;
    }).join('');

    tbody.querySelectorAll('input.fixed-expense-status-toggle').forEach(toggle => {
        toggle.addEventListener('change', async (e) => {
            const el = e.currentTarget;
            const id = Number(el.getAttribute('data-id'));
            if (!id) return;
            const prevChecked = !el.checked;
            const nextStatus = el.checked ? 'ACTIVE' : 'PAUSED';
            el.disabled = true;
            try {
                await updateFixedExpenseStatus(id, nextStatus);
                showToast(nextStatus === 'ACTIVE' ? '활성으로 변경' : '중지로 변경', 'success');
                await loadFixedExpenses();
                await loadFixedExpenseAutoSetting();
            } catch (err) {
                el.checked = prevChecked;
                const msg = err?.message || '상태 변경에 실패했습니다.';
                showToast(msg, 'error');
            } finally {
                el.disabled = fixedExpenseMonthLocked;
            }
        });
    });

    tbody.querySelectorAll('button[data-action]').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const action = e.currentTarget.getAttribute('data-action');
            const row = e.currentTarget.closest('tr');
            const id = row ? Number(row.dataset.id) : null;
            if (!id) return;
            const item = fixedExpenseItems.find(entry => entry.id === id);
            if (!item) return;

            if (action === 'edit') {
                fillFixedExpenseForm(item);
                openFixedExpenseModal('edit');
                return;
            }

            if (action === 'delete') {
                if (!confirm('선택한 고정 지출을 삭제할까요?')) return;
                const btn = e.currentTarget;
                const done = setButtonLoading(btn, '삭제 중...');
                try {
                    const res = await apiFetch(`/api/fixed-expenses/${id}`, { method: 'DELETE' });
                    const data = await res.json();
                    if (!res.ok || !data?.success) {
                        throw new Error(data?.error || '삭제에 실패했습니다.');
                    }
                    await loadFixedExpenses();
                    await loadFixedExpenseAutoSetting();
                    showToast('삭제 완료', 'success');
                } catch (err) {
                    if (String(err?.message || '') === 'UNAUTHORIZED') return;
                    const msg = err?.message || '삭제에 실패했습니다.';
                    showToast(msg, 'error');
                } finally {
                    done();
                }
            }
        });
    });
}

function fillFixedExpenseForm(item) {
    const titleEl = document.getElementById('fixed-expense-title');
    const isSubscriptionEl = document.getElementById('fixed-expense-is-subscription');
    const accountEl = document.getElementById('fixed-expense-account');
    const amountEl = document.getElementById('fixed-expense-amount');
    const categoryEl = document.getElementById('fixed-expense-category');
    const dayEl = document.getElementById('fixed-expense-day');
    const memoEl = document.getElementById('fixed-expense-memo');
    const addBtn = document.getElementById('fixed-expense-add-btn');

    if (titleEl) titleEl.value = item.title ?? '';
    if (isSubscriptionEl) {
        isSubscriptionEl.checked = String(item?.type || 'NORMAL').toUpperCase() === 'SUBSCRIPTION';
    }
    if (accountEl) accountEl.value = item.account ?? '';
    if (amountEl) amountEl.value = item.amount ?? '';
    if (categoryEl) categoryEl.value = item.category ?? '';
    if (dayEl) dayEl.value = item.billingDay ?? '';
    if (memoEl) memoEl.value = item.memo ?? '';

    fixedExpenseEditingId = item.id;
    if (addBtn) addBtn.textContent = '수정';
}

function showMessage(element, message, type = 'info') {
    if (type === 'loading') {
        // 진행 중: 로딩 스피너 표시
        let overlay = document.getElementById('loading-overlay');
        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'loading-overlay';
            overlay.className = 'loading-overlay';
            overlay.innerHTML = `
                <div>
                    <div class="loading-spinner"></div>
                    <div class="loading-text">${message}</div>
                </div>
            `;
            document.body.appendChild(overlay);
        } else {
            overlay.querySelector('.loading-text').textContent = message;
            overlay.style.display = 'flex';
        }
    } else {
        // 결과(완료/실패): toast로 표시
        hideLoading();
        const t = String(type || 'info').toLowerCase();
        const toastType = (t === 'error' || t === 'danger') ? 'error'
            : (t === 'success') ? 'success'
                : 'info';
        showToast(message, toastType);
    }
}

function hideLoading() {
    const overlay = document.getElementById('loading-overlay');
    if (overlay) {
        overlay.style.display = 'none';
    }
}

function updateUploadButton() {
    const file = selectedUploadFile;
    const provider = currentProvider;
    const btn = document.getElementById('upload-next-btn');
    if (!btn) return;

    if (isCurrentMonthConfirmed) {
        btn.disabled = true;
        btn.style.opacity = '0.5';
        btn.style.cursor = 'not-allowed';
        return;
    }

    const fileOk = file && file.name.toLowerCase().endsWith('.pdf');
    const providerOk = provider && provider.trim() !== '';
    // 버튼은 비활성화하지 않고 시각적으로만 비활성화처럼 보이게
    if (fileOk && providerOk) {
        btn.disabled = false;
        btn.style.opacity = '1';
        btn.style.cursor = 'pointer';
    } else {
        btn.disabled = false; // 클릭 이벤트를 받기 위해 활성화 상태 유지
        btn.style.opacity = '0.5';
        btn.style.cursor = 'not-allowed';
    }
}

function clearFileSelection() {
    const dropzone = document.getElementById('upload-dropzone');
    const fileInput = document.getElementById('upload-file-input');
    const selectedFileDiv = document.getElementById('selected-file');
    const pdfPasswordGroup = document.getElementById('pdf-password-group');
    const pdfPasswordInput = document.getElementById('pdf-password-input');
    const uploadSection = dropzone?.closest('.upload-section');

    selectedUploadFile = null;
    updateUploadButton();
    fileInput.value = '';

    setPreviewDataVisible(false);
    
    if (selectedFileDiv) {
        selectedFileDiv.style.display = 'none';
    }
    const selectedFileName = document.getElementById('selected-file-name');
    if (selectedFileName) {
        selectedFileName.textContent = '';
    }
    if (dropzone) {
        dropzone.style.display = 'block';
    }
    if (pdfPasswordGroup) {
        pdfPasswordGroup.style.display = 'none';
    }
    if (pdfPasswordInput) {
        pdfPasswordInput.value = '';
    }
    
    // 기존 메시지 제거
    if (uploadSection) {
        const existingMessages = uploadSection.querySelectorAll('.error, .success, .loading');
        existingMessages.forEach(msg => msg.remove());
    }

    // 현재 은행/월 상태에서도 파일/데이터 제거
    const p = String(currentProvider || '').trim().toUpperCase();
    if (bankMonthState?.[p]?.[currentYear]) {
        delete bankMonthState[p][currentYear][currentMonth];
    }

    clearUploadFileState(p, currentYear, currentMonth);
}

function setupUploadStep() {
    const uploadSection = document.querySelector('.upload-section');
    const dropzone = document.getElementById('upload-dropzone');
    const fileInput = document.getElementById('upload-file-input');
    const uploadBtn = document.getElementById('upload-next-btn');
    const selectedFileDiv = document.getElementById('selected-file');
    const selectedFileName = document.getElementById('selected-file-name');
    const fileRemoveBtn = document.getElementById('file-remove-btn');
    const pdfPasswordGroup = document.getElementById('pdf-password-group');
    const pdfPasswordInput = document.getElementById('pdf-password-input');

    // 업로드/파일 변경/삭제 잠금
    if (uploadBtn) {
        uploadBtn.disabled = !!isCurrentMonthConfirmed;
        uploadBtn.style.opacity = isCurrentMonthConfirmed ? '0.5' : '1';
        uploadBtn.style.cursor = isCurrentMonthConfirmed ? 'not-allowed' : 'pointer';
    }

    if (!dropzone || !fileInput || !uploadBtn) {
        return;
    }

    function setFile(file) {
        if (!file) {
            return;
        }
        const ext = '.' + file.name.split('.').pop().toLowerCase();
        const validTypes = ['.pdf'];
        if (!validTypes.includes(ext)) {
            selectedUploadFile = null;
            updateUploadButton();
            showMessage(uploadSection, 'PDF 파일만 업로드 가능합니다.', 'error');
            return;
        }
        selectedUploadFile = file;
        setUploadFileState(currentProvider, currentYear, currentMonth, file);
        updateUploadButton();
        // 파일 선택 시에는 성공 메시지만 띄우고 파일 정보 영역은 숨김
        showMessage(uploadSection, `선택된 파일: ${file.name}`, 'success');

        // 파일 선택 즉시 파일명 표시 (사용자가 어떤 파일을 올렸는지 항상 확인 가능)
        if (selectedFileDiv && selectedFileName) {
            selectedFileName.textContent = file.name;
            selectedFileDiv.style.display = 'block';
        }
        // 업로드(dropzone) 영역은 계속 유지 (사용자가 바로 다른 파일로 교체 가능)

        if (pdfPasswordGroup) {
            pdfPasswordGroup.style.display = 'none';
        }
        if (pdfPasswordInput) {
            pdfPasswordInput.value = '';
        }
    }

    dropzone.addEventListener('click', () => {
        if (!requireLoginForUpload(uploadSection)) return;
        fileInput.click();
    });

    dropzone.addEventListener('dragover', (e) => {
        e.preventDefault();
        if (!isLoggedIn) {
            return;
        }
        dropzone.classList.add('dragover');
    });

    dropzone.addEventListener('dragleave', () => {
        dropzone.classList.remove('dragover');
    });

    dropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropzone.classList.remove('dragover');
        if (!requireLoginForUpload(uploadSection)) return;
        const files = e.dataTransfer.files;
        if (files && files.length > 0) {
            setFile(files[0]);
        }
    });

    fileInput.addEventListener('change', (e) => {
        if (!requireLoginForUpload(uploadSection)) {
            fileInput.value = '';
            return;
        }
        const files = e.target.files;
        if (files && files.length > 0) {
            setFile(files[0]);
        }
    });

    // 파일 삭제 버튼 이벤트
    if (fileRemoveBtn) {
        fileRemoveBtn.addEventListener('click', (e) => {
            if (e && typeof e.stopPropagation === 'function') {
                e.stopPropagation();
            }
            if (e && typeof e.preventDefault === 'function') {
                e.preventDefault();
            }
            clearFileSelection();
        });
    }

    uploadBtn.addEventListener('click', async () => {
        if (!requireLoginForUpload(uploadSection)) return;
        if (!selectedUploadFile) {
            if (selectedFileName && String(selectedFileName.textContent || '').trim() !== '') {
                showMessage(uploadSection, '파일을 다시 선택해주세요.', 'error');
                fileInput.click();
                return;
            }
            showMessage(uploadSection, '파일을 선택해주세요.', 'error');
            return;
        }

        if (!currentProvider || String(currentProvider).trim() === '') {
            showMessage(uploadSection, '은행/카드사를 선택해주세요.', 'error');
            return;
        }

        if (!supportedProviders.includes(String(currentProvider).trim().toUpperCase())) {
            showMessage(uploadSection, '현재 선택한 은행/카드사는 아직 지원하지 않습니다.', 'error');
            return;
        }

        showMessage(uploadSection, '파일을 읽는 중...', 'loading');

        try {
            const formData = new FormData();
            formData.append('file', selectedUploadFile);
            formData.append('provider', currentProvider);

            const ext = '.' + selectedUploadFile.name.split('.').pop().toLowerCase();
            if (ext === '.pdf' && pdfPasswordInput && pdfPasswordInput.value) {
                formData.append('pdfPassword', pdfPasswordInput.value);
            }

            const response = await apiFetch('/api/import/preview', {
                method: 'POST',
                body: formData
            });
            const data = await response.json();
            if (!response.ok || !data?.success) {
                throw new Error(data?.error || '프리뷰 생성에 실패했습니다.');
            }

            // 성공적으로 파싱되면 PDF 비밀번호 입력은 다시 숨김/초기화
            if (pdfPasswordGroup) {
                pdfPasswordGroup.style.display = 'none';
            }
            if (pdfPasswordInput) {
                pdfPasswordInput.value = '';
            }

            previewRows = (data.rows || []).map(r => {
                const errors = Array.isArray(r.errors) ? r.errors : [];
                const base = {
                    date: r.date ?? '',
                    description: r.description ?? '',
                    txType: r.txType ?? '',
                    txDetail: r.txDetail ?? '',
                    amount: (typeof r.amount === 'number') ? r.amount : null,
                    postBalance: (typeof r.postBalance === 'number') ? r.postBalance : null,
                    category: r.category ?? '',
                    _errors: errors
                };
                if (shouldAutoSuggestCategory(base.category)) {
                    base.category = suggestCategoryForRow(base);
                }
                return base;
            });

            // 파싱 실패(은행 선택/서식 불일치 등): 에러 row만 오는 케이스
            const hasAnyDataRow = hasAnyPreviewDataRow(previewRows);

            if (!hasAnyDataRow) {
                setPreviewDataVisible(false);
                const allErrors = previewRows
                    .flatMap(r => Array.isArray(r._errors) ? r._errors : [])
                    .filter(Boolean);
                const msg = allErrors.length > 0
                    ? allErrors.join(' / ')
                    : '파일 읽기에 실패했습니다. 은행 선택이 올바른지 확인해주세요.';
                showMessage(uploadSection, msg, 'error');

                const isPasswordError = allErrors.some(err =>
                    String(err || '').includes('PDF에 비밀번호가 걸려있습니다')
                    || String(err || '').includes('PDF 비밀번호가 올바르지 않습니다')
                );
                if (isPasswordError) {
                    if (pdfPasswordGroup) {
                        pdfPasswordGroup.style.display = '';
                    }
                    if (pdfPasswordInput) {
                        pdfPasswordInput.focus();
                    }
                    if (!bankMonthState[currentProvider]) {
                        bankMonthState[currentProvider] = {};
                    }
                    if (!bankMonthState[currentProvider][currentYear]) {
                        bankMonthState[currentProvider][currentYear] = {};
                    }
                    bankMonthState[currentProvider][currentYear][currentMonth] = {
                        rows: [],
                        errorMessage: msg,
                        passwordRequired: true
                    };
                }

                // 은행/월 상태 저장(에러만 저장)
                if (!bankMonthState[currentProvider]) {
                    bankMonthState[currentProvider] = {};
                }
                if (!bankMonthState[currentProvider][currentYear]) {
                    bankMonthState[currentProvider][currentYear] = {};
                }
                bankMonthState[currentProvider][currentYear][currentMonth] = {
                    rows: [],
                    errorMessage: msg
                };
                return;
            }

            // PDF 비밀번호 관련 에러가 있으면 업로드 화면에 멈추고 에러 표시
            const hasPasswordError = previewRows.some(r => 
                r._errors.some(err => 
                    err.includes('PDF에 비밀번호가 걸려있습니다') || 
                    err.includes('PDF 비밀번호가 올바르지 않습니다')
                )
            );

            if (hasPasswordError) {
                const errorMsg = previewRows[0]._errors.find(err => 
                    err.includes('PDF에 비밀번호가 걸려있습니다') || 
                    err.includes('PDF 비밀번호가 올바르지 않습니다')
                );
                showMessage(uploadSection, errorMsg, 'error');

                // 비밀번호 걸린 PDF일 때만 입력란 노출
                if (pdfPasswordGroup) {
                    pdfPasswordGroup.style.display = '';
                }
                if (pdfPasswordInput) {
                    pdfPasswordInput.focus();
                }

                if (!bankMonthState[currentProvider]) {
                    bankMonthState[currentProvider] = {};
                }
                if (!bankMonthState[currentProvider][currentYear]) {
                    bankMonthState[currentProvider][currentYear] = {};
                }
                bankMonthState[currentProvider][currentYear][currentMonth] = {
                    rows: [],
                    errorMessage: errorMsg,
                    passwordRequired: true
                };
                return; // 프리뷰로 넘어가지 않음
            }

            // 초기 표시 건수(기본 20)를 먼저 적용 후 렌더링
            const sizeEl = document.getElementById('tx-search-size');
            const size = sizeEl ? Number(sizeEl.value) : NaN;
            previewPageSize = (Number.isFinite(size) && size > 0) ? size : 20;
            previewCurrentPage = 1;
            renderPreviewTable(previewRows);
            updatePreviewProviderInfo();
            showMessage(uploadSection, '파일 읽기가 완료되었습니다.', 'success');
            setPreviewDataVisible(true);
            
            // 은행/월 상태 저장
            saveBankMonthState(currentProvider, currentYear, currentMonth);
        } catch (err) {
            if (String(err?.message || '') === 'UNAUTHORIZED') {
                showMessage(uploadSection, '로그인이 필요합니다.', 'error');
                return;
            }
            showMessage(uploadSection, err?.message || '프리뷰 생성에 실패했습니다.', 'error');
        }
    });
}

function updatePreviewProviderInfo() {
    const infoEl = document.getElementById('preview-provider-info');
    if (!infoEl) return;

    const provider = currentProvider;
    const label = getProviderLabel(provider);

    if (!previewRows.length) {
        infoEl.textContent = `${label} ${currentYear}년 ${currentMonth}월`;
        return;
    }

    const dates = previewRows.map(r => r.date).filter(Boolean);
    const minDate = dates.length ? Math.min(...dates.map(d => new Date(d).getTime())) : null;
    const maxDate = dates.length ? Math.max(...dates.map(d => new Date(d).getTime())) : null;

    let periodText = '';
    if (minDate && maxDate) {
        const min = new Date(minDate);
        const max = new Date(maxDate);
        const minStr = `${min.getFullYear()}년 ${min.getMonth() + 1}월`;
        const maxStr = `${max.getFullYear()}년 ${max.getMonth() + 1}월`;
        periodText = minStr === maxStr ? minStr : `${minStr} ~ ${maxStr}`;
    }

    infoEl.textContent = periodText ? `${label} ${periodText} 거래내역` : `${label} 거래내역`;
}

function renderPreviewTable(rows) {
    const tbody = document.getElementById('preview-tbody');
    if (!tbody) {
        return;
    }

    const base = Array.isArray(rows) ? rows : [];
    const candidates = base.map((_, i) => i).filter(i => {
        const r = base[i];

        if (getPreviewMiscOnly()) {
            if (String(r?.category ?? '').trim() !== '기타') return false;
        }

        // 미확정(프리뷰) 상태에서만 로컬 날짜 필터 적용
        if (!isCurrentMonthConfirmed) {
            const sd = previewDateFilterStart ? Date.parse(previewDateFilterStart) : NaN;
            const ed = previewDateFilterEnd ? Date.parse(previewDateFilterEnd) : NaN;
            if (!isNaN(sd) || !isNaN(ed)) {
                const dt = r?.date ? Date.parse(r.date) : NaN;
                if (isNaN(dt)) return false;
                if (!isNaN(sd) && dt < sd) return false;
                if (!isNaN(ed) && dt > ed) return false;
            }
        }

        return true;
    });

    const total = candidates.length;
    const totalPages = Math.max(1, Math.ceil(total / previewPageSize));
    if (previewCurrentPage < 1) previewCurrentPage = 1;
    if (previewCurrentPage > totalPages) previewCurrentPage = totalPages;

    if (total === 0) {
        tbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="10">표시할 데이터가 없습니다.</td>
            </tr>
        `;
        window.__previewVisiblePageIndices = [];
        const selectAll = document.getElementById('preview-select-all');
        if (selectAll) selectAll.checked = false;
        updatePreviewSelectedCount();
        updatePreviewMeta([]);
        renderPreviewPagination(1);
        return;
    }

    // 정렬 + index 보존(정렬/페이징에서도 edit가 원본 previewRows 기준으로 동작)
    const indices = [...candidates];
    const d = (String(previewSortDir || 'desc').toLowerCase() === 'asc') ? 'asc' : 'desc';
    const cmp = (ai, bi) => {
        const a = base[ai];
        const b = base[bi];

        if (previewSortKey === 'date') {
            const at = a?.date ? new Date(a.date).getTime() : NaN;
            const bt = b?.date ? new Date(b.date).getTime() : NaN;
            if (isNaN(at) && isNaN(bt)) return 0;
            if (isNaN(at)) return 1;
            if (isNaN(bt)) return -1;
            return at - bt;
        }

        if (previewSortKey === 'amount') {
            const av = (typeof a?.amount === 'number' && !isNaN(a.amount)) ? a.amount : null;
            const bv = (typeof b?.amount === 'number' && !isNaN(b.amount)) ? b.amount : null;
            if (av == null && bv == null) return 0;
            if (av == null) return 1;
            if (bv == null) return -1;
            return av - bv;
        }

        if (previewSortKey === 'postBalance') {
            const av = (typeof a?.postBalance === 'number' && !isNaN(a.postBalance)) ? a.postBalance : null;
            const bv = (typeof b?.postBalance === 'number' && !isNaN(b.postBalance)) ? b.postBalance : null;
            if (av == null && bv == null) return 0;
            if (av == null) return 1;
            if (bv == null) return -1;
            return av - bv;
        }

        if (previewSortKey === 'category') {
            const as = String(a?.category ?? '').trim();
            const bs = String(b?.category ?? '').trim();
            return as.localeCompare(bs, 'ko');
        }

        if (previewSortKey === 'txType') {
            const as = String(a?.txType ?? '').trim();
            const bs = String(b?.txType ?? '').trim();
            return as.localeCompare(bs, 'ko');
        }

        if (previewSortKey === 'txDetail') {
            const as = String(a?.txDetail ?? '').trim();
            const bs = String(b?.txDetail ?? '').trim();
            return as.localeCompare(bs, 'ko');
        }

        const as = String(a?.description ?? '').trim();
        const bs = String(b?.description ?? '').trim();
        return as.localeCompare(bs, 'ko');
    };
    indices.sort((ai, bi) => {
        const v = cmp(ai, bi);
        return d === 'asc' ? v : -v;
    });

    const start = (previewCurrentPage - 1) * previewPageSize;
    const pageIndices = indices.slice(start, start + previewPageSize);

    // store for select-all behavior
    window.__previewVisiblePageIndices = pageIndices;

    tbody.innerHTML = pageIndices.map((rowIndex) => {
        const r = base[rowIndex];
        const isError = r._errors && r._errors.length > 0;
        const amountText = (typeof r.amount === 'number') ? r.amount.toLocaleString() : '';
        const postBalanceText = (typeof r.postBalance === 'number') ? r.postBalance.toLocaleString() : '';
        const rowClass = isError ? 'row-error' : '';
        const title = isError ? r._errors.join(', ') : '';

        const checked = previewSelectedIndices.has(rowIndex) ? 'checked' : '';

        return `
            <tr class="${rowClass}" title="${title}">
                <td style="text-align:center;">
                    <input type="checkbox" class="preview-row-check" data-index="${rowIndex}" ${checked} />
                </td>
                <td>${r.date ?? ''}</td>
                <td>${r.description ?? ''}</td>
                <td>${r.txType ?? ''}</td>
                <td>${r.txDetail ?? ''}</td>
                <td>${amountText}</td>
                <td>${postBalanceText}</td>
                <td>${r.category ?? ''}</td>
                <td>
                    <button class="edit-btn" onclick="editRow(${rowIndex})" title="수정">✏️</button>
                </td>
            </tr>
        `;
    }).join('');

    tbody.querySelectorAll('input.preview-row-check').forEach(chk => {
        chk.addEventListener('change', (e) => {
            const el = e.currentTarget;
            const idx = Number(el.getAttribute('data-index'));
            if (!Number.isFinite(idx)) return;
            if (el.checked) {
                previewSelectedIndices.add(idx);
            } else {
                previewSelectedIndices.delete(idx);
            }
            updatePreviewSelectedCount();

            const selectAll = document.getElementById('preview-select-all');
            if (selectAll) {
                const currentPage = Array.isArray(window.__previewVisiblePageIndices) ? window.__previewVisiblePageIndices : [];
                const allChecked = currentPage.length > 0 && currentPage.every(i => previewSelectedIndices.has(i));
                selectAll.checked = allChecked;
            }
        });
    });

    const selectAll = document.getElementById('preview-select-all');
    if (selectAll) {
        const allChecked = pageIndices.length > 0 && pageIndices.every(i => previewSelectedIndices.has(i));
        selectAll.checked = allChecked;
    }

    updatePreviewSelectedCount();

    const viewRows = indices.map(i => base[i]);
    updatePreviewMeta(viewRows);

    renderPreviewPagination(totalPages);
}

function getPreviewRowStats(rows) {
    const total = Array.isArray(rows) ? rows.length : 0;
    if (!total) {
        return { total: 0, valid: 0, error: 0 };
    }
    const valid = rows.filter(r => {
        if (!r || (Array.isArray(r._errors) && r._errors.length > 0)) return false;
        if (!r.date || !/^\d{4}-\d{2}-\d{2}$/.test(String(r.date))) return false;
        if (!r.description || !String(r.description).trim()) return false;
        return typeof r.amount === 'number' && !isNaN(r.amount);
    }).length;
    return { total, valid, error: total - valid };
}

function updatePreviewMeta(rows) {
    const metaEl = document.getElementById('preview-meta');
    const hintEl = document.getElementById('preview-hint');
    if (!metaEl) return;

    const stats = getPreviewRowStats(rows || previewRows || []);
    metaEl.innerHTML = `총 <span class="count-strong">${stats.total}</span>건 · 정상 <span class="count-strong">${stats.valid}</span>건 · 오류 <span class="${stats.error > 0 ? 'count-error' : 'count-strong'}">${stats.error}</span>건`;
    if (hintEl) {
        hintEl.style.display = stats.total ? '' : 'none';
    }
}

function renderPreviewPagination(totalPages) {
    const wrap = document.getElementById('preview-pagination');
    if (!wrap) return;

    if (totalPages <= 1) {
        wrap.innerHTML = '';
        return;
    }

    wrap.innerHTML = `
        <button id="preview-page-prev">이전</button>
        <span class="page-info">${previewCurrentPage} / ${totalPages}</span>
        <button id="preview-page-next">다음</button>
    `;

    const prevBtn = document.getElementById('preview-page-prev');
    const nextBtn = document.getElementById('preview-page-next');
    if (prevBtn) {
        prevBtn.disabled = previewCurrentPage <= 1;
        prevBtn.onclick = () => {
            previewCurrentPage = Math.max(1, previewCurrentPage - 1);
            renderPreviewTable(previewRows);
        };
    }
    if (nextBtn) {
        nextBtn.disabled = previewCurrentPage >= totalPages;
        nextBtn.onclick = () => {
            previewCurrentPage = Math.min(totalPages, previewCurrentPage + 1);
            renderPreviewTable(previewRows);
        };
    }
}

function sortPreviewRowsByKey(rows, key, dir) {
    const sorted = [...rows];
    const d = (String(dir || 'desc').toLowerCase() === 'asc') ? 'asc' : 'desc';

    const cmp = (a, b) => {
        if (key === 'date') {
            const at = a?.date ? new Date(a.date).getTime() : NaN;
            const bt = b?.date ? new Date(b.date).getTime() : NaN;
            if (isNaN(at) && isNaN(bt)) return 0;
            if (isNaN(at)) return 1;
            if (isNaN(bt)) return -1;
            return at - bt;
        }

        if (key === 'amount') {
            const av = (typeof a?.amount === 'number' && !isNaN(a.amount)) ? a.amount : null;
            const bv = (typeof b?.amount === 'number' && !isNaN(b.amount)) ? b.amount : null;
            if (av == null && bv == null) return 0;
            if (av == null) return 1;
            if (bv == null) return -1;
            return av - bv;
        }

        if (key === 'category') {
            const as = String(a?.category ?? '').trim();
            const bs = String(b?.category ?? '').trim();
            return as.localeCompare(bs, 'ko');
        }

        const as = String(a?.description ?? '').trim();
        const bs = String(b?.description ?? '').trim();
        return as.localeCompare(bs, 'ko');
    };

    sorted.sort((a, b) => {
        const v = cmp(a, b);
        return d === 'asc' ? v : -v;
    });
    return sorted;
}

function updateSortHeaderIndicators() {
    const headers = document.querySelectorAll('#preview-table thead th.sortable');
    headers.forEach(th => {
        const key = th.getAttribute('data-sort-key');
        if (key === previewSortKey) {
            th.setAttribute('data-sort-dir', previewSortDir);
        } else {
            th.removeAttribute('data-sort-dir');
        }
    });
}

 function setupPreviewStep() {
     const sortableHeaders = document.querySelectorAll('#preview-table thead th.sortable');
     sortableHeaders.forEach(th => {
         th.addEventListener('click', () => {
             const key = th.getAttribute('data-sort-key');
            if (!key) return;
            if (key === previewSortKey) {
                previewSortDir = (previewSortDir === 'asc') ? 'desc' : 'asc';
            } else {
                previewSortKey = key;
                previewSortDir = (key === 'date' || key === 'amount') ? 'desc' : 'asc';
            }
            updateSortHeaderIndicators();
            previewCurrentPage = 1;
            renderPreviewTable(previewRows);
        });
    });

    updateSortHeaderIndicators();
}

function setupPreviewActions() {
    const previewSection = document.getElementById('preview-data-area');
    const saveBtn = document.getElementById('preview-save-btn');
    if (!saveBtn) {
        return;
    }
    saveBtn.addEventListener('click', async () => {
        if (isCurrentMonthConfirmed) {
            showToast('이미 확정된 월입니다. 수정/재저장이 불가합니다.', 'error');
            return;
        }
        if (!previewRows.length) {
            showToast('저장할 데이터가 없습니다.', 'error');
            return;
        }

        const provider = String(currentProvider || '').trim().toUpperCase();
        const rows = Array.isArray(previewRows)
            ? previewRows.map(r => ({
                date: r?.date ?? '',
                description: r?.description ?? '',
                txType: r?.txType ?? '',
                txDetail: r?.txDetail ?? '',
                amount: (typeof r?.amount === 'number') ? r.amount : null,
                postBalance: (typeof r?.postBalance === 'number') ? r.postBalance : null,
                category: r?.category ?? '',
                errors: Array.isArray(r?._errors) ? r._errors : []
            }))
            : [];

        const hasValid = rows.some(r => (!r.errors || r.errors.length === 0) && r.date && r.description && typeof r.amount === 'number');
        if (!hasValid) {
            showMessage(previewSection, '저장 가능한 데이터가 없습니다. (오류가 있는 행은 저장되지 않습니다.)', 'error');
            return;
        }

        showMessage(previewSection, 'DB에 저장 중...', 'loading');

        try {
            const res = await apiFetch('/api/import/confirm', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    provider,
                    rows
                })
            });

            const result = await res.json();
            if (!res.ok || !result?.success) {
                throw new Error(result?.error || 'DB 저장에 실패했습니다.');
            }

            showMessage(previewSection, `확정 완료 (${result.inserted || 0}건)`, 'success');

            // 저장된 데이터의 년도로 currentYear를 맞추고, DB 데이터로 다시 로드하여 잠금
            const ym = inferYearMonthFromRows(previewRows);
            if (ym) {
                currentYear = ym.year;
                currentMonth = ym.month;
                syncPreviewYearTabs();
                document.querySelectorAll('.month-tab').forEach(tab => {
                    tab.classList.toggle('active', parseInt(tab.dataset.month) === currentMonth);
                });
            } else {
                syncCurrentYearFromRows(previewRows);
                syncPreviewYearTabs();
            }

            setConfirmedLock(true);

            // 저장 완료 후 DB 데이터로 다시 로드하고 잠금
            await loadConfirmedMonthForYear(provider, currentYear, currentMonth);
        } catch (err) {
            if (String(err?.message || '') === 'UNAUTHORIZED') {
                showMessage(previewSection, '로그인이 필요합니다.', 'error');
                return;
            }
            showMessage(previewSection, err?.message || 'DB 저장에 실패했습니다.', 'error');
        }
    });
}

// 행 수정 함수
function editRow(index) {
    if (isCurrentMonthConfirmed) {
        showToast('확정된 데이터는 수정할 수 없습니다.', 'error');
        return;
    }
    const row = previewRows[index];
    if (!row) return;
    
    // 수정 다이얼로그 생성
    const dialog = document.createElement('div');
    dialog.className = 'edit-dialog';
    dialog.innerHTML = `
        <div class="edit-dialog-content">
            <h3>데이터 수정</h3>
            <div class="edit-form">
                <div class="form-group">
                    <label>날짜</label>
                    <input type="date" id="edit-date" value="${row.date || ''}">
                </div>
                <div class="form-group">
                    <label>내용</label>
                    <input type="text" id="edit-description" value="${row.description || ''}" placeholder="거래 내용">
                </div>
                <div class="form-group">
                    <label>금액</label>
                    <input type="number" id="edit-amount" value="${row.amount || ''}" placeholder="숫자만 입력">
                </div>
                <div class="form-group">
                    <label>카테고리</label>
                    <select id="edit-category">
                        ${renderCategoryOptions(row.category)}
                    </select>
                </div>
            </div>
            <div class="edit-actions">
                <button class="execute-btn" onclick="closeEditDialog()">취소</button>
                <button class="execute-btn" onclick="saveEdit(${index})">저장</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(dialog);
}

// 수정 다이얼로그 닫기
function closeEditDialog() {
    const dialog = document.querySelector('.edit-dialog');
    if (dialog) {
        dialog.remove();
    }
}

// 수정 저장
function saveEdit(index) {
    if (isCurrentMonthConfirmed) {
        showToast('확정된 데이터는 수정할 수 없습니다.', 'error');
        closeEditDialog();
        return;
    }
    const date = document.getElementById('edit-date').value;
    const description = document.getElementById('edit-description').value.trim();
    const amount = parseFloat(document.getElementById('edit-amount').value) || null;
    let category = document.getElementById('edit-category').value;
    
    // 유효성 검사
    const errors = [];
    if (!date) {
        errors.push('날짜 누락');
    } else if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
        errors.push('날짜 형식 오류');
    }
    if (!description) errors.push('내용 누락');
    if (amount === null || isNaN(amount)) errors.push('금액 오류');

    if (!category || !String(category).trim()) {
        category = '기타';
    }
    
    // 데이터 업데이트 (기존 확장 필드 유지)
    const prev = previewRows[index] || {};
    previewRows[index] = {
        ...prev,
        date,
        description,
        amount,
        category,
        _errors: errors
    };
    
    // 테이블 다시 렌더링
    renderPreviewTable(previewRows);
    
    // 다이얼로그 닫기
    closeEditDialog();
    
    // 메시지 표시
    const previewSection = document.querySelector('#preview-content .form-section');
    if (errors.length > 0) {
        showMessage(previewSection, '수정했지만 오류가 남아있습니다.', 'error');
    } else {
        showMessage(previewSection, '수정이 완료되었습니다.', 'success');
    }
}

// initApp() is already bound on DOMContentLoaded above
