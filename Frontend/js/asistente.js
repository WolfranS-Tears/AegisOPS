/**
 * AegisOps — Asistente RAG (RF04, RF05, RF08).
 * Envía la consulta a POST /api/asistente/consultas (nunca a n8n directamente).
 */
(() => {
    const { api, escapeHtml, toast, badge, PRIORIDAD, ESTADO_INC } = globalThis.Aegis;
    const { tarjeta, activarCalificacion } = globalThis.AegisConsulta;

    const form = document.getElementById('form-consulta');
    const selInc = document.getElementById('c-incidente');
    const pregunta = document.getElementById('c-pregunta');
    const errorPregunta = document.getElementById('error-pregunta');
    const info = document.getElementById('incidente-info');
    const resultado = document.getElementById('resultado');
    const historial = document.getElementById('historial');
    const btn = document.getElementById('btn-consultar');
    const textoBtn = btn.innerHTML;

    activarCalificacion(resultado);
    activarCalificacion(historial);

    async function cargarIncidentes(seleccionado) {
        // Incidentes abiertos/en progreso primero, más el seleccionado si viene por URL
        const [abiertos, enProgreso] = await Promise.all([
            api.get('/incidentes', { estado: 'ABIERTO', tamano: 100 }),
            api.get('/incidentes', { estado: 'EN_PROGRESO', tamano: 100 }),
        ]);
        const lista = [...abiertos.contenido, ...enProgreso.contenido];
        if (seleccionado && !lista.some(i => String(i.id) === seleccionado)) {
            try { lista.unshift(await api.get(`/incidentes/${seleccionado}`)); } catch { /* id inválido */ }
        }
        selInc.innerHTML = '<option value="">Sin incidente — consulta libre</option>'
            + lista.map(i => `<option value="${i.id}">#${i.id} · ${escapeHtml(i.titulo)}</option>`).join('');
        selInc.dataset.cache = JSON.stringify(lista);
        if (seleccionado) selInc.value = seleccionado;
    }

    function incidenteActual() {
        const lista = JSON.parse(selInc.dataset.cache || '[]');
        return lista.find(i => String(i.id) === selInc.value) || null;
    }

    async function alCambiarIncidente(prellenar) {
        const inc = incidenteActual();
        if (inc) {
            info.classList.remove('hidden');
            info.innerHTML = `<div class="flex flex-wrap items-center gap-2 mb-1">
                <span class="font-semibold">#${inc.id} ${escapeHtml(inc.titulo)}</span>
                ${badge(PRIORIDAD, inc.prioridad)} ${badge(ESTADO_INC, inc.estado)}
                <span class="text-dark-secondary">${escapeHtml(inc.categoria)}</span></div>
                <div class="text-dark-secondary">${escapeHtml(inc.descripcion)}</div>`;
            if (prellenar) pregunta.value = inc.descripcion;
        } else {
            info.classList.add('hidden');
        }
        await cargarHistorial();
    }

    async function cargarHistorial() {
        const id = selInc.value;
        document.getElementById('titulo-historial').textContent = id ? `Historial del incidente #${id}` : 'Últimas consultas';
        historial.innerHTML = '<p class="text-body-sm text-dark-secondary">Cargando…</p>';
        try {
            let consultas = await api.get('/asistente/consultas', id ? { incidenteId: id } : {});
            if (!id) consultas = consultas.slice(0, 5);
            historial.innerHTML = consultas.length
                ? consultas.map(c => tarjeta(c, { compacta: true })).join('')
                : '<p class="text-body-sm text-dark-secondary">Todavía no hay consultas registradas.</p>';
        } catch (e) {
            historial.innerHTML = `<p class="text-body-sm text-severity-p1">${escapeHtml(e.message)}</p>`;
        }
    }

    selInc.addEventListener('change', () => alCambiarIncidente(true));

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        errorPregunta.textContent = '';
        const texto = pregunta.value.trim();
        // Prueba 7: una descripción vacía no ejecuta el flujo inteligente
        if (!texto) {
            errorPregunta.textContent = 'La descripción del problema es obligatoria.';
            pregunta.focus();
            return;
        }
        if (texto.length < 10) {
            errorPregunta.textContent = 'Describa el problema con al menos 10 caracteres.';
            pregunta.focus();
            return;
        }
        btn.disabled = true;
        btn.innerHTML = '<span class="material-symbols-outlined text-[18px] spinner">progress_activity</span>Buscando fuentes y generando…';
        resultado.innerHTML = '';
        try {
            const consulta = await api.post('/asistente/consultas', {
                incidenteId: selInc.value ? Number(selInc.value) : null,
                pregunta: texto,
            });
            resultado.innerHTML = tarjeta(consulta);
            if (consulta.estado === 'ERROR') toast('La consulta quedó registrada con estado Error', 'error');
            await cargarHistorial();
        } catch (err) {
            errorPregunta.textContent = err.errores?.pregunta || '';
            toast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.innerHTML = textoBtn;
        }
    });

    const inicial = new URLSearchParams(location.search).get('incidenteId');
    cargarIncidentes(inicial)
        .then(() => alCambiarIncidente(Boolean(inicial)))
        .catch(e => toast(e.message, 'error'));
})();
