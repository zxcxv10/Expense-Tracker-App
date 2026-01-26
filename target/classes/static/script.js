let selectedUploadFile = null;
let previewRows = [];
let currentMonth = new Date().getMonth() + 1; // 1~12
let currentYear = new Date().getFullYear();
let currentProvider = 'TOSS';
let bankMonthState = {}; // { [provider:string]: { [month:number]: { fileName: string, rows: any[], errorMessage?: string } } }

let isLoggedIn = false;

async function apiFetch(url, options) {
    const res = await fetch(url, { ...(options || {}), credentials: 'same-origin' });
    if (res.status === 401) {
        alert('로그인이 필요합니다.');
        openAuthModal('login');
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
    for (let y = nowY - 3; y <= nowY + 1; y++) {
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
    } catch (e) {
        hideLoading();
        chartsWrap.style.display = 'none';
        if (summaryWrap) summaryWrap.style.display = 'none';
        placeholder.style.display = '';
        alert(e?.message || '대시보드 로딩 실패');
    }
}

function renderDashboardSummaryNumbers(monthly, selectedMonth) {
    const incomeEl = document.getElementById('dashboard-summary-income');
    const expenseEl = document.getElementById('dashboard-summary-expense');
    const balanceEl = document.getElementById('dashboard-summary-balance');
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

const supportedProviders = ['TOSS', 'NH', 'KB', 'HYUNDAI'];

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

    const uploadBtn = document.getElementById('upload-next-btn');
    const saveBtn = document.getElementById('preview-save-btn');
    const dropzone = document.getElementById('upload-dropzone');
    const fileInput = document.getElementById('upload-file-input');
    const fileRemoveBtn = document.getElementById('file-remove-btn');

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
            currentYear,
            currentYear - 1,
            currentYear + 1,
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

function saveBankMonthState(provider, month) {
    const p = String(provider || '').trim().toUpperCase();
    if (!p) return;
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
        if (bankMonthState[p]) {
            delete bankMonthState[p][month];
        }
        return;
    }

    if (!bankMonthState[p]) {
        bankMonthState[p] = {};
    }
    bankMonthState[p][month] = {
        rows: hasData ? rows : [],
        errorMessage: hasData ? '' : errorMessage,
        passwordRequired
    };
}

function restoreBankMonthState(provider, month) {
    const p = String(provider || '').trim().toUpperCase();
    if (!p) return;
    if (!month || month < 1 || month > 12) return;

    const selectedFileDiv = document.getElementById('selected-file');
    const selectedFileName = document.getElementById('selected-file-name');
    const pdfPasswordGroup = document.getElementById('pdf-password-group');
    const pdfPasswordInput = document.getElementById('pdf-password-input');
    const uploadSection = document.getElementById('upload-dropzone')?.closest('.upload-section');

    // 파일 객체는 복원 불가하므로 항상 null로 리셋
    selectedUploadFile = null;

    const state = bankMonthState?.[p]?.[month];
    if (!state) {
        previewRows = [];
        renderPreviewTable([]);
        setPreviewDataVisible(false);

        if (selectedFileDiv) {
            selectedFileDiv.style.display = 'none';
        }
        if (selectedFileName) {
            selectedFileName.textContent = '';
        }

        updateUploadButton();
        updatePreviewProviderInfo();

        // 확정 데이터가 있으면 DB에서 로드 (async)
        loadConfirmedMonthIfAny(p, month);
        return;
    }

    previewRows = Array.isArray(state.rows) ? state.rows.map(r => ({ ...r })) : [];
    previewCurrentPage = 1;
    renderPreviewTable(previewRows);
    setPreviewDataVisible(hasAnyPreviewDataRow(previewRows));

    if (state.errorMessage && state.errorMessage.trim() !== '' && uploadSection) {
        showMessage(uploadSection, state.errorMessage, 'error');
    }

    // 파일 객체(File)는 복원 불가하므로, 탭 이동 시 파일명 표시는 항상 초기화한다.
    if (selectedFileDiv) {
        selectedFileDiv.style.display = 'none';
    }
    if (selectedFileName) {
        selectedFileName.textContent = '';
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
    loadConfirmedMonthIfAny(p, month);
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
            saveBankMonthState(currentProvider, currentMonth);

            currentProvider = p;
            tabs.forEach(t => t.classList.toggle('active', t === btn));

            // 선택된 은행/월 상태 복원 (없으면 초기화)
            restoreBankMonthState(currentProvider, currentMonth);
        });
    });
}

