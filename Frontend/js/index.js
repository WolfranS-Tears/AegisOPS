/**
 * AegisOps — Configuración de Tailwind CSS (CDN)
 * Debe cargarse DESPUÉS del script https://cdn.tailwindcss.com
 * @type {import('tailwindcss').Config}
 */
globalThis.tailwind = globalThis.tailwind || {};
globalThis.tailwind.config = {
    darkMode: 'class',
theme: {
    extend: {
        colors: {
            'accent': '#0EA5E9',
            'rag-ai': '#8B5CF6',
            'severity-p1': '#EF4444',
            'severity-p2': '#F59E0B',
            'severity-p3': '#10B981',
            'severity-p4': '#64748B',
            'success': '#10B981',
            'warning': '#F59E0B',
            'error': '#EF4444',
            'info': '#0EA5E9',
            'light-primary': '#0F172A',
            'light-on-primary': '#FFFFFF',
            'light-surface': '#F8FAFC',
            'light-on-surface': '#0F172A',
            'dark-primary': '#0EA5E9',
            'dark-on-primary': '#030712',
            'dark-primary-container': '#111827',
            'dark-on-primary-container': '#E0F2FE',
            'dark-secondary': '#94A3B8',
            'dark-on-secondary': '#030712',
            'dark-surface': '#0B0F19',
            'dark-on-surface': '#F8FAFC',
            'dark-surface-container-low': '#111827',
            'dark-surface-container': '#1E293B',
            'dark-surface-container-high': '#334155',
            'dark-outline': '#1E293B',
            'dark-outline-variant': '#334155',
    },
        borderRadius: {
            'sm': '4px',
            'DEFAULT': '8px',
            'md': '12px',
            'lg': '16px',
            'xl': '20px',
            'full': '9999px',
    },
        spacing: {
            'gutter-xs': '4px',
            'gutter-sm': '8px',
            'gutter-md': '16px',
            'gutter-lg': '24px',
            'gutter-xl': '32px',
            'section': '40px',
            'page-margin': '24px',
    },
        fontFamily: {
            'display': ['"Plus Jakarta Sans"', 'sans-serif'],
            'headline': ['"Plus Jakarta Sans"', 'sans-serif'],
            'title': ['"Plus Jakarta Sans"', 'sans-serif'],
            'body': ['"Plus Jakarta Sans"', 'sans-serif'],
            'label': ['"Plus Jakarta Sans"', 'sans-serif'],
            'code': ['"JetBrains Mono"', 'monospace'],
    },
        fontSize: {
            'display-lg': ['36px', { lineHeight: '44px', letterSpacing: '-0.5px', fontWeight: '700' }],
            'display-lg-mobile': ['28px', { lineHeight: '36px', letterSpacing: '-0.25px', fontWeight: '700' }],
            'headline-xl': ['32px', { lineHeight: '40px', letterSpacing: '-0.25px', fontWeight: '700' }],
            'headline-xl-mobile': ['24px', { lineHeight: '32px', fontWeight: '700' }],
            'headline-lg': ['24px', { lineHeight: '32px', letterSpacing: '-0.2px', fontWeight: '600' }],
            'headline-lg-mobile': ['20px', { lineHeight: '28px', fontWeight: '600' }],
            'headline-md': ['20px', { lineHeight: '28px', fontWeight: '600' }],
            'title-lg': ['18px', { lineHeight: '24px', fontWeight: '600' }],
            'title-md': ['16px', { lineHeight: '22px', fontWeight: '600' }],
            'body-lg': ['15px', { lineHeight: '22px', fontWeight: '400' }],
            'body-md': ['14px', { lineHeight: '20px', fontWeight: '400' }],
            'body-sm': ['12px', { lineHeight: '16px', fontWeight: '400' }],
            'label-lg': ['14px', { lineHeight: '20px', fontWeight: '600' }],
            'label-md': ['12px', { lineHeight: '16px', fontWeight: '600' }],
            'label-sm': ['11px', { lineHeight: '14px', letterSpacing: '0.2px', fontWeight: '600' }],
            'code-md': ['13px', { lineHeight: '18px', fontWeight: '500' }],
            'code-sm': ['11px', { lineHeight: '15px', fontWeight: '500' }],
    },
        transitionDuration: {
            'state-change': '120ms',
            'toast-fade': '180ms',
    },
        transitionDelay: {
            'state-change': '0ms',
            'toast-fade': '0ms',
    },
        transitionTimingFunction: {
            'state-change': 'cubic-bezier(0, 0, 0.2, 1)',
            'toast-fade': 'ease-out',
    },
},
},
};

if (typeof module !== 'undefined') module.exports = globalThis.tailwind.config;