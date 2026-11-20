/**
 * AegisOps — Portada: muestra métricas reales del backend (sin cifras inventadas).
 */
(async () => {
    const { api, fmtNum } = globalThis.Aegis;
    const set = (id, v) => { document.getElementById(id).textContent = v; };
    try {
        const [resumen, articulos] = await Promise.all([
            api.get('/analitica/resumen'),
            api.get('/articulos', { estado: 'ACTIVO' }),
        ]);
        set('m-total', fmtNum(resumen.total, 0));
        set('m-resolucion', `${fmtNum(resumen.porcentaje_resolucion)}%`);
        set('m-tiempo', `${fmtNum(resumen.tiempo_promedio_horas, 1)} h`);
        set('m-articulos', fmtNum(articulos.filter(a => a.fuenteValida).length, 0));
    } catch {
        set('m-nota', 'El backend no está disponible: inicie la API para ver las métricas.');
    }
})();
