/**
 * AegisOps — Gestión de incidentes (RF01).
 */
(() => {
    const { api, escapeHtml, fmtFecha, fmtNum, badge, toast, mostrarErroresFormulario, cargarOpciones, PRIORIDAD } = globalThis.Aegis;

    const TAMANO = 15;
    let pagina = 0;
    let totalPaginas = 0;
    let cache = [];

    const filtros = document.getElementById('filtros');
    const filas = document.getElementById('filas');
    const modal = document.getElementById('modal');
    const formInc = document.getElementById('form-incidente');

    const ESTADOS = [['ABIERTO', 'Abierto'], ['EN_PROGRESO', 'En progreso'], ['RESUELTO', 'Resuelto'], ['CERRADO', 'Cerrado']];

    async function cargar() {
        const params = { ...Object.fromEntries(new FormData(filtros).entries()), pagina, tamano: TAMANO };
        filas.innerHTML = '<tr><td colspan="9" class="text-dark-secondary">Cargando…</td></tr>';
        try {
            const p = await api.get('/incidentes', params);
            cache = p.contenido;
            totalPaginas = p.totalPaginas;
            document.getElementById('total').textContent = `${fmtNum(p.totalElementos, 0)} incidentes`;
            document.getElementById('pagina').textContent = p.totalPaginas ? `${p.pagina + 1} / ${p.totalPaginas}` : '0 / 0';
            document.getElementById('btn-prev').disabled = p.pagina <= 0;
            document.getElementById('btn-next').disabled = p.pagina + 1 >= p.totalPaginas;
            pintar();
        } catch (e) {
            filas.innerHTML = `<tr><td colspan="9" class="text-severity-p1">${escapeHtml(e.message)}</td></tr>`;
        }
    }

    function pintar() {
        if (cache.length === 0) {
            filas.innerHTML = '<tr><td colspan="9" class="text-dark-secondary">No hay incidentes con estos filtros.</td></tr>';
            return;
        }
        filas.innerHTML = cache.map(i => `
            <tr>
                <td class="font-code text-dark-secondary">${i.id}</td>
                <td class="min-w-[240px]">
                    <div class="font-semibold">${escapeHtml(i.titulo)}</div>
                    <div class="text-body-sm text-dark-secondary line-clamp-2">${escapeHtml(i.descripcion)}</div>
                </td>
                <td>${escapeHtml(i.categoria)}</td>
                <td>${badge(PRIORIDAD, i.prioridad)}</td>
                <td>
                    <label class="sr-only" for="estado-${i.id}">Estado del incidente ${i.id}</label>
                    <select id="estado-${i.id}" data-estado="${i.id}" class="campo !py-1 !text-body-sm min-w-[130px]">
                        ${ESTADOS.map(([v, t]) => `<option value="${v}" ${v === i.estado ? 'selected' : ''}>${t}</option>`).join('')}
                    </select>
                </td>
                <td class="whitespace-nowrap">${escapeHtml(i.usuario)}</td>
                <td class="whitespace-nowrap text-body-sm">${fmtFecha(i.fechaCreacion)}</td>
                <td class="num">${i.horasResolucion == null ? '—' : fmtNum(i.horasResolucion, 1)}</td>
                <td class="whitespace-nowrap">
                    <a class="btn btn-ia !px-2 !py-1" href="asistente.html?incidenteId=${i.id}" title="Consultar asistente RAG" aria-label="Consultar asistente para el incidente ${i.id}">
                        <span class="material-symbols-outlined text-[18px]">psychology</span></a>
                    <button type="button" class="btn btn-secundario !px-2 !py-1" data-editar="${i.id}" title="Editar" aria-label="Editar incidente ${i.id}">
                        <span class="material-symbols-outlined text-[18px]">edit</span></button>
                </td>
            </tr>`).join('');
    }

    filas.addEventListener('change', async (e) => {
        const id = e.target.dataset.estado;
        if (!id) return;
        try {
            const actualizado = await api.patch(`/incidentes/${id}/estado`, { estado: e.target.value });
            toast(`Incidente ${id}: estado ${actualizado.estado.replace('_', ' ').toLowerCase()}`);
            cargar();
        } catch (err) {
            toast(err.message, 'error');
            cargar();
        }
    });

    filas.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-editar]');
        if (!btn) return;
        abrirModal(cache.find(i => String(i.id) === btn.dataset.editar));
    });

    function abrirModal(inc) {
        formInc.reset();
        mostrarErroresFormulario(formInc, {});
        document.getElementById('modal-titulo').textContent = inc ? `Editar incidente #${inc.id}` : 'Nuevo incidente';
        formInc.id.value = inc?.id ?? '';
        if (inc) {
            formInc.titulo.value = inc.titulo;
            formInc.descripcion.value = inc.descripcion;
            formInc.categoriaId.value = inc.categoriaId;
            formInc.prioridad.value = inc.prioridad;
            formInc.usuarioId.value = inc.usuarioId;
        } else {
            const sesion = globalThis.AegisSesion.leer();
            if (sesion) formInc.usuarioId.value = sesion.id;
        }
        modal.showModal();
        formInc.titulo.focus();
    }

    formInc.addEventListener('submit', async (e) => {
        e.preventDefault();
        const datos = Object.fromEntries(new FormData(formInc).entries());
        const body = {
            titulo: datos.titulo.trim(),
            descripcion: datos.descripcion.trim(),
            categoriaId: datos.categoriaId ? Number(datos.categoriaId) : null,
            prioridad: datos.prioridad,
            usuarioId: datos.usuarioId ? Number(datos.usuarioId) : null,
        };
        const btn = document.getElementById('btn-guardar');
        btn.disabled = true;
        try {
            const r = datos.id ? await api.put(`/incidentes/${datos.id}`, body) : await api.post('/incidentes', body);
            modal.close();
            toast(datos.id ? `Incidente #${r.id} actualizado` : `Incidente #${r.id} registrado`);
            if (!datos.id) pagina = 0;
            cargar();
        } catch (err) {
            mostrarErroresFormulario(formInc, err);
            toast(err.message, 'error');
        } finally {
            btn.disabled = false;
        }
    });

    modal.addEventListener('click', (e) => {
        if (e.target.closest('[data-cerrar]')) modal.close();
    });

    filtros.addEventListener('submit', (e) => { e.preventDefault(); pagina = 0; cargar(); });
    document.getElementById('btn-limpiar').addEventListener('click', () => { filtros.reset(); pagina = 0; cargar(); });
    document.getElementById('btn-prev').addEventListener('click', () => { if (pagina > 0) { pagina--; cargar(); } });
    document.getElementById('btn-next').addEventListener('click', () => { if (pagina + 1 < totalPaginas) { pagina++; cargar(); } });
    document.getElementById('btn-nuevo').addEventListener('click', () => abrirModal(null));

    Promise.all([
        cargarOpciones(document.getElementById('f-categoria'), '/categorias'),
        cargarOpciones(document.getElementById('f-usuario'), '/usuarios', { vacio: 'Todos' }),
        cargarOpciones(document.getElementById('i-categoria'), '/categorias', { vacio: '' }),
        cargarOpciones(document.getElementById('i-usuario'), '/usuarios', { vacio: '' }),
    ]).catch(e => toast(e.message, 'error')).finally(() => {
        const params = new URLSearchParams(location.search);
        if (params.get('categoriaId')) document.getElementById('f-categoria').value = params.get('categoriaId');
        cargar();
    });
})();
