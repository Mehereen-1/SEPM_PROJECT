(function () {
    const POLL_INTERVAL_MS = 30000;

    function notifyToast(message, type) {
        if (window.BiblioToast && typeof window.BiblioToast.show === "function") {
            window.BiblioToast.show(message, type || "info");
        }
    }

    function toRelativeTime(value, epochMillis) {
        if (!value && !Number.isFinite(epochMillis)) {
            return "just now";
        }

        const ts = Number.isFinite(epochMillis) ? new Date(epochMillis) : new Date(value);
        if (Number.isNaN(ts.getTime())) {
            return "just now";
        }

        const diffSec = Math.max(1, Math.floor((Date.now() - ts.getTime()) / 1000));
        if (diffSec < 60) return diffSec + "s ago";
        const diffMin = Math.floor(diffSec / 60);
        if (diffMin < 60) return diffMin + "m ago";
        const diffHour = Math.floor(diffMin / 60);
        if (diffHour < 24) return diffHour + "h ago";
        const diffDay = Math.floor(diffHour / 24);
        return diffDay + "d ago";
    }

    async function fetchJson(url, options) {
        const response = await fetch(url, options || {});
        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }
        return response.json();
    }

    function createDropdownItem(item) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "notif-item" + (item.read ? "" : " unread");
        button.dataset.notificationId = item.id;
        button.innerHTML =
            '<div class="notif-message"></div>' +
            '<div class="notif-meta">' +
            (item.read ? "" : '<span class="notif-dot"></span>') +
            '<span>' + toRelativeTime(item.timestamp, item.timestampEpochMillis) + "</span>" +
            "</div>";
        button.querySelector(".notif-message").textContent = item.message;
        return button;
    }

    function attachNavbarWidget(shell) {
        const bell = shell.querySelector("[data-notification-bell]");
        const badge = shell.querySelector("[data-notification-badge]");
        const dropdown = shell.querySelector("[data-notification-dropdown]");
        const list = shell.querySelector("[data-notification-list]");
        const empty = shell.querySelector("[data-notification-empty]");
        const markAllBtn = shell.querySelector("[data-notification-mark-all]");

        if (!bell || !badge || !dropdown || !list || !empty || !markAllBtn) {
            return;
        }

        const openState = { open: false };
        const summaryState = {
            initialized: false,
            lastUnreadCount: 0,
            latestNotificationId: null
        };

        function renderSummary(summary) {
            const unreadCount = summary.unreadCount || 0;
            badge.textContent = unreadCount > 99 ? "99+" : String(unreadCount);
            badge.style.display = unreadCount > 0 ? "inline-block" : "none";

            list.innerHTML = "";
            const items = summary.recent || [];
            if (!items.length) {
                empty.style.display = "block";
                return;
            }

            empty.style.display = "none";
            items.forEach(function (item) {
                list.appendChild(createDropdownItem(item));
            });
        }

        async function refreshSummary() {
            try {
                const summary = await fetchJson("/notifications/summary?limit=7");
                console.debug("[notifications] summary fetched", summary);

                const unreadCount = summary.unreadCount || 0;
                const latestId = summary.recent && summary.recent.length ? summary.recent[0].id : null;

                if (summaryState.initialized && unreadCount > summaryState.lastUnreadCount && latestId !== summaryState.latestNotificationId) {
                    notifyToast("You have new notifications", "info");
                }

                summaryState.initialized = true;
                summaryState.lastUnreadCount = unreadCount;
                summaryState.latestNotificationId = latestId;
                renderSummary(summary);
            } catch (error) {
                console.error("Failed to fetch notifications summary", error);
            }
        }

        function setDropdown(open) {
            openState.open = open;
            if (open) {
                dropdown.classList.add("open");
            } else {
                dropdown.classList.remove("open");
            }
        }

        bell.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();
            setDropdown(!openState.open);
        });

        document.addEventListener("click", function (event) {
            if (!shell.contains(event.target)) {
                setDropdown(false);
            }
        });

        list.addEventListener("click", async function (event) {
            const target = event.target;
            const button = target.closest("button[data-notification-id]");
            if (!button) {
                return;
            }

            const id = button.dataset.notificationId;
            try {
                await fetchJson("/notifications/" + id, { method: "PATCH" });
            } catch (error) {
                console.error("Failed to mark notification as read", error);
            }
            window.location.href = "/notification-center";
        });

        markAllBtn.addEventListener("click", async function () {
            try {
                await fetchJson("/notifications", { method: "PATCH" });
                console.debug("[notifications] marked all notifications as read");
                await refreshSummary();
                window.dispatchEvent(new CustomEvent("notifications:updated"));
            } catch (error) {
                console.error("Failed to mark all notifications as read", error);
            }
        });

        refreshSummary();
        setInterval(refreshSummary, POLL_INTERVAL_MS);
    }

    function attachNotificationPage() {
        const page = document.querySelector("[data-notification-page]");
        if (!page) {
            return;
        }

        const list = page.querySelector("[data-notification-page-list]");
        const empty = page.querySelector("[data-notification-page-empty]");
        const markAllBtn = page.querySelector("[data-notification-page-mark-all]");
        const allBtn = page.querySelector("[data-notification-filter-all]");
        const unreadBtn = page.querySelector("[data-notification-filter-unread]");

        if (!list || !empty || !markAllBtn || !allBtn || !unreadBtn) {
            return;
        }

        const state = {
            filter: "all",
            items: []
        };

        function render() {
            const filtered = state.filter === "unread"
                ? state.items.filter(function (item) { return !item.read; })
                : state.items;

            list.innerHTML = "";
            if (!filtered.length) {
                empty.style.display = "block";
                return;
            }

            empty.style.display = "none";
            filtered.forEach(function (item) {
                const row = document.createElement("div");
                row.className = "notif-page-item" + (item.read ? "" : " unread");
                row.innerHTML =
                    '<div class="notif-page-content">' +
                    '<p class="notif-message"></p>' +
                    '<div class="notif-meta"><span>' + toRelativeTime(item.timestamp, item.timestampEpochMillis) + "</span></div>" +
                    "</div>" +
                    '<div class="notif-page-actions"></div>';
                row.querySelector(".notif-message").textContent = item.message;

                if (!item.read) {
                    const btn = document.createElement("button");
                    btn.type = "button";
                    btn.className = "notif-mark-btn";
                    btn.textContent = "Mark as read";
                    btn.dataset.notificationId = item.id;
                    row.querySelector(".notif-page-actions").appendChild(btn);
                }

                list.appendChild(row);
            });
        }

        async function loadItems() {
            try {
                state.items = await fetchJson("/notifications?limit=120");
                console.debug("[notifications] loaded page notifications", { count: state.items.length });
                render();
            } catch (error) {
                console.error("Failed to load notifications", error);
            }
        }

        list.addEventListener("click", async function (event) {
            const target = event.target;
            const button = target.closest("button[data-notification-id]");
            if (!button) {
                return;
            }

            try {
                await fetchJson("/notifications/" + button.dataset.notificationId, { method: "PATCH" });
                await loadItems();
                window.dispatchEvent(new CustomEvent("notifications:updated"));
            } catch (error) {
                console.error("Failed to mark notification as read", error);
            }
        });

        markAllBtn.addEventListener("click", async function () {
            try {
                await fetchJson("/notifications", { method: "PATCH" });
                await loadItems();
                window.dispatchEvent(new CustomEvent("notifications:updated"));
            } catch (error) {
                console.error("Failed to mark all notifications", error);
            }
        });

        allBtn.addEventListener("click", function () {
            state.filter = "all";
            allBtn.classList.add("active");
            unreadBtn.classList.remove("active");
            render();
        });

        unreadBtn.addEventListener("click", function () {
            state.filter = "unread";
            unreadBtn.classList.add("active");
            allBtn.classList.remove("active");
            render();
        });

        window.addEventListener("notifications:updated", loadItems);
        loadItems();
    }

    document.querySelectorAll("[data-notification-shell]").forEach(attachNavbarWidget);
    attachNotificationPage();
})();
