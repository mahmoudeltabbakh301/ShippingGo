/**
 * Sidebar Toggle - Mobile responsive sidebar management
 * Handles open/close, overlay, and auto-close on navigation
 */
(function () {
    'use strict';

    var sidebar = null;
    var overlay = null;

    function getSidebar() {
        if (!sidebar) {
            sidebar = document.querySelector('.sidebar');
        }
        return sidebar;
    }

    // Create overlay element
    function createOverlay() {
        if (overlay) return overlay;
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';
        overlay.addEventListener('click', closeSidebar);
        document.body.appendChild(overlay);
        return overlay;
    }

    // Create close button inside sidebar
    function createCloseButton() {
        var sb = getSidebar();
        if (!sb || sb.querySelector('.sidebar-close-btn')) return;
        var btn = document.createElement('button');
        btn.className = 'sidebar-close-btn';
        btn.innerHTML = '✕';
        btn.setAttribute('aria-label', 'Close sidebar');
        btn.addEventListener('click', closeSidebar);
        sb.insertBefore(btn, sb.firstChild);
    }

    function openSidebar() {
        var sb = getSidebar();
        if (!sb) return;
        createOverlay();
        createCloseButton();
        sb.classList.add('open');
        overlay.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    function closeSidebar() {
        var sb = getSidebar();
        if (!sb) return;
        sb.classList.remove('open');
        if (overlay) {
            overlay.classList.remove('active');
        }
        document.body.style.overflow = '';
    }

    // Global toggle function
    window.toggleSidebar = function () {
        var sb = getSidebar();
        if (!sb) return;
        if (sb.classList.contains('open')) {
            closeSidebar();
        } else {
            openSidebar();
        }
    };

    // Auto-close sidebar when a nav link is clicked (mobile)
    document.addEventListener('DOMContentLoaded', function () {
        var sb = getSidebar();
        if (sb) {
            var navLinks = sb.querySelectorAll('.nav-link');
            for (var i = 0; i < navLinks.length; i++) {
                navLinks[i].addEventListener('click', function () {
                    if (window.innerWidth <= 1024) {
                        closeSidebar();
                    }
                });
            }
        }
    });

    // Close sidebar on window resize to desktop
    window.addEventListener('resize', function () {
        if (window.innerWidth > 1024) {
            closeSidebar();
        }
    });

    // Close sidebar on Escape key
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            closeSidebar();
        }
    });
})();
