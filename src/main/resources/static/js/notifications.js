/**
 * ShippingGo - Notification System
 * Handles dropdown positioning, polling, and notification sounds
 */
(function () {
    'use strict';

    // ==================== Sound Setup ====================
    let audioCtx = null;

    function playNotificationSound() {
        try {
            if (!audioCtx) {
                audioCtx = new (window.AudioContext || window.webkitAudioContext)();
            }

            var oscillator1 = audioCtx.createOscillator();
            var oscillator2 = audioCtx.createOscillator();
            var gainNode = audioCtx.createGain();

            oscillator1.connect(gainNode);
            oscillator2.connect(gainNode);
            gainNode.connect(audioCtx.destination);

            oscillator1.frequency.setValueAtTime(880, audioCtx.currentTime);
            oscillator1.type = 'sine';
            oscillator2.frequency.setValueAtTime(1108.73, audioCtx.currentTime);
            oscillator2.type = 'sine';

            gainNode.gain.setValueAtTime(0, audioCtx.currentTime);
            gainNode.gain.linearRampToValueAtTime(0.15, audioCtx.currentTime + 0.05);
            gainNode.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.5);

            oscillator1.start(audioCtx.currentTime);
            oscillator2.start(audioCtx.currentTime + 0.1);
            oscillator1.stop(audioCtx.currentTime + 0.5);
            oscillator2.stop(audioCtx.currentTime + 0.6);
        } catch (e) {
            // Audio not supported or blocked
        }
    }

    // ==================== State ====================
    var lastUnreadCount = -1;
    var dropdownOpen = false;
    var dropdownLoaded = false;
    var csrfToken = null;
    var csrfHeader = null;

    function getCsrfInfo() {
        var metaToken = document.querySelector('meta[name="_csrf"]');
        var metaHeader = document.querySelector('meta[name="_csrf_header"]');
        if (metaToken && metaHeader) {
            csrfToken = metaToken.getAttribute('content');
            csrfHeader = metaHeader.getAttribute('content');
            return;
        }
        var hiddenInput = document.querySelector('input[name="_csrf"]');
        if (hiddenInput) {
            csrfToken = hiddenInput.value;
            csrfHeader = 'X-CSRF-TOKEN';
        }
    }

    // ==================== Dropdown Positioning ====================
    function positionDropdown() {
        var btn = document.getElementById('notificationBellBtn');
        var dropdown = document.getElementById('notificationDropdown');
        if (!btn || !dropdown) return;

        var rect = btn.getBoundingClientRect();
        var dropdownWidth = 340;
        var isRtl = document.documentElement.getAttribute('dir') === 'rtl' ||
            document.body.style.direction === 'rtl' ||
            getComputedStyle(document.body).direction === 'rtl';

        // Position below the bell button
        var top = rect.bottom + 8;

        if (isRtl) {
            // RTL: Dropdown opens to the LEFT of the bell (towards main content)
            // Right edge aligns with bell's right edge
            var right = window.innerWidth - rect.right;
            dropdown.style.top = top + 'px';
            dropdown.style.right = right + 'px';
            dropdown.style.left = 'auto';
        } else {
            // LTR: Dropdown opens to the RIGHT of the bell (towards main content)
            // Left edge aligns with bell's left edge
            var left = rect.left;
            dropdown.style.top = top + 'px';
            dropdown.style.left = left + 'px';
            dropdown.style.right = 'auto';
        }

        // Ensure dropdown doesn't go off-screen horizontally
        var dropdownRect = dropdown.getBoundingClientRect();
        if (dropdownRect.left < 8) {
            dropdown.style.left = '8px';
            dropdown.style.right = 'auto';
        }
        if (dropdownRect.right > window.innerWidth - 8) {
            dropdown.style.right = '8px';
            dropdown.style.left = 'auto';
        }

        // Ensure dropdown doesn't go off-screen vertically
        if (top + 480 > window.innerHeight) {
            dropdown.style.maxHeight = (window.innerHeight - top - 16) + 'px';
        }
    }

    // ==================== Dropdown Toggle ====================
    window.toggleNotificationDropdown = function (event) {
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }

        var dropdown = document.getElementById('notificationDropdown');
        if (!dropdown) return;

        dropdownOpen = !dropdownOpen;

        if (dropdownOpen) {
            dropdown.classList.add('open');
            positionDropdown();
            if (!dropdownLoaded) {
                loadRecentNotifications();
            }
        } else {
            dropdown.classList.remove('open');
        }
    };

    // Close dropdown when clicking anywhere outside
    document.addEventListener('click', function (event) {
        if (!dropdownOpen) return;

        var wrapper = document.getElementById('notificationBellWrapper');
        var dropdown = document.getElementById('notificationDropdown');

        if (wrapper && !wrapper.contains(event.target) &&
            dropdown && !dropdown.contains(event.target)) {
            dropdown.classList.remove('open');
            dropdownOpen = false;
        }
    });

    // Reposition on window resize
    window.addEventListener('resize', function () {
        if (dropdownOpen) {
            positionDropdown();
        }
    });

    // ==================== API Calls ====================
    function loadRecentNotifications() {
        var body = document.getElementById('notificationDropdownBody');
        if (!body) return;

        body.innerHTML = '<div class="notification-loading"><span>جاري التحميل...</span></div>';

        fetch('/api/v1/notifications?page=0&size=10', {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        })
            .then(function (response) { return response.json(); })
            .then(function (result) {
                if (result.success && result.data) {
                    renderDropdownNotifications(result.data.notifications || []);
                    dropdownLoaded = true;
                } else {
                    body.innerHTML = '<div class="notification-empty-dropdown">حدث خطأ في تحميل الإشعارات</div>';
                }
            })
            .catch(function () {
                body.innerHTML = '<div class="notification-empty-dropdown">حدث خطأ في الاتصال</div>';
            });
    }

    function renderDropdownNotifications(notifications) {
        var body = document.getElementById('notificationDropdownBody');
        if (!body) return;

        if (notifications.length === 0) {
            body.innerHTML = '<div class="notification-empty-dropdown">🔕 لا توجد إشعارات</div>';
            return;
        }

        var html = '';
        for (var i = 0; i < notifications.length; i++) {
            var notif = notifications[i];
            var icon = getNotificationIcon(notif.type);
            var unreadClass = notif.read ? '' : ' dropdown-item-unread';
            var linkUrl = notif.linkUrl ? '/dashboard/notifications/go/' + notif.id : '/dashboard/notifications';

            html += '<a href="' + linkUrl + '" class="dropdown-notification-item' + unreadClass + '">' +
                '<span class="dropdown-notif-icon">' + icon + '</span>' +
                '<div class="dropdown-notif-content">' +
                '<div class="dropdown-notif-title">' + escapeHtml(notif.title) + '</div>' +
                '<div class="dropdown-notif-body">' + escapeHtml(notif.body) + '</div>' +
                '<div class="dropdown-notif-time">' + escapeHtml(notif.timeAgo || '') + '</div>' +
                '</div>' +
                (notif.read ? '' : '<span class="dropdown-unread-dot"></span>') +
                '</a>';
        }

        body.innerHTML = html;
    }

    function getNotificationIcon(type) {
        switch (type) {
            case 'STATUS_UPDATE': return '📦';
            case 'ORG_ASSIGNMENT': case 'BULK_ORG_ASSIGNMENT': return '🏢';
            case 'ASSIGNMENT': case 'BULK_ASSIGNMENT': return '🚚';
            case 'UNASSIGNMENT': return '❌';
            case 'CLIENT_ORDER': case 'WEBHOOK_ORDER': return '📥';
            case 'WORK_INVITATION': case 'CLIENT_INVITATION': return '📩';
            case 'INVITATION_RESPONSE': return '✉️';
            default: return '🔔';
        }
    }

    window.markAllNotificationsRead = function () {
        getCsrfInfo();
        var headers = { 'Accept': 'application/json' };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch('/api/v1/notifications/read-all', {
            method: 'PUT',
            credentials: 'same-origin',
            headers: headers
        })
            .then(function (response) { return response.json(); })
            .then(function () {
                updateBadge(0);
                if (dropdownOpen) {
                    loadRecentNotifications();
                }
            })
            .catch(function () { });
    };

    // ==================== Badge & Polling ====================
    function updateBadge(count) {
        var badge = document.getElementById('notificationBadge');

        if (count > 0) {
            if (!badge) {
                var btn = document.getElementById('notificationBellBtn');
                if (btn) {
                    badge = document.createElement('span');
                    badge.className = 'notification-badge';
                    badge.id = 'notificationBadge';
                    btn.appendChild(badge);
                }
            }
            if (badge) {
                badge.textContent = count > 99 ? '99+' : count;
                badge.style.display = '';
            }
        } else {
            if (badge) {
                badge.style.display = 'none';
            }
        }
    }

    function pollUnreadCount() {
        fetch('/api/v1/notifications/unread-count', {
            credentials: 'same-origin',
            headers: { 'Accept': 'application/json' }
        })
            .then(function (response) { return response.json(); })
            .then(function (result) {
                if (result.success && result.data) {
                    var newCount = result.data.unreadCount || 0;

                    if (lastUnreadCount >= 0 && newCount > lastUnreadCount) {
                        playNotificationSound();
                        dropdownLoaded = false;
                        if (dropdownOpen) {
                            loadRecentNotifications();
                        }
                    }

                    lastUnreadCount = newCount;
                    updateBadge(newCount);
                }
            })
            .catch(function () { });
    }

    // ==================== Utility ====================
    function escapeHtml(text) {
        if (!text) return '';
        var div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // ==================== Initialize ====================
    function init() {
        getCsrfInfo();
        pollUnreadCount();
        setInterval(pollUnreadCount, 30000);

        var badge = document.getElementById('notificationBadge');
        if (badge) {
            var count = parseInt(badge.textContent) || 0;
            lastUnreadCount = count;
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
