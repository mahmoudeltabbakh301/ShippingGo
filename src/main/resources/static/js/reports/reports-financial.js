/**
 * Reports Financial Tab (Tab 2)
 * Financial summary, commissions breakdown, entity details
 */
const ReportsFinancial = (() => {
    'use strict';

    async function load() {
        try {
            const data = await ReportsMain.fetchApi('financial');
            renderKpis(data);
            renderCommissionsChart(data);
            renderRevenueVsExpenseChart(data);
            renderOutgoingTable(data);
            renderIncomingTable(data);
            renderCourierTable(data);
        } catch (e) {
            console.error('Financial load error:', e);
        }
    }

    function renderKpis(d) {
        const el = document.getElementById('financialKpis');
        el.innerHTML = [
            ReportsMain.buildKpiCard('trending-up', ReportsMain.formatMoney(d.totalCommissionRevenue), 'إجمالي إيرادات العمولات', null, true),
            ReportsMain.buildKpiCard('users', ReportsMain.formatMoney(d.courierCommissions), 'عمولات المناديب (مصروف)', null),
            ReportsMain.buildKpiCard('wallet', ReportsMain.formatMoney(d.netProfit), 'صافي الربح', null, true),
            ReportsMain.buildKpiCard('package', ReportsMain.formatNumber(d.totalOrders), 'إجمالي الطلبات', null)
        ].join('');
        if (typeof lucide !== 'undefined') lucide.createIcons();
    }

    function renderCommissionsChart(d) {
        const categories = ['صادر', 'وارد', 'مناديب', 'غير مسندة'];
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'bar', height: 320, stacked: true },
            series: [
                { name: 'عمولة توصيل', data: [num(d.outgoingDeliveryComm), num(d.incomingDeliveryComm), num(d.courierDeliveryComm), num(d.unassignedDeliveryComm)] },
                { name: 'عمولة رفض', data: [num(d.outgoingRejectionComm), num(d.incomingRejectionComm), num(d.courierRejectionComm), num(d.unassignedRejectionComm)] },
                { name: 'عمولة إلغاء', data: [num(d.outgoingCancellationComm), num(d.incomingCancellationComm), num(d.courierCancellationComm), num(d.unassignedCancellationComm)] }
            ],
            xaxis: { ...ReportsMain.getBaseChartOptions().xaxis, categories },
            colors: ['#171717', '#737373', '#d4d4d4'],
            plotOptions: { bar: { horizontal: true, borderRadius: 4, barHeight: '60%' } },
            dataLabels: { enabled: false },
            tooltip: { ...ReportsMain.getBaseChartOptions().tooltip, y: { formatter: v => ReportsMain.formatMoney(v) } },
            legend: { ...ReportsMain.getBaseChartOptions().legend, position: 'top' }
        };
        const chart = new ApexCharts(document.getElementById('chartCommissions'), opts);
        chart.render();
        ReportsMain.registerChart('chartCommissions', chart);
    }

    function renderRevenueVsExpenseChart(d) {
        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'bar', height: 280 },
            series: [
                { name: 'إيرادات العمولات', data: [num(d.outgoingOrgCommissions), num(d.incomingOrgCommissions), num(d.unassignedCommissions)] },
                { name: 'مصروف المناديب', data: [num(d.courierCommissions), 0, 0] }
            ],
            xaxis: { ...ReportsMain.getBaseChartOptions().xaxis, categories: ['صادر', 'وارد', 'غير مسندة'] },
            colors: ['#171717', '#a3a3a3'],
            plotOptions: { bar: { borderRadius: 6, columnWidth: '55%', dataLabels: { position: 'top' } } },
            dataLabels: { enabled: true, formatter: v => v > 0 ? ReportsMain.formatMoney(v) : '', offsetY: -20, style: { fontSize: '11px', fontFamily: "'Cairo',sans-serif", fontWeight: 700, colors: ['#525252'] } },
            tooltip: { ...ReportsMain.getBaseChartOptions().tooltip, y: { formatter: v => ReportsMain.formatMoney(v) } }
        };
        const chart = new ApexCharts(document.getElementById('chartRevenueVsExpense'), opts);
        chart.render();
        ReportsMain.registerChart('chartRevenueVsExpense', chart);
    }

    function renderOutgoingTable(d) {
        const tbody = document.querySelector('#tableOutgoingOrgs tbody');
        if (!d.outgoingOrgDetails || d.outgoingOrgDetails.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted);padding:var(--space-6)">لا توجد بيانات</td></tr>';
            return;
        }
        tbody.innerHTML = d.outgoingOrgDetails.map(e => `<tr>
            <td class="td-name">${e.entityName || '-'}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.totalOrders)}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.deliveredOrders)}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.refusedOrders)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.deliveredAmount)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.totalCommission)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.netAmount)}</td>
        </tr>`).join('');
    }

    function renderIncomingTable(d) {
        const tbody = document.querySelector('#tableIncomingOrgs tbody');
        if (!d.incomingOrgDetails || d.incomingOrgDetails.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;color:var(--text-muted);padding:var(--space-6)">لا توجد بيانات</td></tr>';
            return;
        }
        tbody.innerHTML = d.incomingOrgDetails.map(e => `<tr>
            <td class="td-name">${e.entityName || '-'}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.totalOrders)}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.deliveredOrders)}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.refusedOrders)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.deliveredAmount)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.totalCommission)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.netAmount)}</td>
        </tr>`).join('');
    }

    function renderCourierTable(d) {
        const tbody = document.querySelector('#tableCourierFinancial tbody');
        if (!d.courierDetails || d.courierDetails.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;color:var(--text-muted);padding:var(--space-6)">لا توجد بيانات</td></tr>';
            return;
        }
        tbody.innerHTML = d.courierDetails.map(e => `<tr>
            <td class="td-name">${e.entityName || '-'}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.totalOrders)}</td>
            <td class="td-value">${ReportsMain.formatNumber(e.deliveredOrders)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.deliveryCommission)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.rejectionCommission)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.cancellationCommission)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.totalCommission)}</td>
            <td class="td-value">${ReportsMain.formatMoney(e.netAmount)}</td>
        </tr>`).join('');
    }

    function num(v) { return v != null ? Number(v) : 0; }

    return { load };
})();
