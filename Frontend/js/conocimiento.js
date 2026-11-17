/**
 * AegisOps — Base de conocimiento (RF02) y disparo de indexación (RF03).
 */
(() => {
    const { api, escapeHtml, fmtFecha, badge, toast, mostrarErroresFormulario, cargarOpciones, ESTADO_ART } = globalThis.Aegis;

    const form = document.getElementById('filtros');
    const lista = document.getElementById('lista');
    const modal = document.getElementById('modal');
    const formArt = document.getElementById('form-articulo');
    let cache = [];

    async function cargar() {
        lista.innerHTML = '<p class="text-body-sm text-dark-secondary">Cargando…</p>';
        try {
            cache = await api.get('/articulos', Object.fromEntries(new FormData(form).entries()));
            const fuentes = cache.filter(a => a.fuenteValida).length;
            document.getElementById('resumen').textContent = `${cache.length} artículo(s) · ${fuentes} utilizables como fuente`;
            pintar();
        } catch (e) {
            lista.innerHTML = `<p class="text-body-sm text-severity-p1">${escapeHtml(e.message)}</p>`;
        }
    }

    function pintar() {
        if (cache.length === 0) {
            lista.innerHTML = '<p class="text-body-sm text-dark-secondary">No hay artículos con estos filtros.</p>';
            return;
        }
        lista.innerHTML = cache.map(a => `
            <article class="tarjeta p-4 space-y-3 ${a.fuenteValida ? '' : 'opacity-80'}">
                <header class="flex items-start justify-between gap-3">
                    <div class="min-w-0">
                        <div class="text-body-sm text-dark-secondary font-code">#${a.id} · ${escapeHtml(a.categoria)}</div>
                        <h2 class="font-headline text-title-md font-bold">${escapeHtml(a.titulo)}</h2>
                    </div>
                    <div class="flex flex-col items-end gap-1 shrink-0">
                        ${badge(ESTADO_ART, a.estado)}
                        <span class="text-label-sm ${a.verificado ? 'text-success' : 'text-dark-secondary'} flex items-center gap-1">
                            <span class="material-symbols-outlined text-[14px]">${a.verificado ? 'verified' : 'pending'}</span>${a.verificado ? 'Verificado' : 'Sin verificar'}</span>
                    </div>
                </header>
                <dl class="text-body-sm space-y-1">
                    <div><dt class="inline text-dark-secondary">Problema: </dt><dd class="inline">${escapeHtml(a.problema)}</dd></div>
                    <div><dt class="inline text-dark-secondary">Síntomas: </dt><dd class="inline">${escapeHtml(a.sintomas)}</dd></div>
                    <div><dt class="inline text-dark-secondary">Causa: </dt><dd class="inline">${escapeHtml(a.causa)}</dd></div>
                </dl>
                <details class="text-body-sm">
                    <summary class="cursor-pointer text-accent">Procedimiento de solución</summary>
                    <p class="mt-2 whitespace-pre-line">${escapeHtml(a.solucion)}</p>
                </details>
                <footer class="flex flex-wrap items-center justify-between gap-2 pt-1 border-t border-dark-outline">
                    <span class="text-body-sm text-dark-secondary">Actualizado ${fmtFecha(a.fechaActualizacion)}</span>
                    <div class="flex gap-2">
                        ${a.estado === 'ACTIVO'
                            ? `<button type="button" class="btn btn-secundario !py-1" data-estado="INACTIVO" data-id="${a.id}"><span class="material-symbols-outlined text-[16px]">block</span>Desactivar</button>`
                            : `<button type="button" class="btn btn-secundario !py-1" data-estado="ACTIVO" data-id="${a.id}"><span class="material-symbols-outlined text-[16px]">check_circle</span>Activar</button>`}
                        ${a.verificado ? '' : `<button type="button" class="btn btn-secundario !py-1" data-verificar="${a.id}"><span class="material-symbols-outlined text-[16px]">verified</span>Verificar</button>`}
                        <button type="button" class="btn btn-secundario !py-1" data-editar="${a.id}" aria-label="Editar artículo ${a.id}"><span class="material-symbols-outlined text-[16px]">edit</span></button>
                    </div>
                </footer>
            </article>`).join('');
    }

    lista.addEventListener('click', async (e) => {
        const est = e.target.closest('[data-estado]');
        const ver = e.target.closest('[data-verificar]');
        const edi = e.target.closest('[data-editar]');
        try {
            if (est) {
                const a = await api.patch(`/articulos/${est.dataset.id}/estado`, { estado: est.dataset.estado });
                toast(a.fuenteValida ? `Artículo #${a.id} activo: se usará como fuente`
                    : `Artículo #${a.id} ${a.estado.toLowerCase()}: ya no se usará como fuente`);
                cargar();
            } else if (ver) {
                const actual = cache.find(a => String(a.id) === ver.dataset.verificar);
                const a = await api.patch(`/articulos/${actual.id}/estado`, { estado: actual.estado, verificado: true });
                toast(`Artículo #${a.id} verificado`);
                cargar();
            } else if (edi) {
                abrirModal(cache.find(a => String(a.id) === edi.dataset.editar));
            }
        } catch (err) {
            toast(err.message, 'error');
        }
    });

    function abrirModal(a) {
        formArt.reset();
        mostrarErroresFormulario(formArt, {});
        document.getElementById('modal-titulo').textContent = a ? `Editar artículo #${a.id}` : 'Nuevo artículo';
        formArt.id.value = a?.id ?? '';
        if (a) {
            ['titulo', 'problema', 'sintomas', 'causa', 'solucion', 'categoriaId', 'estado'].forEach(k => { formArt[k].value = a[k]; });
            formArt.verificado.checked = a.verificado;
        }
        modal.showModal();
        formArt.titulo.focus();
    }

    formArt.addEventListener('submit', async (e) => {
        e.preventDefault();
        const d = Object.fromEntries(new FormData(formArt).entries());
        const body = {
            titulo: d.titulo.trim(), problema: d.problema.trim(), sintomas: d.sintomas.trim(),
            causa: d.causa.trim(), solucion: d.solucion.trim(),
            categoriaId: d.categoriaId ? Number(d.categoriaId) : null,
            estado: d.estado, verificado: formArt.verificado.checked,
        };
        const btn = document.getElementById('btn-guardar');
        btn.disabled = true;
        try {
            const r = d.id ? await api.put(`/articulos/${d.id}`, body) : await api.post('/articulos', body);
            modal.close();
            toast(`Artículo #${r.id} guardado`);
            cargar();
        } catch (err) {
            mostrarErroresFormulario(formArt, err);
            toast(err.message, 'error');
        } finally {
            btn.disabled = false;
        }
    });

    modal.addEventListener('click', (e) => { if (e.target.closest('[data-cerrar]')) modal.close(); });
    form.addEventListener('submit', (e) => { e.preventDefault(); cargar(); });
    document.getElementById('btn-nuevo').addEventListener('click', () => abrirModal(null));

    document.getElementById('btn-indexar').addEventListener('click', async (e) => {
        const btn = e.currentTarget;
        const texto = btn.innerHTML;
        btn.disabled = true;
        btn.innerHTML = '<span class="material-symbols-outlined text-[18px] spinner">sync</span>Indexando…';
        try {
            const r = await api.post('/articulos/indexar');
            toast(`Indexación completa: ${r.indexados} indexados, ${r.retirados} retirados de Qdrant`);
        } catch (err) {
            toast(err.message, 'error');
        } finally {
            btn.disabled = false;
            btn.innerHTML = texto;
        }
    });

    Promise.all([
        cargarOpciones(document.getElementById('a-categoria'), '/categorias'),
        cargarOpciones(document.getElementById('m-categoria'), '/categorias', { vacio: '' }),
    ]).catch(e => toast(e.message, 'error')).finally(cargar);
})();
