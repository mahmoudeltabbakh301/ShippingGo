/**
 * Reports Organizations Tab (Tab 4)
 * Organization performance comparison
 */
const ReportsOrgs = (() => {
    'use strict';

    async function load() {
        try {
            const data = await ReportsMain.fetchApi('performance/organizations');
            renderHighlights(data);
            renderComparisonChart(data);
            renderTable(data);
        } catch (e) {
            console.error('Orgs load error:', e);
        }
    }

    function renderHighlights(d) {
        const el = document.getElementById('orgHighlights');
        const top = d.topPerformer;
        const worst = d.worstPerformer;
        const count = d.entries ? d.entries.length : 0;
        el.innerHTML = `
            <div class="highlight-card top">
                <div class="highlight-emoji">🏆</div>
                <div class="highlight-label">أفضل منظمة</div>
                <div class="highlight-name">${top ? top.entityName : '-'}</div>
                <div class="highlight-value">${top ? ReportsMain.formatPercent(top.deliveryRate) : '-'}</div>
            </div>
            <div class="highlight-card">
                <div class="highlight-emoji">🏢</div>
                <div class="highlight-label">عدد المنظمات النشطة</div>
                <div class="highlight-name">&nbsp;</div>
                <div class="highlight-value">${count}</div>
            </div>
            <div class="highlight-card">
                <div class="highlight-emoji">📊</div>
                <div class="highlight-label">المتوسط العام</div>
                <div class="highlight-name">نسبة التوصيل</div>
                <div class="highlight-value">${ReportsMain.formatPercent(d.averageDeliveryRate)}</div>
            </div>`;
    }

    function renderComparisonChart(d) {
        if (!d.entries || d.entries.length === 0) {
            ReportsMain.showEmpty('chartOrgComparison');
            return;
        }
        const entries = d.entries.slice(0, 12);
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'bar', height: 360 },
            series: [
                { name: 'مسلمة', data: entries.map(e => e.deliveredOrders) },
                { name: 'مرفوضة', data: entries.map(e => e.refusedOrders) },
            ],
            xaxis: { ...ReportsMain.getBaseChartOptions().xaxis, categories: entries.map(e => e.entityName) },
            colors: ['#171717', '#a3a3a3'],
            plotOptions: { bar: { borderRadius: 6, columnWidth: '60%' } },
            dataLabels: { enabled: false },
            legend: { ...ReportsMain.getBaseChartOptions().legend, position: 'top' },
            tooltip: { ...ReportsMain.getBaseChartOptions().tooltip }
        };
        const chart = new ApexCharts(document.getElementById('chartOrgComparison'), opts);
        chart.render();
        ReportsMain.registerChart('chartOrgComparison', chart);
    }

    function renderTable(d) {
        const tbody = document.querySelector('#tableOrgPerf tbody');
        if (!d.entries || d.entries.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--text-muted);padding:var(--space-6)">لا توجد بيانات</td></tr>';
            return;
        }
        tbody.innerHTML = d.entries.map(e => {
            const rankCls = e.rank <= 3 ? `rank-${e.rank}` : '';
            return `<tr>
                <td><span class="rank-num ${rankCls}">${e.rank}</span></td>
                <td class="td-name">${e.entityName}</td>
                <td class="td-value">${ReportsMain.formatNumber(e.totalOrders)}</td>
                <td class="td-value">${ReportsMain.formatNumber(e.deliveredOrders)}</td>
                <td class="td-value">${ReportsMain.formatNumber(e.refusedOrders)}</td>
                <td>
                    <div class="progress-inline">
                        <div class="progress-bar-mini"><div class="progress-bar-mini-fill" style="width:${Math.min(e.deliveryRate, 100)}%"></div></div>
                        <span class="progress-label">${e.deliveryRate.toFixed(1)}%</span>
                    </div>
                </td>
                <td class="td-value">${ReportsMain.formatMoney(e.totalCollected)}</td>
                <td class="td-value">${ReportsMain.formatMoney(e.netAmount)}</td>
            </tr>`;
        }).join('');
    }

    return { load };
})();
