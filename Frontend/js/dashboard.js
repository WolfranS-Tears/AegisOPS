/**
 * AegisOps — Dashboard analítico (RF07).
 * Todos los valores vienen de GET /api/analitica/dashboard (SQL sobre datos reales).
 */
(() => {
    const { api, fmtNum, escapeHtml, toast, cargarOpciones, PRIORIDAD, ESTADO_INC, badge } = globalThis.Aegis;

    const COLORES = {
        acento: '#0EA5E9', exito: '#10B981', ia: '#8B5CF6', alerta: '#F59E0B', critico: '#EF4444', neutro: '#64748B',
        texto: '#94A3B8', rejilla: '#1E293B',
    };
    const COLOR_PRIORIDAD = { CRITICA: COLORES.critico, ALTA: COLORES.alerta, MEDIA: COLORES.acento, BAJA: COLORES.neutro };
    const COLOR_ESTADO = { ABIERTO: COLORES.critico, EN_PROGRESO: COLORES.alerta, RESUELTO: COLORES.exito, CERRADO: COLORES.neutro };

    Chart.defaults.color = COLORES.texto;
    Chart.defaults.borderColor = COLORES.rejilla;
    Chart.defaults.font.family = '"Plus Jakarta Sans", sans-serif';

    const graficos = {};
    const form = document.getElementById('filtros');
    const estadoCarga = document.getElementById('estado-carga');

    function dibujar(id, config) {
        graficos[id]?.destroy();
        graficos[id] = new Chart(document.getElementById(id), config);
    }

    const opcionesBase = (extra = {}) => ({
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { labels: { boxWidth: 12 } }, ...extra.plugins },
        ...extra,
    });

    const set = (id, v) => { document.getElementById(id).textContent = v; };

    function filtrosActuales() {
        const datos = Object.fromEntries(new FormData(form).entries());
        return datos;
    }

    async function cargar() {
        estadoCarga.textContent = 'Calculando…';
        try {
            const d = await api.get('/analitica/dashboard', filtrosActuales());
            pintarResumen(d.resumen);
            pintarTendencia(d.tendenciaSemanal);
            pintarCategorias(d.porCategoria, d.tiemposPorCategoria);
            pintarDona('g-prioridad', d.porPrioridad, COLOR_PRIORIDAD, PRIORIDAD);
            pintarDona('g-estado', d.porEstado, COLOR_ESTADO, ESTADO_INC);
            pintarCriticas(d.categoriasCriticas);
            pintarSla(d.sla);
            pintarAsistente(d.asistente);
            pintarResponsables(d.responsables);
            estadoCarga.textContent = `Actualizado ${new Date().toLocaleTimeString('es-CO')}`;
        } catch (e) {
            estadoCarga.textContent = '';
            toast(e.message, 'error');
        }
    }

    function pintarResumen(r) {
        set('k-total', fmtNum(r.total, 0));
        set('k-abiertos', `${fmtNum(r.abiertos, 0)} abiertos o en progreso`);
        set('k-resolucion', r.porcentaje_resolucion == null ? '—' : `${fmtNum(r.porcentaje_resolucion)}%`);
        set('k-resueltos', `${fmtNum(r.resueltos, 0)} resueltos o cerrados`);
        set('k-promedio', r.tiempo_promedio_horas == null ? '—' : `${fmtNum(r.tiempo_promedio_horas, 1)} h`);
        set('k-mediana', r.tiempo_mediana_horas == null ? '—' : `${fmtNum(r.tiempo_mediana_horas, 1)} h`);
        set('k-criticos', fmtNum(r.criticos_abiertos, 0));
    }

    function pintarTendencia(filas) {
        dibujar('g-tendencia', {
            type: 'line',
            data: {
                labels: filas.map(f => f.semana),
                datasets: [
                    { label: 'Creados', data: filas.map(f => f.creados), borderColor: COLORES.acento, backgroundColor: COLORES.acento + '33', fill: true, tension: 0.3, pointRadius: 2 },
                    { label: 'Resueltos', data: filas.map(f => f.resueltos), borderColor: COLORES.exito, backgroundColor: COLORES.exito, tension: 0.3, pointRadius: 2 },
                ],
            },
            options: opcionesBase({
                interaction: { mode: 'index', intersect: false },
                scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
            }),
        });
    }

    function pintarCategorias(dist, tiempos) {
        dibujar('g-categoria', {
            type: 'bar',
            data: {
                labels: dist.map(f => f.etiqueta),
                datasets: [{ label: 'Incidentes', data: dist.map(f => f.cantidad), backgroundColor: COLORES.acento, borderRadius: 4 }],
            },
            options: opcionesBase({
                indexAxis: 'y',
                plugins: {
                    legend: { display: false },
                    tooltip: { callbacks: { afterLabel: (c) => `${fmtNum(dist[c.dataIndex].porcentaje)}% del total` } },
                },
                scales: { x: { beginAtZero: true, ticks: { precision: 0 } } },
            }),
        });
        const t = tiempos.filter(f => f.promedio_horas != null);
        dibujar('g-tiempos', {
            type: 'bar',
            data: {
                labels: t.map(f => f.etiqueta),
                datasets: [{ label: 'Horas promedio', data: t.map(f => Number(f.promedio_horas)), backgroundColor: COLORES.ia, borderRadius: 4 }],
            },
            options: opcionesBase({
                indexAxis: 'y',
                plugins: {
                    legend: { display: false },
                    tooltip: { callbacks: { afterLabel: (c) => `${t[c.dataIndex].resueltos} resueltos` } },
                },
                scales: { x: { beginAtZero: true, title: { display: true, text: 'horas' } } },
            }),
        });
    }

    function pintarDona(id, filas, colores, nombres) {
        dibujar(id, {
            type: 'doughnut',
            data: {
                labels: filas.map(f => nombres[f.etiqueta]?.texto || f.etiqueta),
                datasets: [{
                    data: filas.map(f => f.cantidad),
                    backgroundColor: filas.map(f => colores[f.etiqueta] || COLORES.neutro),
                    borderColor: '#111827', borderWidth: 2,
                }],
            },
            options: opcionesBase({
                cutout: '60%',
                plugins: {
                    legend: { position: 'right' },
                    tooltip: { callbacks: { label: (c) => ` ${c.label}: ${c.raw} (${fmtNum(filas[c.dataIndex].porcentaje)}%)` } },
                },
            }),
        });
    }

    function tabla(id, columnas, filas, vacio = 'Sin datos para los filtros seleccionados') {
        const el = document.getElementById(id);
        const thead = `<thead><tr>${columnas.map(c => `<th class="${c.num ? 'num' : ''}">${escapeHtml(c.titulo)}</th>`).join('')}</tr></thead>`;
        const cuerpo = filas.length === 0
            ? `<tr><td colspan="${columnas.length}" class="text-dark-secondary">${escapeHtml(vacio)}</td></tr>`
            : filas.map(f => `<tr>${columnas.map(c => `<td class="${c.num ? 'num' : ''}">${c.render ? c.render(f) : escapeHtml(f[c.campo] ?? '—')}</td>`).join('')}</tr>`).join('');
        el.innerHTML = `${thead}<tbody>${cuerpo}</tbody>`;
    }

    const pct = (campo) => (f) => f[campo] == null ? '—' : `${fmtNum(f[campo])}%`;
    const horas = (campo) => (f) => f[campo] == null ? '—' : fmtNum(f[campo], 1);

    function pintarCriticas(filas) {
        tabla('t-criticas', [
            { titulo: '#', render: (f) => filas.indexOf(f) + 1 },
            { titulo: 'Categoría', campo: 'categoria' },
            { titulo: 'Total', campo: 'total', num: true },
            { titulo: 'Abiertos', campo: 'abiertos', num: true },
            { titulo: 'Alta/Crítica', campo: 'alta_o_critica', num: true },
            { titulo: '% Alta/Crítica', num: true, render: pct('porcentaje_alta_critica') },
            { titulo: '% Resolución', num: true, render: pct('porcentaje_resolucion') },
            { titulo: 'Horas prom.', num: true, render: horas('promedio_horas') },
        ], filas);
    }

    function pintarSla(filas) {
        tabla('t-sla', [
            { titulo: 'Prioridad', render: (f) => badge(PRIORIDAD, f.prioridad) },
            { titulo: 'Objetivo (h)', campo: 'objetivo_horas', num: true },
            { titulo: 'Resueltos', campo: 'resueltos', num: true },
            { titulo: 'Dentro', campo: 'dentro_objetivo', num: true },
            {
                titulo: 'Cumplimiento', num: true, render: (f) => {
                    const v = f.porcentaje_cumplimiento;
                    if (v == null) return '—';
                    const color = v >= 80 ? 'text-success' : v >= 60 ? 'text-severity-p2' : 'text-severity-p1';
                    return `<span class="${color} font-semibold">${fmtNum(v)}%</span>`;
                },
            },
        ], filas);
    }

    function pintarAsistente(a) {
        const total = a.consultasPorEstado.reduce((s, e) => s + Number(e.cantidad), 0);
        const porEstado = Object.fromEntries(a.consultasPorEstado.map(e => [e.estado, e.cantidad]));
        const tile = (titulo, valor, extra = '') => `<div class="rounded border border-dark-outline p-3">
            <div class="text-body-sm text-dark-secondary">${titulo}</div>
            <div class="font-headline text-title-lg font-bold tabular-nums ${extra}">${valor}</div></div>`;
        document.getElementById('asistente-kpis').innerHTML =
            tile('Consultas', fmtNum(total, 0))
            + tile('Respondidas', fmtNum(porEstado.RESPONDIDA || 0, 0), 'text-success')
            + tile('Sin resultados / error', `${fmtNum(porEstado.SIN_RESULTADOS || 0, 0)} / ${fmtNum(porEstado.ERROR || 0, 0)}`)
            + tile('Calificación', a.calificaciones.promedio == null ? '—'
                : `${fmtNum(a.calificaciones.promedio)} <span class="text-body-sm text-dark-secondary">(${a.calificaciones.calificadas})</span>`, 'text-severity-p2');
        tabla('t-articulos', [
            { titulo: 'Artículo más usado', render: (f) => `<span class="font-code text-dark-secondary">#${f.articulo_id}</span> ${escapeHtml(f.titulo)}` },
            { titulo: 'Usos', campo: 'usos', num: true },
            { titulo: 'Similitud prom.', num: true, render: (f) => fmtNum(f.similitud_promedio, 3) },
        ], a.articulosMasUsados, 'Aún no hay respuestas con fuentes');
    }

    function pintarResponsables(filas) {
        tabla('t-responsables', [
            { titulo: 'Responsable', campo: 'responsable' },
            { titulo: 'Área', campo: 'area' },
            { titulo: 'Asignados', campo: 'asignados', num: true },
            { titulo: 'Abiertos', campo: 'abiertos', num: true },
            { titulo: 'Resueltos', campo: 'resueltos', num: true },
            { titulo: 'Horas prom.', num: true, render: horas('promedio_horas') },
        ], filas);
    }

    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const { desde, hasta } = filtrosActuales();
        if (desde && hasta && hasta < desde) {
            toast('La fecha "hasta" no puede ser anterior a "desde"', 'error');
            return;
        }
        cargar();
    });
    document.getElementById('btn-limpiar').addEventListener('click', () => {
        form.reset();
        cargar();
    });

    cargarOpciones(document.getElementById('f-categoria'), '/categorias')
        .catch(() => { /* el error se muestra al cargar el dashboard */ })
        .finally(cargar);
})();
