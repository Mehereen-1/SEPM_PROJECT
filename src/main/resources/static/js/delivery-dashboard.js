(function () {
    var actionConfig = {
        "start-pickup": {
            endpoint: "/delivery/pickup-start/",
            successMessage: "Pickup started successfully",
            nextStatus: "PICKUP_STARTED"
        },
        "book-picked": {
            endpoint: "/delivery/book-picked/",
            successMessage: "Book picked successfully",
            nextStatus: "BOOK_PICKED"
        },
        "mark-delivered": {
            endpoint: "/delivery/complete/",
            successMessage: "Delivery completed",
            nextStatus: "COMPLETED"
        }
    };

    function toast(message, type) {
        if (window.BiblioToast && typeof window.BiblioToast.show === "function") {
            window.BiblioToast.show(message, type || "success");
            return;
        }
        window.alert(message);
    }

    function escapeHtml(value) {
        return String(value || "").replace(/[&<>"']/g, function (char) {
            var map = {
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                '"': "&quot;",
                "'": "&#39;"
            };
            return map[char] || char;
        });
    }

    function statusBadgeClass(status) {
        if (status === "AVAILABLE") {
            return "inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold bg-rose-50 text-rose-700";
        }
        if (status === "COMPLETED") {
            return "inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold bg-emerald-100 text-emerald-800";
        }
        return "inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold bg-amber-100 text-amber-800";
    }

    function resolveParticipants(actionsNode) {
        return {
            firstPickupName: actionsNode ? (actionsNode.getAttribute("data-first-pickup-name") || "Reader A") : "Reader A",
            secondPickupName: actionsNode ? (actionsNode.getAttribute("data-second-pickup-name") || "Reader B") : "Reader B",
            finalDropoffName: actionsNode ? (actionsNode.getAttribute("data-final-dropoff-name") || "Reader A") : "Reader A"
        };
    }

    function lifecycleButtonHtml(status, actionsNode) {
        var participants = resolveParticipants(actionsNode);
        var firstPickupName = escapeHtml(participants.firstPickupName);
        var secondPickupName = escapeHtml(participants.secondPickupName);
        var finalDropoffName = escapeHtml(participants.finalDropoffName);

        if (status === "ACCEPTED" || status === "PENDING") {
            return '<button type="button" class="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-indigo-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-indigo-700" data-delivery-action="start-pickup"><span class="material-symbols-rounded text-base">play_circle</span>Pick up from ' + firstPickupName + '</button>';
        }
        if (status === "PICKUP_STARTED") {
            return '<button type="button" class="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-amber-500 px-4 py-3 text-sm font-semibold text-white transition hover:bg-amber-600" data-delivery-action="book-picked"><span class="material-symbols-rounded text-base">inventory_2</span>Deliver to ' + secondPickupName + ' and pick up from ' + secondPickupName + '</button>';
        }
        if (status === "BOOK_PICKED") {
            return '<button type="button" class="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-slate-900 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800" data-delivery-action="mark-delivered"><span class="material-symbols-rounded text-base">task_alt</span>Deliver to ' + finalDropoffName + '</button>';
        }
        if (status === "COMPLETED") {
            return '<button type="button" class="w-full inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-100 px-4 py-3 text-sm font-semibold text-emerald-800 cursor-not-allowed" disabled><span class="material-symbols-rounded text-base">verified</span>Completed</button>';
        }
        return "";
    }

    function updateCardStatus(card, status) {
        var actions = card.querySelector("[data-delivery-actions]");
        var statusLabel = card.querySelector("[data-delivery-status-label]");

        if (!actions || !statusLabel) {
            return;
        }

        actions.setAttribute("data-delivery-status", status);
        statusLabel.textContent = status;
        statusLabel.className = statusBadgeClass(status);

        var mapLink = actions.querySelector('a[href*="/delivery/location/"]');
        var form = actions.querySelector("form");

        actions.innerHTML = "";
        if (form) {
            actions.appendChild(form);
        }

        var lifecycleHtml = lifecycleButtonHtml(status, actions);
        if (lifecycleHtml) {
            actions.insertAdjacentHTML("beforeend", lifecycleHtml);
        }

        if (mapLink) {
            actions.appendChild(mapLink);
        }

        if (status === "COMPLETED" && window.location.pathname.indexOf("/delivery/pending") !== -1) {
            window.setTimeout(function () {
                card.remove();
                if (!document.querySelector("[data-delivery-card-id]")) {
                    window.location.reload();
                }
            }, 380);
        }
    }

    async function postAction(url) {
        var response = await fetch(url, {
            method: "POST",
            headers: {
                "Accept": "application/json"
            }
        });

        var payload = {};
        try {
            payload = await response.json();
        } catch (error) {
            console.warn("[delivery-dashboard] Could not parse JSON response", error);
        }

        console.debug("[delivery-dashboard] action response", {
            url: url,
            statusCode: response.status,
            body: payload
        });

        if (!response.ok || payload.success === false) {
            throw new Error(payload.message || ("Request failed with " + response.status));
        }

        return payload;
    }

    async function handleActionClick(button) {
        var action = button.getAttribute("data-delivery-action");
        var config = actionConfig[action];
        var actionsNode = button.closest("[data-delivery-actions]");

        if (!config || !actionsNode) {
            return;
        }

        var deliveryId = actionsNode.getAttribute("data-delivery-id");
        var card = button.closest("[data-delivery-card-id]");
        if (!deliveryId || !card) {
            return;
        }

        var originalLabel = button.innerHTML;
        button.disabled = true;
        button.innerHTML = '<span class="material-symbols-rounded text-base">hourglass_top</span>Processing...';

        var endpoint = config.endpoint + deliveryId;
        console.debug("[delivery-dashboard] calling endpoint", { action: action, deliveryId: deliveryId, endpoint: endpoint });

        try {
            var payload = await postAction(endpoint);
            updateCardStatus(card, payload.status || config.nextStatus);
            toast(payload.message || config.successMessage, "success");
            window.dispatchEvent(new CustomEvent("notifications:updated"));
        } catch (error) {
            console.error("[delivery-dashboard] action failed", error);
            button.disabled = false;
            button.innerHTML = originalLabel;
            toast(error.message || "Something went wrong while processing this action.", "error");
        }
    }

    document.addEventListener("click", function (event) {
        var button = event.target.closest("button[data-delivery-action]");
        if (!button) {
            return;
        }

        event.preventDefault();
        handleActionClick(button);
    });
})();
