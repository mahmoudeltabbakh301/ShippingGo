/**
 * Preferences JS for ShippingGo
 * Handles theme toggle logic. Language uses Spring Boot LocaleInterceptor (query param ?lang=en).
 */

document.addEventListener('DOMContentLoaded', function() {
    initTheme();
});

function initTheme() {
    const themeBtn = document.getElementById('theme-toggle-btn');
    if (themeBtn) {
        // Update button icon initially based on html data-theme
        const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
        updateThemeIcon(themeBtn, isDark);
        
        // Add listener
        themeBtn.addEventListener('click', function(e) {
            e.preventDefault();
            const currentIsDark = document.documentElement.getAttribute('data-theme') === 'dark';
            const newIsDark = !currentIsDark;
            
            if (newIsDark) {
                document.documentElement.setAttribute('data-theme', 'dark');
                localStorage.setItem('theme', 'dark');
            } else {
                document.documentElement.removeAttribute('data-theme');
                localStorage.setItem('theme', 'light');
            }
            
            updateThemeIcon(themeBtn, newIsDark);
        });
    }
}

function updateThemeIcon(btn, isDark) {
    if (isDark) {
        btn.innerHTML = '☀️'; // Sun when it's dark
        btn.title = btn.dataset.langLight || 'Light Mode';
    } else {
        btn.innerHTML = '🌙'; // Moon when it's light
        btn.title = btn.dataset.langDark || 'Dark Mode';
    }
}
