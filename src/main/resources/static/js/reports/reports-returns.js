/**
 * Reports Returns Tab (Tab 7)
 * Returns analysis: reasons, by courier, by governorate
 */
const ReportsReturns = (() => {
    'use strict';

    async function load() {
        try {
            const data = await ReportsMain.fetchApi('returns');
            renderKpis(data);
            renderReasonsChart(data);
            renderByCourierChart(data);
            renderTreemap(data);
        } catch (e) {
            console.error('Returns load error:', e);
        }
    }

    function renderKpis(d) {
        const el = document.getElementById('returnsKpis');
        el.innerHTML = [
            ReportsMain.buildKpiCard('undo-2', ReportsMain.formatNumber(d.totalReturns), 'إجمالي المرتجعات', null, true),
            ReportsMain.buildKpiCard('percent', ReportsMain.formatPercent(d.returnRate), 'نسبة المرتجعات', null),
            ReportsMain.buildKpiCard('x-circle', ReportsMain.formatNumber(d.refusedCount), 'مرفوضة', null),
            ReportsMain.buildKpiCard('clock', ReportsMain.formatNumber(d.deferredCount), 'مؤجلة', null)
        ].join('');
        if (typeof lucide !== 'undefined') lucide.createIcons();
    }

    function renderReasonsChart(d) {
        if (!d.topReasons || d.topReasons.length === 0) {
            ReportsMain.showEmpty('chartReturnReasons');
            return;
        }
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'pie', height: 340 },
            series: d.topReasons.map(r => r.count),
            labels: d.topReasons.map(r => r.reasonLabel),
            colors: ['#0a0a0a', '#262626', '#404040', '#525252', '#737373', '#a3a3a3', '#d4d4d4', '#e5e5e5'],
            legend: { ...ReportsMain.getBaseChartOptions().legend, position: 'bottom', fontSize: '12px' },
            stroke: { show: true, width: 2, colors: [document.documentElement.getAttribute('data-theme') === 'dark' ? '#171717' : '#ffffff'] },
            dataLabels: {
                enabled: true,
                formatter: (val) => val.toFixed(0) + '%',
                style: { fontSize: '12px', fontFamily: "'Cairo',sans-serif", fontWeight: 700 }
            },
            tooltip: {
                ...ReportsMain.getBaseChartOptions().tooltip,
                y: { formatter: (val, { seriesIndex }) => {
                    const reason = d.topReasons[seriesIndex];
                    return `${val.toLocaleString('ar-EG')} (${reason.percentage}%)`;
                }}
            }
        };
        const chart = new ApexCharts(document.getElementById('chartReturnReasons'), opts);
        chart.render();
        ReportsMain.registerChart('chartReturnReasons', chart);
    }

    function renderByCourierChart(d) {
        if (!d.byCourier || d.byCourier.length === 0) {
            ReportsMain.showEmpty('chartReturnsByCourier');
            return;
        }
        const entries = d.byCourier.slice(0, 10);
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'bar', height: Math.max(280, entries.length * 40) },
            series: [
                { name: 'إجمالي', data: entries.map(e => e.totalOrders) },
                { name: 'مرتجع', data: entries.map(e => e.returnedOrders) }
            ],
            xaxis: { ...ReportsMain.getBaseChartOptions().xaxis, categories: entries.map(e => e.courierName) },
            colors: ['#d4d4d4', '#171717'],
            plotOptions: { bar: { horizontal: true, borderRadius: 4, barHeight: '55%' } },
            dataLabels: { enabled: false },
            tooltip: {
                ...ReportsMain.getBaseChartOptions().tooltip,
                shared: true,
                custom: function({ dataPointIndex }) {
                    const e = entries[dataPointIndex];
                    return `<div style="padding:8px 12px;font-family:'Cairo',sans-serif">
                        <b>${e.courierName}</b><br>
                        إجمالي: ${e.totalOrders.toLocaleString('ar-EG')}<br>
                        مرتجع: ${e.returnedOrders.toLocaleString('ar-EG')}<br>
                        النسبة: ${e.returnRate.toFixed(1)}%
                    </div>`;
                }
            },
            legend: { ...ReportsMain.getBaseChartOptions().legend, position: 'top' }
        };
        const chart = new ApexCharts(document.getElementById('chartReturnsByCourier'), opts);
        chart.render();
        ReportsMain.registerChart('chartReturnsByCourier', chart);
    }

    function renderTreemap(d) {
        const GOV_NAMES = {
            CAIRO: 'القاهرة', GIZA: 'الجيزة', ALEXANDRIA: 'الإسكندرية', DAKHALIA: 'الدقهلية',
            RED_SEA: 'البحر الأحمر', BEHEIRA: 'البحيرة', FAYOUM: 'الفيوم', GHARBIA: 'الغربية',
            ISMAILIA: 'الإسماعيلية', MENOFIA: 'المنوفية', MINYA: 'المنيا', QALUBIA: 'القليوبية',
            NEW_VALLEY: 'الوادي الجديد', SUEZ: 'السويس', ASWAN: 'أسوان', ASSIUT: 'أسيوط',
            BENI_SUEF: 'بني سويف', PORT_SAID: 'بورسعيد', DAMIETTA: 'دمياط', SHARQIA: 'الشرقية',
            SOUTH_SINAI: 'جنوب سيناء', KAFR_EL_SHEIKH: 'كفر الشيخ', MATROUH: 'مطروح',
            LUXOR: 'الأقصر', QENA: 'قنا', NORTH_SINAI: 'شمال سيناء', SOHAG: 'سوهاج'
        };

        if (!d.byGovernorate || Object.keys(d.byGovernorate).length === 0) {
            ReportsMain.showEmpty('chartReturnsTreemap');
            return;
        }

        const treemapData = Object.entries(d.byGovernorate)
            .map(([key, val]) => ({
                x: GOV_NAMES[key] || key,
                y: val.returnedOrders || 0
            }))
            .filter(item => item.y > 0)
            .sort((a, b) => b.y - a.y);

        if (treemapData.length === 0) {
            ReportsMain.showEmpty('chartReturnsTreemap');
            return;
        }

        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'treemap', height: 350 },
            series: [{ data: treemapData }],
            colors: ['#171717'],
            plotOptions: {
                treemap: {
                    distributed: false,
                    enableShades: true,
                    shadeIntensity: 0.5,
                    colorScale: {
                        ranges: [
                            { from: 0, to: 5, color: '#d4d4d4' },
                            { from: 6, to: 20, color: '#a3a3a3' },
                            { from: 21, to: 50, color: '#737373' },
                            { from: 51, to: 100, color: '#404040' },
                            { from: 101, to: 10000, color: '#171717' },
                        ]
                    }
                }
            },
            dataLabels: {
                enabled: true,
                style: { fontSize: '13px', fontFamily: "'Cairo',sans-serif", fontWeight: 700 },
                formatter: (text, { value }) => [text, value.toLocaleString('ar-EG')]
            },
            tooltip: {
                ...ReportsMain.getBaseChartOptions().tooltip,
                y: { formatter: v => v.toLocaleString('ar-EG') + ' مرتجع' }
            }
        };
        const chart = new ApexCharts(document.getElementById('chartReturnsTreemap'), opts);
        chart.render();
        ReportsMain.registerChart('chartReturnsTreemap', chart);
    }

    return { load };
})();
