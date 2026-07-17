/**
 * Reports Main Controller
 * Handles tab switching, filters, AJAX calls, and shared utilities
 */
const ReportsMain = (() => {
    'use strict';

    let currentTab = 'overview';
    const loadedTabs = new Set();
    const chartInstances = {};

    // ===== ApexCharts shared theme =====
    function getChartTheme() {
        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
        return {
            mode: isDark ? 'dark' : 'light',
            palette: 'palette1',
            monochrome: { enabled: false }
        };
    }

    function getChartColors() {
        return ['#171717', '#525252', '#737373', '#a3a3a3', '#d4d4d4', '#404040', '#e5e5e5'];
    }

    function getBaseChartOptions() {
        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
        return {
            chart: {
                fontFamily: "'Cairo', 'Noto Sans Arabic', sans-serif",
                background: 'transparent',
                toolbar: { show: true, tools: { download: true, selection: false, zoom: false, zoomin: false, zoomout: false, pan: false, reset: false } },
                animations: { enabled: true, easing: 'easeinout', speed: 600 },
            },
            theme: getChartTheme(),
            colors: getChartColors(),
            grid: {
                borderColor: isDark ? '#262626' : '#e5e5e5',
                strokeDashArray: 4,
            },
            tooltip: {
                style: { fontSize: '13px', fontFamily: "'Cairo', sans-serif" },
                y: { formatter: (val) => typeof val === 'number' ? val.toLocaleString('ar-EG') : val }
            },
            dataLabels: { style: { fontFamily: "'Cairo', sans-serif", fontWeight: 700 } },
            xaxis: {
                labels: { style: { fontFamily: "'Cairo', sans-serif", fontSize: '12px', colors: isDark ? '#737373' : '#737373' } }
            },
            yaxis: {
                labels: { style: { fontFamily: "'Cairo', sans-serif", fontSize: '12px', colors: isDark ? '#737373' : '#737373' } }
            },
            legend: {
                fontFamily: "'Cairo', sans-serif",
                fontSize: '13px',
                labels: { colors: isDark ? '#a3a3a3' : '#525252' }
            },
            noData: {
                text: 'لا توجد بيانات',
                style: { fontSize: '16px', fontFamily: "'Cairo', sans-serif", color: isDark ? '#525252' : '#a3a3a3' }
            }
        };
    }

    // ===== Filter Helpers =====
    function getFilterParams() {
        const bdId = document.getElementById('filterBusinessDay').value;
        const from = document.getElementById('filterFrom').value;
        const to = document.getElementById('filterTo').value;
        const params = new URLSearchParams();
        if (bdId) {
            params.set('businessDayId', bdId);
        } else {
            if (from) params.set('from', from);
            if (to) params.set('to', to);
        }
        return params;
    }

    async function fetchApi(endpoint) {
        const params = getFilterParams();
        const url = `/api/reports/${endpoint}${params.toString() ? '?' + params.toString() : ''}`;
        const resp = await fetch(url);
        if (!resp.ok) throw new Error(`API Error: ${resp.status}`);
        return resp.json();
    }

    // ===== Tab Switching =====
    function switchTab(tab) {
        // Update tab buttons
        document.querySelectorAll('.reports-tab').forEach(btn => btn.classList.remove('active'));
        document.querySelector(`.reports-tab[data-tab="${tab}"]`).classList.add('active');

        // Update panels
        document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
        document.getElementById(`panel-${tab}`).classList.add('active');

        currentTab = tab;

        // Load data if not loaded yet
        if (!loadedTabs.has(tab)) {
            loadTabData(tab);
        }
    }

    function loadTabData(tab) {
        loadedTabs.add(tab);
        switch (tab) {
            case 'overview': ReportsOverview.load(); break;
            case 'financial': ReportsFinancial.load(); break;
            case 'couriers': ReportsCouriers.load(); break;
            case 'organizations': ReportsOrgs.load(); break;
            case 'geographic': ReportsGeographic.load(); break;
            case 'speed': ReportsSpeed.load(); break;
            case 'returns': ReportsReturns.load(); break;
            case 'trips': ReportsTrips.load(); break;
        }
    }

    function applyFilters() {
        // Toggle date inputs enabled/disabled when BD selected
        const bdVal = document.getElementById('filterBusinessDay').value;
        document.getElementById('filterFrom').disabled = !!bdVal;
        document.getElementById('filterTo').disabled = !!bdVal;

        // Clear all loaded tabs and reload current
        destroyAllCharts();
        loadedTabs.clear();
        loadTabData(currentTab);
    }

    function setQuickDate(period) {
        document.getElementById('filterBusinessDay').value = '';
        document.getElementById('filterFrom').disabled = false;
        document.getElementById('filterTo').disabled = false;

        const today = new Date();
        let fromDate = new Date();

        if (period === 'today') {
            // fromDate is already today
        } else if (period === '7days') {
            fromDate.setDate(today.getDate() - 7);
        } else if (period === '30days') {
            fromDate.setDate(today.getDate() - 30);
        } else if (period === 'thisMonth') {
            fromDate = new Date(today.getFullYear(), today.getMonth(), 1);
        }

        // format to yyyy-mm-dd
        const formatDate = (date) => {
            const d = new Date(date);
            let month = '' + (d.getMonth() + 1);
            let day = '' + d.getDate();
            const year = d.getFullYear();

            if (month.length < 2) month = '0' + month;
            if (day.length < 2) day = '0' + day;

            return [year, month, day].join('-');
        };

        document.getElementById('filterFrom').value = formatDate(fromDate);
        document.getElementById('filterTo').value = formatDate(today);
        applyFilters();
    }

    // ===== Chart Instance Management =====
    function registerChart(id, chart) {
        if (chartInstances[id]) {
            chartInstances[id].destroy();
        }
        chartInstances[id] = chart;
    }

    function destroyAllCharts() {
        Object.keys(chartInstances).forEach(id => {
            if (chartInstances[id]) {
                chartInstances[id].destroy();
                delete chartInstances[id];
            }
        });
    }

    // ===== Export =====
    function exportCurrentTab() {
        const params = getFilterParams();
        let exportUrl = '';
        switch (currentTab) {
            case 'overview': exportUrl = '/api/reports/export/period'; break;
            case 'financial': exportUrl = '/api/reports/export/financial'; break;
            case 'returns': exportUrl = '/api/reports/export/returns'; break;
            case 'speed': exportUrl = '/api/reports/export/delivery-time'; break;
            case 'trips': exportUrl = '/api/reports/export/trips'; break;
            case 'couriers': exportUrl = '/api/reports/export/performance/couriers'; break;
            case 'organizations': exportUrl = '/api/reports/export/performance/organizations'; break;
            case 'geographic': exportUrl = '/api/reports/export/geographic'; break;
            default: exportUrl = '/api/reports/export/period'; break;
        }
        window.open(`${exportUrl}?${params.toString()}`, '_blank');
    }

    // ===== Utility Functions =====
    function formatNumber(n) {
        if (n == null) return '0';
        return Number(n).toLocaleString('ar-EG');
    }

    function formatMoney(n) {
        if (n == null) return '0.00';
        return Number(n).toLocaleString('ar-EG', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function formatPercent(n) {
        if (n == null) return '0%';
        return Number(n).toFixed(1) + '%';
    }

    function buildKpiCard(icon, value, label, change, isAccent) {
        const changeHtml = change != null ? `
            <div class="kpi-change ${change > 0 ? 'positive' : change < 0 ? 'negative' : 'neutral'}">
                ${change > 0 ? '↑' : change < 0 ? '↓' : '–'} ${Math.abs(change).toFixed(1)}%
            </div>` : '';
        return `
            <div class="kpi-card">
                <div class="kpi-icon ${isAccent ? 'accent' : ''}"><i data-lucide="${icon}"></i></div>
                <div class="kpi-content">
                    <div class="kpi-value">${value}</div>
                    <div class="kpi-label">${label}</div>
                    ${changeHtml}
                </div>
            </div>`;
    }

    function showEmpty(containerId) {
        document.getElementById(containerId).innerHTML = `
            <div class="empty-state">
                <i data-lucide="inbox"></i>
                <p>لا توجد بيانات في هذه الفترة</p>
            </div>`;
        if (typeof lucide !== 'undefined') lucide.createIcons();
    }

    function showSkeleton(containerId, count, type) {
        const cls = type === 'chart' ? 'skeleton-chart' : 'skeleton-card';
        let html = '';
        for (let i = 0; i < count; i++) html += `<div class="skeleton ${cls}"></div>`;
        document.getElementById(containerId).innerHTML = html;
    }

    // ===== Init =====
    function init() {
        // Business day toggle
        document.getElementById('filterBusinessDay').addEventListener('change', function() {
            const hasVal = !!this.value;
            document.getElementById('filterFrom').disabled = hasVal;
            document.getElementById('filterTo').disabled = hasVal;
        });

        // Load overview tab
        loadTabData('overview');
    }

    return {
        init,
        switchTab,
        applyFilters,
        setQuickDate,
        exportCurrentTab,
        fetchApi,
        getBaseChartOptions,
        getChartColors,
        registerChart,
        formatNumber,
        formatMoney,
        formatPercent,
        buildKpiCard,
        showEmpty,
        showSkeleton
    };
})();
