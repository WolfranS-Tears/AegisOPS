/**
 * AegisOps — Cliente HTTP de la API y utilidades compartidas.
 */
(() => {
    const BASE = globalThis.AEGIS_CONFIG.apiBase;

    class ApiError extends Error {
        constructor(status, problem) {
            super(problem?.detail || problem?.title || `Error HTTP ${status}`);
            this.status = status;
            this.problem = problem || {};
            this.errores = problem?.errores || {};
        }
    }

    function query(params = {}) {
        const q = new URLSearchParams();
        Object.entries(params).forEach(([k, v]) => {
            if (v !== undefined && v !== null && v !== '') q.append(k, v);
        });
        const s = q.toString();
        return s ? `?${s}` : '';
    }

    async function request(method, path, body) {
        let res;
        try {
            res = await fetch(BASE + path, {
                method,
                headers: body !== undefined ? { 'Content-Type': 'application/json' } : {},
                body: body !== undefined ? JSON.stringify(body) : undefined,
            });
        } catch {
            throw new ApiError(0, { detail: 'No se pudo conectar con el backend. ¿Está corriendo en ' + BASE + '?' });
        }
        const texto = await res.text();
        const data = texto ? JSON.parse(texto) : null;
        if (!res.ok) throw new ApiError(res.status, data);
        return data;
    }

    const api = {
        get: (path, params) => request('GET', path + query(params)),
        post: (path, body) => request('POST', path, body ?? {}),
        put: (path, body) => request('PUT', path, body),
        patch: (path, body) => request('PATCH', path, body),
    };

    // ---- Utilidades de presentación -------------------------------------------

    const escapeHtml = (v) => String(v ?? '')
        .replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;').replaceAll("'", '&#39;');

    const fmtFecha = (iso) => {
        if (!iso) return '—';
        const d = new Date(iso);
        return d.toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' });
    };

    const fmtNum = (n, dec = 2) => (n === null || n === undefined) ? '—'
        : Number(n).toLocaleString('es-CO', { maximumFractionDigits: dec });

    const PRIORIDAD = {
        CRITICA: { texto: 'Crítica', clase: 'bg-severity-p1/15 text-severity-p1 border-severity-p1/40' },
        ALTA: { texto: 'Alta', clase: 'bg-severity-p2/15 text-severity-p2 border-severity-p2/40' },
        MEDIA: { texto: 'Media', clase: 'bg-accent/15 text-accent border-accent/40' },
        BAJA: { texto: 'Baja', clase: 'bg-severity-p4/20 text-dark-secondary border-severity-p4/40' },
    };
    const ESTADO_INC = {
        ABIERTO: { texto: 'Abierto', clase: 'bg-severity-p1/15 text-severity-p1 border-severity-p1/40' },
        EN_PROGRESO: { texto: 'En progreso', clase: 'bg-severity-p2/15 text-severity-p2 border-severity-p2/40' },
        RESUELTO: { texto: 'Resuelto', clase: 'bg-success/15 text-success border-success/40' },
        CERRADO: { texto: 'Cerrado', clase: 'bg-severity-p4/20 text-dark-secondary border-severity-p4/40' },
    };
    const ESTADO_CONSULTA = {
        PENDIENTE: { texto: 'Pendiente', clase: 'bg-severity-p2/15 text-severity-p2 border-severity-p2/40' },
        RESPONDIDA: { texto: 'Respondida', clase: 'bg-success/15 text-success border-success/40' },
        SIN_RESULTADOS: { texto: 'Sin resultados', clase: 'bg-severity-p4/20 text-dark-secondary border-severity-p4/40' },
        ERROR: { texto: 'Error', clase: 'bg-severity-p1/15 text-severity-p1 border-severity-p1/40' },
    };
    const ESTADO_ART = {
        ACTIVO: { texto: 'Activo', clase: 'bg-success/15 text-success border-success/40' },
        BORRADOR: { texto: 'Borrador', clase: 'bg-severity-p2/15 text-severity-p2 border-severity-p2/40' },
        INACTIVO: { texto: 'Inactivo', clase: 'bg-severity-p4/20 text-dark-secondary border-severity-p4/40' },
    };

    const badge = (mapa, valor) => {
        const m = mapa[valor] || { texto: valor, clase: 'border-dark-outline text-dark-secondary' };
        return `<span class="inline-flex items-center px-2 py-0.5 rounded-sm border text-label-sm font-code ${m.clase}">${escapeHtml(m.texto)}</span>`;
    };

    function toast(mensaje, tipo = 'ok') {
        let cont = document.getElementById('toasts');
        if (!cont) {
            cont = document.createElement('div');
            cont.id = 'toasts';
            cont.className = 'fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-sm';
            document.body.appendChild(cont);
        }
        const colores = tipo === 'error'
            ? 'border-severity-p1/50 bg-dark-surface-container text-dark-on-surface'
            : 'border-success/50 bg-dark-surface-container text-dark-on-surface';
        const icono = tipo === 'error' ? 'error' : 'check_circle';
        const el = document.createElement('div');
        el.setAttribute('role', 'status');
        el.className = `flex items-start gap-2 px-4 py-3 rounded border shadow-lg text-body-md ${colores}`;
        el.innerHTML = `<span class="material-symbols-outlined text-[18px] ${tipo === 'error' ? 'text-severity-p1' : 'text-success'}">${icono}</span><span>${escapeHtml(mensaje)}</span>`;
        cont.appendChild(el);
        setTimeout(() => el.remove(), 4500);
    }

    /** Pinta errores de validación del backend debajo de cada campo del formulario. */
    function mostrarErroresFormulario(form, error) {
        form.querySelectorAll('[data-error-for]').forEach(e => { e.textContent = ''; });
        Object.entries(error.errores || {}).forEach(([campo, msg]) => {
            const el = form.querySelector(`[data-error-for="${campo}"]`);
            if (el) el.textContent = msg;
        });
    }

    async function cargarOpciones(select, path, { valor = 'id', texto = 'nombre', vacio = 'Todas' } = {}) {
        const items = await api.get(path);
        select.innerHTML = (vacio ? `<option value="">${escapeHtml(vacio)}</option>` : '')
            + items.map(i => `<option value="${escapeHtml(i[valor])}">${escapeHtml(i[texto])}</option>`).join('');
        return items;
    }

    globalThis.Aegis = {
        api, ApiError, escapeHtml, fmtFecha, fmtNum, badge, toast, mostrarErroresFormulario, cargarOpciones,
        PRIORIDAD, ESTADO_INC, ESTADO_CONSULTA, ESTADO_ART,
    };
})();
