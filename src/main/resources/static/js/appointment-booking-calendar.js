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

    function localizeListLabels() {
        calendarEl.querySelectorAll("*").forEach(function (element) {
            if (element.getAttribute("aria-label") === "Previous day" || element.getAttribute("title") === "Previous day") {
                element.setAttribute("aria-label", "前一天");
                element.setAttribute("title", "前一天");
            } else if (element.getAttribute("aria-label") === "Next day" || element.getAttribute("title") === "Next day") {
                element.setAttribute("aria-label", "后一天");
                element.setAttribute("title", "后一天");
            }

            if (element.children.length > 0) {
                return;
            }

            var text = element.textContent.trim();
            if (text === "Time") {
                element.textContent = "时间";
            } else if (text === "Event") {
                element.textContent = "状态";
            }
        });
    }

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
        eventDidMount: function () {
            window.setTimeout(localizeListLabels, 0);
        },
        datesSet: function () {
            window.setTimeout(localizeListLabels, 0);
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
                    window.setTimeout(localizeListLabels, 0);
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
    localizeListLabels();
    window.setTimeout(localizeListLabels, 100);
});
