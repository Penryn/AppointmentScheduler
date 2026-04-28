document.addEventListener("DOMContentLoaded", function () {
    var calendarEl = document.getElementById("calendar");
    if (!calendarEl || !window.FullCalendar) {
        return;
    }

    var eventsUrl = calendarEl.dataset.eventsUrl;
    if (!eventsUrl) {
        return;
    }

    var calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: "timeGridWeek",
        headerToolbar: {
            left: "prev,next today",
            center: "title",
            right: "dayGridMonth,timeGridWeek"
        },
        buttonText: {
            today: "今天",
            month: "月",
            week: "周"
        },
        locale: "zh-cn",
        allDaySlot: false,
        slotMinTime: "06:00:00",
        slotMaxTime: "22:00:00",
        firstDay: 1,
        height: "auto",
        eventTimeFormat: {
            hour: "2-digit",
            minute: "2-digit",
            hour12: false
        },
        events: eventsUrl,
        eventClick: function (info) {
            if (!info.event.url) {
                return;
            }
            info.jsEvent.preventDefault();
            window.location.assign(info.event.url);
        }
    });

    calendar.render();
});
