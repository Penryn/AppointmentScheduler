document.addEventListener('DOMContentLoaded', function () {
    var notificationIndicator = document.getElementById('notifications-count');
    if (!notificationIndicator) {
        return;
    }

    var pollDelayMs = 30000;
    var pollHandle = null;

    function updateIndicator(count) {
        notificationIndicator.setAttribute('data-count', count);
        notificationIndicator.className = count > 0 ? 'notification-bell notification-icon' : 'notification-bell';
    }

    function scheduleNextPoll() {
        window.clearTimeout(pollHandle);
        if (document.hidden) {
            return;
        }
        pollHandle = window.setTimeout(fetchUnreadNotifications, pollDelayMs);
    }

    function fetchUnreadNotifications() {
        if (document.hidden) {
            return;
        }

        fetch('/api/user/notifications', {
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('Notification polling failed with status ' + response.status);
                }
                return response.text();
            })
            .then(function (body) {
                updateIndicator(Number(body) || 0);
            })
            .catch(function () {
                updateIndicator(0);
            })
            .finally(scheduleNextPoll);
    }

    document.addEventListener('visibilitychange', function () {
        if (document.hidden) {
            window.clearTimeout(pollHandle);
            return;
        }
        fetchUnreadNotifications();
    });

    fetchUnreadNotifications();
});
