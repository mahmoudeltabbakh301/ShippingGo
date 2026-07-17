/**
 * Reports Geographic Tab (Tab 5)
 * Interactive Egypt SVG map + governorate bar chart + table
 */
const ReportsGeographic = (() => {
    'use strict';

    let geoData = null;
    let mapMode = 'orders'; // orders | rate | revenue

    // Egypt governorate SVG path data (simplified polygons)
    const GOV_PATHS = {
        CAIRO: { d: 'M382,285 L398,275 L410,285 L405,300 L390,305 L378,295Z', cx: 393, cy: 290 },
        GIZA: { d: 'M350,270 L382,265 L390,290 L385,310 L360,320 L340,300Z', cx: 365, cy: 290 },
        ALEXANDRIA: { d: 'M280,200 L320,190 L335,210 L315,225 L280,220Z', cx: 305, cy: 208 },
        QALUBIA: { d: 'M385,255 L410,250 L418,270 L405,280 L382,275Z', cx: 398, cy: 265 },
        SHARQIA: { d: 'M415,260 L450,250 L460,280 L445,300 L415,295Z', cx: 435, cy: 275 },
        DAKHALIA: { d: 'M370,215 L410,205 L420,235 L400,250 L375,245Z', cx: 395, cy: 230 },
        GHARBIA: { d: 'M330,220 L365,215 L370,240 L345,248 L325,240Z', cx: 348, cy: 232 },
        MENOFIA: { d: 'M340,245 L370,240 L378,260 L355,268 L338,258Z', cx: 356, cy: 252 },
        BEHEIRA: { d: 'M270,225 L325,218 L335,255 L310,270 L265,260Z', cx: 300, cy: 242 },
        KAFR_EL_SHEIKH: { d: 'M310,190 L355,185 L365,215 L335,225 L305,215Z', cx: 335, cy: 205 },
        DAMIETTA: { d: 'M395,190 L425,185 L430,205 L410,210 L395,205Z', cx: 412, cy: 198 },
        PORT_SAID: { d: 'M440,195 L465,190 L470,210 L455,215 L440,210Z', cx: 455, cy: 202 },
        ISMAILIA: { d: 'M450,220 L475,215 L485,250 L465,260 L445,245Z', cx: 465, cy: 238 },
        SUEZ: { d: 'M460,265 L485,260 L490,295 L470,305 L455,290Z', cx: 472, cy: 280 },
        FAYOUM: { d: 'M325,310 L360,305 L365,340 L340,350 L320,335Z', cx: 342, cy: 325 },
        BENI_SUEF: { d: 'M340,345 L375,340 L380,380 L355,390 L335,375Z', cx: 358, cy: 365 },
        MINYA: { d: 'M335,385 L375,378 L382,430 L350,440 L330,420Z', cx: 356, cy: 410 },
        ASSIUT: { d: 'M330,435 L370,428 L378,480 L345,490 L325,470Z', cx: 352, cy: 458 },
        SOHAG: { d: 'M325,485 L365,478 L372,530 L340,540 L320,520Z', cx: 348, cy: 508 },
        QENA: { d: 'M335,535 L370,528 L380,575 L350,585 L330,570Z', cx: 355, cy: 558 },
        LUXOR: { d: 'M340,580 L370,575 L375,600 L350,608 L335,598Z', cx: 355, cy: 592 },
        ASWAN: { d: 'M330,605 L375,598 L385,650 L345,660 L320,640Z', cx: 355, cy: 630 },
        RED_SEA: { d: 'M420,300 L500,280 L520,500 L480,550 L400,480 L390,350Z', cx: 460, cy: 400 },
        NEW_VALLEY: { d: 'M150,340 L320,330 L325,600 L200,620 L120,500Z', cx: 240, cy: 470 },
        MATROUH: { d: 'M80,180 L265,170 L270,260 L200,280 L80,270Z', cx: 180, cy: 220 },
        NORTH_SINAI: { d: 'M475,190 L540,170 L550,230 L510,250 L475,235Z', cx: 510, cy: 210 },
        SOUTH_SINAI: { d: 'M490,250 L545,235 L555,340 L520,370 L485,310Z', cx: 520, cy: 300 },
    };

    // Arabic names for governorates
    const GOV_NAMES = {
        CAIRO: 'القاهرة', GIZA: 'الجيزة', ALEXANDRIA: 'الإسكندرية', DAKHALIA: 'الدقهلية',
        RED_SEA: 'البحر الأحمر', BEHEIRA: 'البحيرة', FAYOUM: 'الفيوم', GHARBIA: 'الغربية',
        ISMAILIA: 'الإسماعيلية', MENOFIA: 'المنوفية', MINYA: 'المنيا', QALUBIA: 'القليوبية',
        NEW_VALLEY: 'الوادي الجديد', SUEZ: 'السويس', ASWAN: 'أسوان', ASSIUT: 'أسيوط',
        BENI_SUEF: 'بني سويف', PORT_SAID: 'بورسعيد', DAMIETTA: 'دمياط', SHARQIA: 'الشرقية',
        SOUTH_SINAI: 'جنوب سيناء', KAFR_EL_SHEIKH: 'كفر الشيخ', MATROUH: 'مطروح',
        LUXOR: 'الأقصر', QENA: 'قنا', NORTH_SINAI: 'شمال سيناء', SOHAG: 'سوهاج'
    };

    async function load() {
        try {
            geoData = await ReportsMain.fetchApi('geographic');
            renderMap();
            renderTopGovernoratesChart();
            renderTable();
        } catch (e) {
            console.error('Geographic load error:', e);
        }
    }

    function setMapMode(mode) {
        mapMode = mode;
        document.querySelectorAll('.map-mode-btn').forEach(b => b.classList.remove('active'));
        document.querySelector(`.map-mode-btn[data-mode="${mode}"]`).classList.add('active');
        renderMap();
    }

    function renderMap() {
        const container = document.getElementById('egyptMapContainer');
        const tooltip = document.getElementById('mapTooltip');

        // Build SVG
        const stats = geoData?.statsByGovernorate || {};
        let maxVal = 0;
        Object.values(stats).forEach(s => {
            const v = mapMode === 'orders' ? s.totalOrders : mapMode === 'rate' ? s.deliveryRate : Number(s.totalRevenue || 0);
            if (v > maxVal) maxVal = v;
        });

        let svgPaths = '';
        Object.keys(GOV_PATHS).forEach(key => {
            const path = GOV_PATHS[key];
            const stat = stats[key] || { totalOrders: 0, deliveryRate: 0, totalRevenue: 0 };
            const val = mapMode === 'orders' ? stat.totalOrders : mapMode === 'rate' ? stat.deliveryRate : Number(stat.totalRevenue || 0);
            const intensity = maxVal > 0 ? val / maxVal : 0;

            // Monochrome gradient: light gray → dark
            const r = Math.round(229 - intensity * 206);
            const g = Math.round(229 - intensity * 206);
            const b = Math.round(229 - intensity * 206);
            const fill = `rgb(${r},${g},${b})`;

            svgPaths += `<path d="${path.d}" fill="${fill}" data-gov="${key}" data-orders="${stat.totalOrders}" data-rate="${stat.deliveryRate}" data-revenue="${Number(stat.totalRevenue || 0)}" />`;
        });

        // Remove old SVG but keep tooltip
        const oldSvg = container.querySelector('svg');
        if (oldSvg) oldSvg.remove();

        const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        svg.setAttribute('viewBox', '60 160 520 530');
        svg.setAttribute('preserveAspectRatio', 'xMidYMid meet');
        svg.innerHTML = svgPaths;
        container.insertBefore(svg, tooltip);

        // Tooltip events
        svg.querySelectorAll('path').forEach(p => {
            p.addEventListener('mouseenter', e => {
                const gov = e.target.getAttribute('data-gov');
                const name = GOV_NAMES[gov] || gov;
                const orders = e.target.getAttribute('data-orders');
                const rate = e.target.getAttribute('data-rate');
                const revenue = e.target.getAttribute('data-revenue');
                tooltip.innerHTML = `
                    <div class="tooltip-title">${name}</div>
                    <div class="tooltip-row"><span>الطلبات:</span><span>${Number(orders).toLocaleString('ar-EG')}</span></div>
                    <div class="tooltip-row"><span>نسبة التوصيل:</span><span>${Number(rate).toFixed(1)}%</span></div>
                    <div class="tooltip-row"><span>الإيرادات:</span><span>${Number(revenue).toLocaleString('ar-EG', {minimumFractionDigits:2})}</span></div>`;
                tooltip.classList.add('visible');
            });
            p.addEventListener('mousemove', e => {
                const rect = container.getBoundingClientRect();
                tooltip.style.left = (e.clientX - rect.left + 15) + 'px';
                tooltip.style.top = (e.clientY - rect.top - 10) + 'px';
            });
            p.addEventListener('mouseleave', () => {
                tooltip.classList.remove('visible');
            });
        });
    }

    function renderTopGovernoratesChart() {
        const stats = geoData?.statsByGovernorate || {};
        const sorted = Object.entries(stats)
            .map(([key, val]) => ({ key, name: GOV_NAMES[key] || key, ...val }))
            .sort((a, b) => b.totalOrders - a.totalOrders)
            .slice(0, 10);

        if (sorted.length === 0) {
            ReportsMain.showEmpty('chartTopGovernorates');
            return;
        }

        const opts = {
            ...ReportsMain.getBaseChartOptions(),
            chart: { ...ReportsMain.getBaseChartOptions().chart, type: 'bar', height: Math.max(300, sorted.length * 38) },
            series: [{ name: 'عدد الطلبات', data: sorted.map(s => s.totalOrders) }],
            xaxis: { ...ReportsMain.getBaseChartOptions().xaxis, categories: sorted.map(s => s.name) },
            colors: ['#171717'],
            plotOptions: { bar: { horizontal: true, borderRadius: 4, barHeight: '60%' } },
            dataLabels: { enabled: true, formatter: v => v.toLocaleString('ar-EG'), style: { fontSize: '11px', fontFamily: "'Cairo',sans-serif", fontWeight: 700 } },
            tooltip: { ...ReportsMain.getBaseChartOptions().tooltip }
        };
        const chart = new ApexCharts(document.getElementById('chartTopGovernorates'), opts);
        chart.render();
        ReportsMain.registerChart('chartTopGovernorates', chart);
    }

    function renderTable() {
        const stats = geoData?.statsByGovernorate || {};
        const sorted = Object.entries(stats)
            .map(([key, val]) => ({ name: GOV_NAMES[key] || key, ...val }))
            .sort((a, b) => b.totalOrders - a.totalOrders);

        const tbody = document.querySelector('#tableGeo tbody');
        if (sorted.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:var(--text-muted);padding:var(--space-6)">لا توجد بيانات</td></tr>';
            return;
        }
        tbody.innerHTML = sorted.map(s => `<tr>
            <td class="td-name">${s.name}</td>
            <td class="td-value">${ReportsMain.formatNumber(s.totalOrders)}</td>
            <td>
                <div class="progress-inline">
                    <div class="progress-bar-mini"><div class="progress-bar-mini-fill" style="width:${Math.min(s.deliveryRate, 100)}%"></div></div>
                    <span class="progress-label">${s.deliveryRate.toFixed(1)}%</span>
                </div>
            </td>
            <td class="td-value">${ReportsMain.formatMoney(s.totalRevenue)}</td>
        </tr>`).join('');
    }

    return { load, setMapMode };
})();
