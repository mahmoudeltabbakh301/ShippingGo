/**
 * Reports Overview Tab (Tab 1)
 * Period stats, trends, comparison
 */
const ReportsOverview = (() => {
    'use strict';

    async function load() {
        try {
            const [period, trends, comparison] = await Promise.all([
                ReportsMain.fetchApi('period'),
                ReportsMain.fetchApi('trends'),
                ReportsMain.fetchApi('comparison')
            ]);
            renderKpis(period, comparison);
            renderComparison(comparison);
            renderStatusDonut(period);
            renderTrendsChart(trends);
        } catch (e) {
            console.error('Overview load error:', e);
        }
    }

    function renderKpis(p, c) {
        const el = document.getElementById('overviewKpis');
        el.innerHTML = [
            ReportsMain.buildKpiCard('package', ReportsMain.formatNumber(p.totalOrders), 'إجمالي الطلبات', c?.ordersGrowth, true),
            ReportsMain.buildKpiCard('check-circle', ReportsMain.formatPercent(p.deliveryRate), 'نسبة التوصيل', c?.deliveryRateChange),
            ReportsMain.buildKpiCard('banknote', ReportsMain.formatMoney(p.totalCollected), 'إجمالي المحصّل', c?.revenueGrowth),
            ReportsMain.buildKpiCard('calculator', ReportsMain.formatMoney(p.avgOrderValue), 'متوسط قيمة الطلب', c?.avgOrderValueChange)
        ].join('');
        if (typeof lucide !== 'undefined') lucide.createIcons();
    }

    function renderComparison(c) {
        if (!c) return;
        const el = document.getElementById('overviewComparison');
        const trendBadge = c.overallTrend === 'IMPROVING' ? 'improving' :
                           c.overallTrend === 'DECLINING' ? 'declining' : 'stable';
        const trendLabel = c.overallTrend === 'IMPROVING' ? '📈 تحسن' :
                           c.overallTrend === 'DECLINING' ? '📉 تراجع' : '➡️ مستقر';

        function compCard(value, label) {
            const cls = value > 0 ? 'positive' : value < 0 ? 'negative' : 'neutral';
            const arrow = value > 0 ? '↑' : value < 0 ? '↓' : '–';
            return `<div class="comparison-card">
                <div class="comp-value kpi-change ${cls}" style="display:inline-block">${arrow} ${Math.abs(value).toFixed(1)}%</div>
                <div class="comp-label">${label}</div>
            </div>`;
        }

        el.innerHTML = compCard(c.ordersGrowth, 'نمو الطلبات') +
            compCard(c.deliveryRateChange, 'تغيير نسبة التوصيل') +
            compCard(c.revenueGrowth, 'نمو الإيرادات') +
            compCard(c.refusalRateChange * -1, 'تحسن نسبة الرفض') +
            `<div class="comparison-card" style="display:flex;flex-direction:column;align-items:center;justify-content:center">
                <span class="rpt-badge ${trendBadge}">${trendLabel}</span>
                <div class="comp-label" style="margin-top:var(--space-2)">الاتجاه العام</div>
            </div>`;
    }

    function renderStatusDonut(p) {
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'donut', height: 320 },
            series: [p.delivered, p.refused, p.cancelled, p.deferred, p.partial, p.inTransit],
            labels: ['مسلمة', 'مرفوضة', 'ملغاة', 'مؤجلة', 'جزئي', 'قيد التوصيل'],
            colors: ['#171717', '#525252', '#737373', '#a3a3a3', '#404040', '#d4d4d4'],
            plotOptions: {
                pie: {
                    donut: {
                        size: '72%',
                        labels: {
                            show: true,
                            name: { show: true, fontSize: '14px', fontFamily: "'Cairo', sans-serif", fontWeight: 700 },
                            value: { show: true, fontSize: '28px', fontFamily: "'Cairo', sans-serif", fontWeight: 800, formatter: v => Number(v).toLocaleString('ar-EG') },
                            total: {
                                show: true,
                                label: 'إجمالي',
                                fontSize: '13px',
                                fontFamily: "'Cairo', sans-serif",
                                fontWeight: 600,
                                formatter: w => w.globals.seriesTotals.reduce((a, b) => a + b, 0).toLocaleString('ar-EG')
                            }
                        }
                    }
                }
            },
            legend: { ...ReportsMain.getBaseChartOptions().legend, position: 'bottom' },
            stroke: { show: true, width: 3, colors: [document.documentElement.getAttribute('data-theme') === 'dark' ? '#171717' : '#ffffff'] },
            dataLabels: { enabled: false }
        };

        const chart = new ApexCharts(document.getElementById('chartStatusDonut'), opts);
        chart.render();
        ReportsMain.registerChart('chartStatusDonut', chart);
    }

    function renderTrendsChart(t) {
        if (!t || !t.labels || t.labels.length === 0) {
            ReportsMain.showEmpty('chartTrends');
            return;
        }

        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'area', height: 320 },
            series: [
                { name: 'عدد الطلبات', type: 'area', data: t.orderCounts },
                { name: 'نسبة التوصيل', type: 'line', data: t.deliveryRates }
            ],
            xaxis: { ...ReportsMain.getBaseChartOptions().xaxis, categories: t.labels },
            yaxis: [
                { title: { text: 'عدد الطلبات', style: { fontFamily: "'Cairo', sans-serif" } }, labels: { formatter: v => Math.round(v).toLocaleString('ar-EG') } },
                { opposite: true, title: { text: 'نسبة التوصيل %', style: { fontFamily: "'Cairo', sans-serif" } }, labels: { formatter: v => v.toFixed(0) + '%' }, max: 100 }
            ],
            colors: ['#171717', '#a3a3a3'],
            fill: {
                type: ['gradient', 'solid'],
                gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05, stops: [0, 100] }
            },
            stroke: { width: [2, 3], dashArray: [0, 5], curve: 'smooth' },
            markers: { size: [0, 4], strokeWidth: 2 },
            dataLabels: { enabled: false }
        };

        const chart = new ApexCharts(document.getElementById('chartTrends'), opts);
        chart.render();
        ReportsMain.registerChart('chartTrends', chart);
    }

    return { load };
})();
