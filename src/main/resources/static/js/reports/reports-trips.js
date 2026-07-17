/**
 * Reports Trips Tab (Tab 8)
 * Trip statistics, vehicle performance, trends
 */
const ReportsTrips = (() => {
    'use strict';

    async function load() {
        try {
            const data = await ReportsMain.fetchApi('trips');
            renderKpis(data);
            renderStatusChart(data);
            renderVehicleChart(data);
            renderTrendChart(data);
            renderDestTable(data);
            renderOriginTable(data);
        } catch (e) {
            console.error('Trips load error:', e);
        }
    }

    function renderKpis(d) {
        const el = document.getElementById('tripsKpis');
        el.innerHTML = [
            ReportsMain.buildKpiCard('truck', ReportsMain.formatNumber(d.totalTrips), 'إجمالي الرحلات', null, true),
            ReportsMain.buildKpiCard('package', ReportsMain.formatNumber(d.totalOrdersShipped), 'الأوردرات المنقولة', null),
            ReportsMain.buildKpiCard('calculator', d.avgOrdersPerTrip ? d.avgOrdersPerTrip.toFixed(1) : '0', 'متوسط أوردرات/رحلة', null)
        ].join('');
        if (typeof lucide !== 'undefined') lucide.createIcons();
    }

    function renderStatusChart(d) {
        const statuses = [
            { label: 'تجهيز', value: d.preparingTrips || 0 },
            { label: 'في الطريق', value: d.inTransitTrips || 0 },
            { label: 'وصلت', value: d.arrivedTrips || 0 },
            { label: 'مكتملة', value: d.completedTrips || 0 },
            { label: 'في طريق العودة', value: d.returningTrips || 0 },
            { label: 'عادت', value: d.returnedTrips || 0 },
            { label: 'ملغاة', value: d.cancelledTrips || 0 }
        ].filter(s => s.value > 0);

        if (statuses.length === 0) {
            ReportsMain.showEmpty('chartTripStatus');
            return;
        }

        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'donut', height: 320 },
            series: statuses.map(s => s.value),
            labels: statuses.map(s => s.label),
            colors: ['#0a0a0a', '#262626', '#404040', '#525252', '#737373', '#a3a3a3', '#d4d4d4'],
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
        const chart = new ApexCharts(document.getElementById('chartTripStatus'), opts);
        chart.render();
        ReportsMain.registerChart('chartTripStatus', chart);
    }

    function renderVehicleChart(d) {
        if (!d.byVehicle || d.byVehicle.length === 0) {
            ReportsMain.showEmpty('chartVehiclePerf');
            return;
        }
        const vehicles = d.byVehicle.slice(0, 10);
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'bar', height: 300 },
            series: [
                { name: 'الرحلات', data: vehicles.map(v => v.totalTrips) },
                { name: 'الأوردرات', data: vehicles.map(v => v.totalOrdersShipped) },
                { name: 'المكتملة', data: vehicles.map(v => v.completedTrips) }
            ],
            xaxis: {
                ...ReportsMain.getBaseChartOptions().xaxis,
                categories: vehicles.map(v => v.vehiclePlateNumber || 'غير محدد')
            },
            colors: ['#171717', '#737373', '#d4d4d4'],
            plotOptions: { bar: { borderRadius: 4, columnWidth: '60%' } },
            dataLabels: { enabled: false },
            legend: { ...ReportsMain.getBaseChartOptions().legend, position: 'top' }
        };
        const chart = new ApexCharts(document.getElementById('chartVehiclePerf'), opts);
        chart.render();
        ReportsMain.registerChart('chartVehiclePerf', chart);
    }

    function renderTrendChart(d) {
        if (!d.trendLabels || d.trendLabels.length === 0) {
            ReportsMain.showEmpty('chartTripTrend');
            return;
        }
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'area', height: 280 },
            series: [{ name: 'عدد الرحلات', data: d.trendCounts }],
            xaxis: { ...ReportsMain.getBaseChartOptions().xaxis, categories: d.trendLabels },
            colors: ['#171717'],
            fill: {
                type: 'gradient',
                gradient: { shadeIntensity: 1, opacityFrom: 0.4, opacityTo: 0.05, stops: [0, 100] }
            },
            stroke: { width: 2, curve: 'smooth' },
            dataLabels: { enabled: false },
            markers: { size: 3, strokeWidth: 2 }
        };
        const chart = new ApexCharts(document.getElementById('chartTripTrend'), opts);
        chart.render();
        ReportsMain.registerChart('chartTripTrend', chart);
    }

    function renderDestTable(d) {
        const tbody = document.querySelector('#tableTripDest tbody');
        if (!d.byDestination || d.byDestination.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:var(--text-muted);padding:var(--space-6)">لا توجد بيانات</td></tr>';
            return;
        }
        tbody.innerHTML = d.byDestination.map(e => `<tr>
            <td class="td-name">${e.orgName || '-'}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.totalTrips)}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.totalOrders)}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.completedTrips)}</td>
        </tr>`).join('');
    }

    function renderOriginTable(d) {
        const tbody = document.querySelector('#tableTripOrigin tbody');
        if (!d.byOrigin || d.byOrigin.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:var(--text-muted);padding:var(--space-6)">لا توجد بيانات</td></tr>';
            return;
        }
        tbody.innerHTML = d.byOrigin.map(e => `<tr>
            <td class="td-name">${e.orgName || '-'}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.totalTrips)}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.totalOrders)}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.completedTrips)}</td>
        </tr>`).join('');
    }

    return { load };
})();