function initSidebarMenu() {
    const menuItems = document.querySelectorAll('.menu-item');

    menuItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            const targetMenu = item.dataset.menu;
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
    saveBankMonthState(currentProvider, currentMonth);

    currentMonth = month;
    document.querySelectorAll('.month-tab').forEach(tab => {
        tab.classList.toggle('active', parseInt(tab.dataset.month) === month);
    });

    // 선택된 은행/월 상태 복원 (없으면 초기화)
    restoreBankMonthState(currentProvider, month);
}

function showContentPanel(panelId) {
    const contentPanels = document.querySelectorAll('.content-panel');
    contentPanels.forEach(panel => {
        panel.classList.remove('active');
        if (panel.id === `${panelId}-content`) {
            panel.classList.add('active');
        }
    });
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
            userEl.textContent = '로그인 필요';
            userEl.style.display = '';
            openBtn.style.display = '';
            logoutBtn.style.display = 'none';
            return;
        }

        isLoggedIn = true;
        userEl.textContent = `${data.username}`;
        userEl.style.display = '';
        openBtn.style.display = 'none';
        logoutBtn.style.display = '';
    } catch {
        isLoggedIn = false;
        userEl.textContent = '로그인 필요';
        userEl.style.display = '';
        openBtn.style.display = '';
        logoutBtn.style.display = 'none';
    }
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
        alert('아이디를 입력해주세요.');
        return;
    }
    if (!password) {
        alert('비밀번호를 입력해주세요.');
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
        alert(data?.error || '로그인 실패');
        return;
    }

    await refreshAuthUI();
    closeAuthModal();
    // 로그인 성공 시 대시보드와 확정 상태 자동 로드
    await loadDashboard();
    await loadConfirmedMonthIfAny(currentProvider, currentMonth);
}

async function doAuthSignup() {
    const usernameEl = document.getElementById('auth-signup-username');
    const passwordEl = document.getElementById('auth-signup-password');
    const username = usernameEl ? String(usernameEl.value || '').trim() : '';
    const password = passwordEl ? String(passwordEl.value || '') : '';
    if (!username) {
        alert('아이디를 입력해주세요.');
        return;
    }
    if (!password) {
        alert('비밀번호를 입력해주세요.');
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
        alert(data?.error || '회원가입 실패');
        return;
    }

    await refreshAuthUI();
    closeAuthModal();
    // 회원가입 성공 시 대시보드와 확정 상태 자동 로드
    await loadDashboard();
    await loadConfirmedMonthIfAny(currentProvider, currentMonth);
}

async function doAuthLogout() {
    const res = await fetch('/api/auth/logout', { method: 'POST', credentials: 'same-origin' });
    if (!res.ok) {
        const data = await res.json().catch(() => null);
        alert(data?.error || '로그아웃 실패');
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
    if (modal) {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeAuthModal();
            }
        });
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

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            const m = document.getElementById('auth-modal');
            if (m && m.style.display !== 'none') {
                closeAuthModal();
            }
        }
    });

    await refreshAuthUI();
}

