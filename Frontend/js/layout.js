/**
 * AegisOps — Estructura común de las pantallas internas: sesión, barra lateral y encabezado.
 * Cada página interna declara <body data-pagina="..."> y un <main id="contenido">.
 */
(() => {
    const SESION_KEY = 'aegisops.sesion';

    const NAV = [
        { id: 'dashboard', href: 'dashboard.html', icono: 'insights', texto: 'Dashboard' },
        { id: 'incidentes', href: 'incidentes.html', icono: 'emergency', texto: 'Incidentes' },
        { id: 'asistente', href: 'asistente.html', icono: 'psychology', texto: 'Asistente RAG' },
        { id: 'historial', href: 'historial.html', icono: 'history', texto: 'Historial IA' },
        { id: 'conocimiento', href: 'conocimiento.html', icono: 'menu_book', texto: 'Conocimiento' },
    ];

    function leerSesion() {
        try {
            return JSON.parse(localStorage.getItem(SESION_KEY));
        } catch {
            return null;
        }
    }

    function guardarSesion(usuario) {
        try {
            localStorage.setItem(SESION_KEY, JSON.stringify(usuario));
        } catch {
            /* almacenamiento no disponible: la sesión dura lo que dure la página */
        }
    }

    function cerrarSesion() {
        try { localStorage.removeItem(SESION_KEY); } catch { /* sin almacenamiento */ }
        location.href = 'login.html';
    }

    function montar() {
        const pagina = document.body.dataset.pagina;
        if (!pagina) return;
        const usuario = leerSesion();
        if (!usuario) {
            location.href = 'login.html';
            return;
        }
        const e = globalThis.Aegis.escapeHtml;
        const iniciales = usuario.nombre.split(' ').map(p => p[0]).slice(0, 2).join('');

        const nav = NAV.map(n => {
            const activo = n.id === pagina;
            return `<a href="${n.href}" class="flex items-center gap-3 px-3 py-2 rounded text-label-lg transition-colors ${activo
                ? 'bg-accent/15 text-accent border border-accent/30'
                : 'text-dark-secondary hover:text-dark-on-surface hover:bg-dark-surface-container border border-transparent'}"
                ${activo ? 'aria-current="page"' : ''}>
                <span class="material-symbols-outlined text-[20px]">${n.icono}</span><span>${n.texto}</span></a>`;
        }).join('');

        const shell = document.createElement('div');
        shell.innerHTML = `
        <aside id="sidebar" class="fixed inset-y-0 left-0 z-40 w-60 bg-dark-surface-container-low border-r border-dark-outline flex flex-col -translate-x-full lg:translate-x-0 transition-transform">
            <a href="index.html" class="flex items-center gap-3 px-5 h-16 border-b border-dark-outline">
                <div class="w-9 h-9 rounded-md bg-accent/20 border border-accent/40 text-accent flex items-center justify-center">
                    <span class="material-symbols-outlined text-[20px]">shield</span>
                </div>
                <span class="font-headline text-title-lg font-bold tracking-tight">Aegis<span class="text-accent">Ops</span></span>
            </a>
            <nav class="flex-1 p-3 space-y-1" aria-label="Principal">${nav}</nav>
            <div class="p-3 border-t border-dark-outline">
                <div class="flex items-center gap-3 px-2 py-2">
                    <div class="w-9 h-9 rounded-full bg-rag-ai/20 border border-rag-ai/40 text-rag-ai flex items-center justify-center font-bold text-label-md">${e(iniciales)}</div>
                    <div class="min-w-0">
                        <div class="text-label-lg truncate">${e(usuario.nombre)}</div>
                        <div class="text-body-sm text-dark-secondary truncate">${e(usuario.area)}</div>
                    </div>
                </div>
                <button id="btn-logout" type="button" class="mt-2 w-full flex items-center justify-center gap-2 px-3 py-2 rounded border border-dark-outline text-label-md text-dark-secondary hover:text-dark-on-surface hover:bg-dark-surface-container">
                    <span class="material-symbols-outlined text-[18px]">logout</span>Cerrar sesión
                </button>
            </div>
        </aside>
        <header class="lg:hidden sticky top-0 z-30 h-14 bg-dark-surface-container-low/95 backdrop-blur border-b border-dark-outline flex items-center justify-between px-4">
            <button id="btn-menu" type="button" class="p-2 -ml-2 text-dark-secondary hover:text-dark-on-surface" aria-label="Abrir menú">
                <span class="material-symbols-outlined">menu</span>
            </button>
            <span class="font-headline font-bold">Aegis<span class="text-accent">Ops</span></span>
            <span class="w-8"></span>
        </header>`;
        document.body.prepend(...shell.children);

        const main = document.getElementById('contenido');
        main.classList.add('lg:pl-60');

        document.getElementById('btn-logout').addEventListener('click', cerrarSesion);
        const sidebar = document.getElementById('sidebar');
        document.getElementById('btn-menu').addEventListener('click', () => sidebar.classList.toggle('-translate-x-full'));
    }

    globalThis.AegisSesion = { leer: leerSesion, guardar: guardarSesion, cerrar: cerrarSesion };
    document.addEventListener('DOMContentLoaded', montar);
})();
