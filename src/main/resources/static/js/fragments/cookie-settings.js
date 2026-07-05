/**
 * Cookie Consent Manager
 * Handles cookie banner display and user preferences via localStorage.
 */
(function () {
    'use strict';

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    function init() {
        const banner = document.getElementById('cookieBanner');
        const modalEl = document.getElementById('cookieSettingsModal');
        const settingsLink = document.getElementById('cookieSettingsLink');

        if (!banner || !modalEl) {
            return;
        }

        const modal = new bootstrap.Modal(modalEl);

        const savedConsent = localStorage.getItem('cookieConsent');
        if (!savedConsent) {
            setTimeout(() => { banner.style.display = 'block'; }, 1000);
        } else {
            applyCookieSettings(JSON.parse(savedConsent));
        }

        const settingsBtn = document.getElementById('cookieSettingsBtn');
        if (settingsBtn) {
            settingsBtn.addEventListener('click', () => modal.show());
        }

        if (settingsLink) {
            settingsLink.addEventListener('click', (e) => {
                e.preventDefault();
                if (banner.style.display === 'none') {
                    banner.style.display = 'block';
                }
                modal.show();
            });
        }

        const acceptAllBtn = document.getElementById('acceptAllCookies');
        if (acceptAllBtn) {
            acceptAllBtn.addEventListener('click', () => {
                const settings = buildSettings(true, true, true);
                saveAndApply(settings);
                banner.style.display = 'none';
            });
        }

        const rejectBtn = document.getElementById('rejectOptionalCookies');
        if (rejectBtn) {
            rejectBtn.addEventListener('click', () => {
                const settings = buildSettings(false, false, false);
                saveAndApply(settings);
                modal.hide();
                banner.style.display = 'none';
            });
        }

        const saveBtn = document.getElementById('saveCookieSettings');
        if (saveBtn) {
            saveBtn.addEventListener('click', () => {
                const settings = buildSettings(
                    isChecked('cookieFunctional'),
                    isChecked('cookieAnalytics'),
                    isChecked('cookieMarketing')
                );
                saveAndApply(settings);
                modal.hide();
                banner.style.display = 'none';
            });
        }
    }


    function isChecked(id) {
        const el = document.getElementById(id);
        return el ? el.checked : false;
    }

    function buildSettings(functional, analytics, marketing) {
        return {
            necessary: true,
            functional: functional,
            analytics: analytics,
            marketing: marketing,
            timestamp: new Date().toISOString()
        };
    }

    function saveAndApply(settings) {
        try {
            localStorage.setItem('cookieConsent', JSON.stringify(settings));
            applyCookieSettings(settings);
            console.log('[Cookie] Settings saved:', settings);
        } catch (e) {
            console.warn('[Cookie] Failed to save settings:', e);
        }
    }

    function applyCookieSettings(settings) {
        if (settings.analytics) {
            console.log('[Cookie] Analytics enabled');
            // initAnalytics();
        }
        if (settings.marketing) {
            console.log('[Cookie] Marketing enabled');
            // initMarketing();
        }
    }
})();