async function initApp() {
    initMonthTabs();
    initBankTabs();
    initSidebarMenu();
    await initAuth();
    setupUploadStep();
    setupPreviewStep();
    setupPreviewActions();
    initDashboardControls();
    setPreviewDataVisible(false);

    if (isLoggedIn) {
        loadDashboard();
        loadConfirmedMonthIfAny(currentProvider, currentMonth);
    }

    // 초기 은행/월 상태에서도 파일/데이터 제거
    const p = String(currentProvider || '').trim().toUpperCase();
    if (bankMonthState?.[p]) {
        delete bankMonthState[p][currentMonth];
    }
}

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', initApp);

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
        // 결과(완료/실패): alert 창
        hideLoading();
        alert(message);
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
    if (bankMonthState?.[p]) {
        delete bankMonthState[p][currentMonth];
    }
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
        fileRemoveBtn.addEventListener('click', () => {
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
                return {
                    date: r.date ?? '',
                    description: r.description ?? '',
                    amount: (typeof r.amount === 'number') ? r.amount : null,
                    category: r.category ?? '',
                    _errors: errors
                };
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
                    bankMonthState[currentProvider][currentMonth] = {
                        rows: [],
                        errorMessage: msg,
                        passwordRequired: true
                    };
                }

                // 은행/월 상태 저장(에러만 저장)
                if (!bankMonthState[currentProvider]) {
                    bankMonthState[currentProvider] = {};
                }
                bankMonthState[currentProvider][currentMonth] = {
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
                bankMonthState[currentProvider][currentMonth] = {
                    rows: [],
                    errorMessage: errorMsg,
                    passwordRequired: true
                };
                return; // 프리뷰로 넘어가지 않음
            }

            renderPreviewTable(previewRows);
            previewCurrentPage = 1;
            updatePreviewProviderInfo();
            showMessage(uploadSection, '파일 읽기가 완료되었습니다.', 'success');
            setPreviewDataVisible(true);
            
            // 은행/월 상태 저장
            saveBankMonthState(currentProvider, currentMonth);
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

    const total = Array.isArray(rows) ? rows.length : 0;
    const totalPages = Math.max(1, Math.ceil(total / previewPageSize));
    if (previewCurrentPage < 1) previewCurrentPage = 1;
    if (previewCurrentPage > totalPages) previewCurrentPage = totalPages;

    // 정렬 + index 보존(정렬/페이징에서도 edit가 원본 previewRows 기준으로 동작)
    const indices = Array.from({ length: total }, (_, i) => i);
    const d = (String(previewSortDir || 'desc').toLowerCase() === 'asc') ? 'asc' : 'desc';
    const cmp = (ai, bi) => {
        const a = rows[ai];
        const b = rows[bi];

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

        if (previewSortKey === 'category') {
            const as = String(a?.category ?? '').trim();
            const bs = String(b?.category ?? '').trim();
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

    tbody.innerHTML = pageIndices.map((rowIndex) => {
        const r = rows[rowIndex];
        const isError = r._errors && r._errors.length > 0;
        const amountText = (typeof r.amount === 'number') ? r.amount.toLocaleString() : '';
        const rowClass = isError ? 'row-error' : '';
        const title = isError ? r._errors.join(', ') : '';

        return `
            <tr class="${rowClass}" title="${title}">
                <td>${r.date ?? ''}</td>
                <td>${r.description ?? ''}</td>
                <td>${amountText}</td>
                <td>${r.category ?? ''}</td>
                <td>
                    <button class="edit-btn" onclick="editRow(${rowIndex})" title="수정">✏️</button>
                </td>
            </tr>
        `;
    }).join('');

    renderPreviewPagination(totalPages);
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
            alert('이미 확정된 월입니다. 수정/재저장이 불가합니다.');
            return;
        }
        if (!previewRows.length) {
            alert('저장할 데이터가 없습니다.');
            return;
        }

        const provider = String(currentProvider || '').trim().toUpperCase();
        const rows = Array.isArray(previewRows)
            ? previewRows.map(r => ({
                date: r?.date ?? '',
                description: r?.description ?? '',
                amount: (typeof r?.amount === 'number') ? r.amount : null,
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
                document.querySelectorAll('.month-tab').forEach(tab => {
                    tab.classList.toggle('active', parseInt(tab.dataset.month) === currentMonth);
                });
            } else {
                syncCurrentYearFromRows(previewRows);
            }

            setConfirmedLock(true);

            // 저장 완료 후 DB 데이터로 다시 로드하고 잠금
            await loadConfirmedMonthIfAny(provider, currentMonth);
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
                        <option value="">선택</option>
                        <option value="식비" ${row.category === '식비' ? 'selected' : ''}>식비</option>
                        <option value="교통" ${row.category === '교통' ? 'selected' : ''}>교통</option>
                        <option value="카페" ${row.category === '카페' ? 'selected' : ''}>카페</option>
                        <option value="쇼핑" ${row.category === '쇼핑' ? 'selected' : ''}>쇼핑</option>
                        <option value="문화" ${row.category === '문화' ? 'selected' : ''}>문화</option>
                        <option value="의료" ${row.category === '의료' ? 'selected' : ''}>의료</option>
                        <option value="교육" ${row.category === '교육' ? 'selected' : ''}>교육</option>
                        <option value="수입" ${row.category === '수입' ? 'selected' : ''}>수입</option>
                        <option value="기타" ${row.category === '기타' ? 'selected' : ''}>기타</option>
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
    const date = document.getElementById('edit-date').value;
    const description = document.getElementById('edit-description').value.trim();
    const amount = parseFloat(document.getElementById('edit-amount').value) || null;
    const category = document.getElementById('edit-category').value;
    
    // 유효성 검사
    const errors = [];
    if (!date) errors.push('날짜 누락');
    if (!description) errors.push('내용 누락');
    if (amount === null || isNaN(amount)) errors.push('금액 오류');
    
    // 데이터 업데이트
    previewRows[index] = {
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
