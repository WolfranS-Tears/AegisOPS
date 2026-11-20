/**
 * AegisOps — Inicio de sesión de demostración.
 * Valida el correo contra la tabla usuario (GET /api/usuarios/por-correo).
 */
(() => {
    const { api, escapeHtml } = globalThis.Aegis;
    const DESTINO = 'dashboard.html';

    const form = document.getElementById('login-form');
    const email = document.getElementById('login-email');
    const error = document.getElementById('login-error');
    const btn = document.getElementById('btn-submit-login');
    const demo = document.getElementById('login-demo');
    const textoBoton = btn.innerHTML;

    if (globalThis.AegisSesion.leer()) {
        location.href = DESTINO;
        return;
    }

    api.get('/usuarios').then(usuarios => {
        demo.innerHTML = '<option value="">Elegir usuario de demostración…</option>' + usuarios
            .map(u => `<option value="${escapeHtml(u.correo)}">${escapeHtml(u.nombre)} — ${escapeHtml(u.area)}</option>`)
            .join('');
    }).catch(e => {
        demo.innerHTML = '<option value="">Backend no disponible</option>';
        error.textContent = e.message;
    });

    demo.addEventListener('change', () => {
        if (demo.value) {
            email.value = demo.value;
            error.textContent = '';
        }
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        error.textContent = '';
        const correo = email.value.trim();
        if (!correo || !email.checkValidity()) {
            error.textContent = 'Ingrese un correo válido.';
            email.focus();
            return;
        }
        btn.disabled = true;
        btn.innerHTML = '<span class="material-symbols-outlined text-[18px] animate-spin">progress_activity</span><span>Verificando…</span>';
        try {
            const usuario = await api.get('/usuarios/por-correo', { correo });
            globalThis.AegisSesion.guardar(usuario);
            location.href = DESTINO;
        } catch (err) {
            error.textContent = err.status === 404 ? 'No existe un usuario con ese correo.' : err.message;
            btn.disabled = false;
            btn.innerHTML = textoBoton;
        }
    });
})();
