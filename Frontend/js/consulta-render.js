/**
 * AegisOps — Renderizado compartido de una consulta RAG (asistente e historial),
 * incluida la calificación de 1 a 5 (RF08).
 */
(() => {
    const { api, escapeHtml, fmtFecha, fmtNum, badge, toast, ESTADO_CONSULTA } = globalThis.Aegis;

    function barraSimilitud(sim) {
        const pct = Math.round(Number(sim) * 100);
        return `<div class="flex items-center gap-2 min-w-[140px]">
            <div class="flex-1 h-1.5 rounded-full bg-dark-surface-container-high overflow-hidden">
                <div class="h-full bg-rag-ai" style="width:${pct}%"></div></div>
            <span class="font-code text-body-sm tabular-nums">${fmtNum(sim, 3)}</span></div>`;
    }

    function estrellas(respuestaId, actual) {
        return `<div class="flex items-center gap-1" role="radiogroup" aria-label="Calificar utilidad de 1 a 5" data-calificar="${respuestaId}">
            ${[1, 2, 3, 4, 5].map(n => `<button type="button" role="radio" aria-checked="${actual === n}" aria-label="${n} de 5"
                data-puntos="${n}" class="material-symbols-outlined estrella ${actual >= n ? 'activa' : ''}">star</button>`).join('')}
        </div>`;
    }

    /** Devuelve el HTML de la tarjeta completa de una consulta. */
    function tarjeta(c, { compacta = false } = {}) {
        const r = c.respuesta;
        let cuerpo = '';
        if (c.estado === 'ERROR') {
            cuerpo = `<div class="flex gap-2 p-3 rounded border border-severity-p1/40 bg-severity-p1/10 text-body-md">
                <span class="material-symbols-outlined text-severity-p1">error</span>
                <div><div class="font-semibold">No fue posible generar la orientación</div>
                <div class="text-dark-secondary text-body-sm break-words">${escapeHtml(c.mensajeError)}</div></div></div>`;
        } else if (r) {
            const sinRes = c.estado === 'SIN_RESULTADOS';
            cuerpo = `
                <div class="${sinRes ? 'p-3 rounded border border-dark-outline-variant bg-dark-surface-container/50' : ''}">
                    <div class="text-label-md text-dark-secondary mb-1">${sinRes ? 'Sin información suficiente' : 'Solución sugerida'}</div>
                    <p class="text-body-lg leading-relaxed">${escapeHtml(r.respuesta)}</p>
                </div>
                ${r.pasos.length ? `<div>
                    <div class="text-label-md text-dark-secondary mb-2">Pasos recomendados</div>
                    <ol class="space-y-2">${r.pasos.map((p, i) => `<li class="flex gap-3 text-body-md">
                        <span class="shrink-0 w-6 h-6 rounded-full bg-accent/15 text-accent text-label-md flex items-center justify-center">${i + 1}</span>
                        <span>${escapeHtml(p.replace(/^\d+[.)]\s*/, ''))}</span></li>`).join('')}</ol></div>` : ''}
                ${r.fuentes.length ? `<div>
                    <div class="text-label-md text-dark-secondary mb-2">Fuentes y similitud</div>
                    <ul class="divide-y divide-dark-outline border border-dark-outline rounded">${r.fuentes.map(f => `
                        <li class="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
                            <div class="min-w-0"><span class="font-code text-dark-secondary">#${f.articuloId}</span>
                                <span class="font-semibold">${escapeHtml(f.titulo)}</span>
                                <span class="text-body-sm text-dark-secondary">· ${escapeHtml(f.categoria)}</span></div>
                            ${barraSimilitud(f.similitud)}</li>`).join('')}</ul></div>` : ''}
                <div class="flex flex-wrap items-center gap-3 pt-1">
                    <span class="text-label-md text-dark-secondary">¿Fue útil?</span>
                    ${estrellas(r.id, r.calificacion?.puntuacion || 0)}
                    <label class="sr-only" for="coment-${r.id}">Comentario</label>
                    <input id="coment-${r.id}" data-comentario="${r.id}" maxlength="500" class="campo !py-1 !text-body-sm flex-1 min-w-[180px]"
                        placeholder="Comentario opcional" value="${escapeHtml(r.calificacion?.comentario || '')}">
                    <span class="text-body-sm text-dark-secondary" data-cal-estado="${r.id}">${r.calificacion ? 'Calificada ' + fmtFecha(r.calificacion.fecha) : ''}</span>
                </div>`;
        }
        return `<article class="tarjeta p-4 space-y-4" data-consulta="${c.id}">
            <header class="flex flex-wrap items-start justify-between gap-2">
                <div class="min-w-0">
                    <div class="text-body-sm text-dark-secondary font-code">Consulta #${c.id} · ${fmtFecha(c.fecha)} · umbral ${fmtNum(c.umbral, 2)}</div>
                    ${c.incidenteId ? `<a href="asistente.html?incidenteId=${c.incidenteId}" class="text-body-sm text-accent hover:underline">Incidente #${c.incidenteId}: ${escapeHtml(c.incidente)}</a>` : ''}
                    <p class="${compacta ? 'line-clamp-2' : ''} mt-1 text-body-md"><span class="text-dark-secondary">Pregunta:</span> ${escapeHtml(c.pregunta)}</p>
                </div>
                ${badge(ESTADO_CONSULTA, c.estado)}
            </header>
            ${cuerpo}
        </article>`;
    }

    /** Activa la calificación por estrellas dentro de un contenedor. */
    function activarCalificacion(contenedor) {
        contenedor.addEventListener('click', async (e) => {
            const btn = e.target.closest('[data-puntos]');
            if (!btn) return;
            const grupo = btn.closest('[data-calificar]');
            const respuestaId = grupo.dataset.calificar;
            const puntuacion = Number(btn.dataset.puntos);
            const comentario = contenedor.querySelector(`[data-comentario="${respuestaId}"]`)?.value || null;
            try {
                const cal = await api.put(`/asistente/respuestas/${respuestaId}/calificacion`, { puntuacion, comentario });
                grupo.querySelectorAll('[data-puntos]').forEach(b => {
                    const n = Number(b.dataset.puntos);
                    b.classList.toggle('activa', n <= cal.puntuacion);
                    b.setAttribute('aria-checked', String(n === cal.puntuacion));
                });
                const estado = contenedor.querySelector(`[data-cal-estado="${respuestaId}"]`);
                if (estado) estado.textContent = `Calificada ${fmtFecha(cal.fecha)}`;
                toast(`Calificación guardada: ${cal.puntuacion}/5`);
            } catch (err) {
                toast(err.message, 'error');
            }
        });
    }

    globalThis.AegisConsulta = { tarjeta, activarCalificacion };
})();
