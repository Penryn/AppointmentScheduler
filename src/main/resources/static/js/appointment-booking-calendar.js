document.addEventListener("DOMContentLoaded", function () {
    var calendarEl = document.getElementById("calendar");
    if (!calendarEl || !window.FullCalendar) {
        return;
    }

    var availabilityBaseUrl = calendarEl.dataset.availabilityBaseUrl;
    var bookingBaseUrl = calendarEl.dataset.bookingBaseUrl;
    if (!availabilityBaseUrl || !bookingBaseUrl) {
        return;
    }

    var firstBookableDate = new Date();
    firstBookableDate.setDate(firstBookableDate.getDate() + 1);
    firstBookableDate.setHours(0, 0, 0, 0);

    var calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: "listDay",
        initialDate: firstBookableDate,
        validRange: {
            start: firstBookableDate
        },
        headerToolbar: {
            left: "title",
            center: "",
            right: "prev,next"
        },
        buttonText: {
            listDay: "可预约时间"
        },
        locale: "zh-cn",
        firstDay: 1,
        height: "auto",
        noEventsContent: "当天暂无可预约时间",
        eventTimeFormat: {
            hour: "2-digit",
            minute: "2-digit",
            hour12: false
        },
        events: function (fetchInfo, successCallback, failureCallback) {
            var requestedDate = fetchInfo.startStr.slice(0, 10);
            fetch(availabilityBaseUrl + requestedDate, {
                headers: {
                    "X-Requested-With": "XMLHttpRequest"
                }
            })
                .then(function (response) {
                    if (!response.ok) {
                        throw new Error("可预约时间请求失败，状态码：" + response.status);
                    }
                    return response.json();
                })
                .then(function (entries) {
                    successCallback(entries.map(function (entry) {
                        return {
                            title: "可预约",
                            start: entry.start,
                            end: entry.end,
                            url: bookingBaseUrl + encodeURIComponent(entry.start)
                        };
                    }));
                })
                .catch(failureCallback);
        },
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
