/**
 * AegisOps — Historial de consultas al asistente (RF08).
 */
(() => {
    const { api, escapeHtml, toast } = globalThis.Aegis;
    const { tarjeta, activarCalificacion } = globalThis.AegisConsulta;

    const form = document.getElementById('filtros');
    const lista = document.getElementById('lista');
    const resumen = document.getElementById('resumen');
    activarCalificacion(lista);

    async function cargar() {
        const { incidenteId, estado } = Object.fromEntries(new FormData(form).entries());
        lista.innerHTML = '<p class="text-body-sm text-dark-secondary">Cargando…</p>';
        try {
            let consultas = await api.get('/asistente/consultas', { incidenteId });
            if (estado) consultas = consultas.filter(c => c.estado === estado);
            resumen.textContent = `${consultas.length} consulta(s)`;
            lista.innerHTML = consultas.length
                ? consultas.map(c => tarjeta(c, { compacta: true })).join('')
                : '<p class="text-body-sm text-dark-secondary">No hay consultas para estos filtros.</p>';
        } catch (e) {
            lista.innerHTML = `<p class="text-body-sm text-severity-p1">${escapeHtml(e.message)}</p>`;
            toast(e.message, 'error');
        }
    }

    form.addEventListener('submit', (e) => { e.preventDefault(); cargar(); });
    const inicial = new URLSearchParams(location.search).get('incidenteId');
    if (inicial) form.incidenteId.value = inicial;
    cargar();
})();
