/**
 * Reports Speed Tab (Tab 6)
 * Delivery time analysis
 */
const ReportsSpeed = (() => {
    'use strict';

    async function load() {
        try {
            const data = await ReportsMain.fetchApi('delivery-time');
            renderKpis(data);
            renderTimeDistribution(data);
            renderCourierSpeed(data);
            renderGovSpeed(data);
        } catch (e) {
            console.error('Speed load error:', e);
        }
    }

    function renderKpis(d) {
        const el = document.getElementById('speedKpis');
        el.innerHTML = [
            ReportsMain.buildKpiCard('clock', d.avgDeliveryTimeHours ? d.avgDeliveryTimeHours.toFixed(1) + ' ساعة' : '-', 'متوسط وقت التوصيل', null, true),
            ReportsMain.buildKpiCard('zap', d.fastestDeliveryHours ? d.fastestDeliveryHours.toFixed(1) + ' ساعة' : '-', 'أسرع توصيل', null),
            ReportsMain.buildKpiCard('hourglass', d.slowestDeliveryHours ? d.slowestDeliveryHours.toFixed(1) + ' ساعة' : '-', 'أبطأ توصيل', null)
        ].join('');
        if (typeof lucide !== 'undefined') lucide.createIcons();
    }

    function renderTimeDistribution(d) {
        const total = (d.within2Hours || 0) + (d.within4Hours || 0) + (d.within8Hours || 0) + (d.within24Hours || 0) + (d.moreThan24Hours || 0);
        if (total === 0) {
            ReportsMain.showEmpty('chartTimeDistribution');
            return;
        }
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'donut', height: 320 },
            series: [d.within2Hours || 0, d.within4Hours || 0, d.within8Hours || 0, d.within24Hours || 0, d.moreThan24Hours || 0],
            labels: ['خلال 2 ساعة', 'خلال 4 ساعات', 'خلال 8 ساعات', 'خلال 24 ساعة', 'أكثر من 24 ساعة'],
            colors: ['#0a0a0a', '#404040', '#737373', '#a3a3a3', '#d4d4d4'],
            plotOptions: {
                pie: {
                    donut: {
                        size: '70%',
                        labels: {
                            show: true,
                            total: {
                                show: true, label: 'إجمالي',
                                fontSize: '13px', fontFamily: "'Cairo',sans-serif",
                                formatter: w => w.globals.seriesTotals.reduce((a, b) => a + b, 0).toLocaleString('ar-EG')
                            },
                            value: { fontSize: '24px', fontFamily: "'Cairo',sans-serif", fontWeight: 800 }
                        }
                    }
                }
            },
            legend: { ...ReportsMain.getBaseChartOptions().legend, position: 'bottom' },
            stroke: { show: true, width: 3, colors: [document.documentElement.getAttribute('data-theme') === 'dark' ? '#171717' : '#ffffff'] },
            dataLabels: { enabled: false }
        };
        const chart = new ApexCharts(document.getElementById('chartTimeDistribution'), opts);
        chart.render();
        ReportsMain.registerChart('chartTimeDistribution', chart);
    }

    function renderCourierSpeed(d) {
        if (!d.courierTimes || d.courierTimes.length === 0) {
            ReportsMain.showEmpty('chartCourierSpeed');
            return;
        }
        const entries = d.courierTimes.slice(0, 10);
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'bar', height: Math.max(280, entries.length * 38) },
            series: [{ name: 'متوسط (ساعات)', data: entries.map(e => e.avgHours) }],
            xaxis: { ...ReportsMain.getBaseChartOptions().xaxis, categories: entries.map(e => e.courierName) },
            colors: ['#171717'],
            plotOptions: { bar: { horizontal: true, borderRadius: 4, barHeight: '60%' } },
            dataLabels: {
                enabled: true, formatter: v => v.toFixed(1) + 'h',
                style: { fontSize: '11px', fontFamily: "'Cairo',sans-serif", fontWeight: 700 }
            },
            tooltip: { ...ReportsMain.getBaseChartOptions().tooltip, y: { formatter: v => v.toFixed(1) + ' ساعة' } }
        };
        const chart = new ApexCharts(document.getElementById('chartCourierSpeed'), opts);
        chart.render();
        ReportsMain.registerChart('chartCourierSpeed', chart);
    }

    function renderGovSpeed(d) {
        if (!d.governorateTimes || d.governorateTimes.length === 0) {
            ReportsMain.showEmpty('chartGovSpeed');
            return;
        }
        const entries = d.governorateTimes.slice(0, 15);
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'bar', height: Math.max(280, entries.length * 36) },
            series: [
                { name: 'متوسط الوقت (ساعات)', data: entries.map(e => e.avgHours) },
            ],
            xaxis: { ...ReportsMain.getBaseChartOptions().xaxis, categories: entries.map(e => e.governorateName) },
            colors: ['#525252'],
            plotOptions: { bar: { horizontal: true, borderRadius: 4, barHeight: '55%' } },
            dataLabels: {
                enabled: true, formatter: v => v.toFixed(1) + 'h',
                style: { fontSize: '11px', fontFamily: "'Cairo',sans-serif", fontWeight: 700 }
            },
            tooltip: {
                ...ReportsMain.getBaseChartOptions().tooltip,
                y: { formatter: v => v.toFixed(1) + ' ساعة' },
                custom: function({ series, seriesIndex, dataPointIndex }) {
                    const e = entries[dataPointIndex];
                    return `<div style="padding:8px 12px;font-family:'Cairo',sans-serif">
                        <b>${e.governorateName}</b><br>
                        متوسط: ${e.avgHours.toFixed(1)} ساعة<br>
                        التوصيلات: ${e.totalDelivered.toLocaleString('ar-EG')}
                    </div>`;
                }
            }
        };
        const chart = new ApexCharts(document.getElementById('chartGovSpeed'), opts);
        chart.render();
        ReportsMain.registerChart('chartGovSpeed', chart);
    }

    return { load };
})();
