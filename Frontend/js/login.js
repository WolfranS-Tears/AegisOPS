/**
 * AegisOps — Lógica interactiva de la pantalla de Login
 */
(() => {
    const DASHBOARD_URL = '../aegis-analytics/DashboardAnalytics.html';
    const REDIRECT_DELAY_MS = 500;

    const form = document.getElementById('login-form');
    const pwdInput = document.getElementById('login-password');
    const toggleBtn = document.getElementById('toggle-password');
    const btnSubmit = document.getElementById('btn-submit-login');

  // Mostrar / ocultar contraseña
    toggleBtn.addEventListener('click', () => {
        const isHidden = pwdInput.type === 'password';
    pwdInput.type = isHidden ? 'text' : 'password';
    toggleBtn.innerHTML = `<span class="material-symbols-outlined text-[18px]">${
        isHidden ? 'visibility_off' : 'visibility'
    }</span>`;
    toggleBtn.title = isHidden ? 'Ocultar contraseña' : 'Mostrar contraseña';
    });

  // Envío del formulario (simulación de autenticación)
    form.addEventListener('submit', (e) => {
        e.preventDefault();

    btnSubmit.innerHTML = `
        <span class="material-symbols-outlined text-[18px] animate-spin">progress_activity</span>
        <span>Autenticando credenciales...</span>
    `;
    btnSubmit.classList.add('opacity-80', 'pointer-events-none');

        setTimeout(() => {
            window.location.href = DASHBOARD_URL;
        }, REDIRECT_DELAY_MS);
    });
})();