/**
 * Reports Couriers Tab (Tab 3)
 * Courier performance comparison, radar chart, ranked table
 */
const ReportsCouriers = (() => {
    'use strict';

    async function load() {
        try {
            const data = await ReportsMain.fetchApi('performance/couriers');
            renderHighlights(data);
            renderRankChart(data);
            renderRadarChart(data);
            renderTable(data);
        } catch (e) {
            console.error('Couriers load error:', e);
        }
    }

    function renderHighlights(d) {
        const el = document.getElementById('courierHighlights');
        const top = d.topPerformer;
        const worst = d.worstPerformer;
        el.innerHTML = `
            <div class="highlight-card top">
                <div class="highlight-emoji">⭐</div>
                <div class="highlight-label">أفضل مندوب</div>
                <div class="highlight-name">${top ? top.entityName : '-'}</div>
                <div class="highlight-value">${top ? ReportsMain.formatPercent(top.deliveryRate) : '-'}</div>
            </div>
            <div class="highlight-card">
                <div class="highlight-emoji">📊</div>
                <div class="highlight-label">المتوسط العام</div>
                <div class="highlight-name">نسبة التوصيل</div>
                <div class="highlight-value">${ReportsMain.formatPercent(d.averageDeliveryRate)}</div>
            </div>
            <div class="highlight-card">
                <div class="highlight-emoji">⚠️</div>
                <div class="highlight-label">أقل أداءً</div>
                <div class="highlight-name">${worst ? worst.entityName : '-'}</div>
                <div class="highlight-value">${worst ? ReportsMain.formatPercent(worst.deliveryRate) : '-'}</div>
            </div>`;
    }

    function renderRankChart(d) {
        if (!d.entries || d.entries.length === 0) {
            ReportsMain.showEmpty('chartCourierRank');
            return;
        }
        const entries = d.entries.slice(0, 15);
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'bar', height: Math.max(300, entries.length * 40) },
            series: [{ name: 'نسبة التوصيل', data: entries.map(e => e.deliveryRate) }],
            xaxis: { ...ReportsMain.getBaseChartOptions().xaxis, categories: entries.map(e => e.entityName) },
            colors: ['#171717'],
            plotOptions: {
                bar: {
                    horizontal: true, borderRadius: 6, barHeight: '65%',
                    dataLabels: { position: 'top' }
                }
            },
            dataLabels: {
                enabled: true, formatter: v => v.toFixed(1) + '%',
                offsetX: 30,
                style: { fontSize: '12px', fontFamily: "'Cairo',sans-serif", fontWeight: 700, colors: ['#525252'] }
            },
            tooltip: { ...ReportsMain.getBaseChartOptions().tooltip, y: { formatter: v => v.toFixed(1) + '%' } }
        };
        const chart = new ApexCharts(document.getElementById('chartCourierRank'), opts);
        chart.render();
        ReportsMain.registerChart('chartCourierRank', chart);
    }

    function renderRadarChart(d) {
        if (!d.entries || d.entries.length < 2) {
            ReportsMain.showEmpty('chartCourierRadar');
            return;
        }
        const top5 = d.entries.slice(0, 5);
        const maxOrders = Math.max(...d.entries.map(e => e.totalOrders), 1);
        const maxCollected = Math.max(...d.entries.map(e => Number(e.totalCollected || 0)), 1);

        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'radar', height: 350 },
            series: top5.map(e => ({
                name: e.entityName,
                data: [
                    e.deliveryRate,
                    (e.totalOrders / maxOrders * 100),
                    (Number(e.totalCollected || 0) / maxCollected * 100),
                    (100 - e.refusalRate),
                ]
            })),
            xaxis: { categories: ['نسبة التوصيل', 'حجم الطلبات', 'المحصّل', 'جودة (عكس الرفض)'] },
            colors: ['#0a0a0a', '#404040', '#737373', '#a3a3a3', '#d4d4d4'],
            yaxis: { show: false, max: 100 },
            stroke: { width: 2 },
            fill: { opacity: 0.15 },
            markers: { size: 3 },
            legend: { ...ReportsMain.getBaseChartOptions().legend, position: 'bottom' }
        };
        const chart = new ApexCharts(document.getElementById('chartCourierRadar'), opts);
        chart.render();
        ReportsMain.registerChart('chartCourierRadar', chart);
    }

    function renderTable(d) {
        const tbody = document.querySelector('#tableCourierPerf tbody');
        if (!d.entries || d.entries.length === 0) {
            tbody.innerHTML = '<tr><td colspan="10" style="text-align:center;color:var(--text-muted);padding:var(--space-6)">لا توجد بيانات</td></tr>';
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
                <td class="td-value">${ReportsMain.formatNumber(e.cancelledOrders)}</td>
                <td>
                    <div class="progress-inline">
                        <div class="progress-bar-mini"><div class="progress-bar-mini-fill" style="width:${Math.min(e.deliveryRate, 100)}%"></div></div>
                        <span class="progress-label">${e.deliveryRate.toFixed(1)}%</span>
                    </div>
                </td>
                <td class="td-value">${ReportsMain.formatMoney(e.totalCollected)}</td>
                <td class="td-value">${ReportsMain.formatMoney(e.totalCommission)}</td>
                <td class="td-value">${ReportsMain.formatMoney(e.netAmount)}</td>
            </tr>`;
        }).join('');
    }

    return { load };
})();
