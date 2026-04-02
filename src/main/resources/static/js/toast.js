(function () {
    function ensureContainer() {
        var container = document.querySelector("[data-toast-container]");
        if (container) {
            return container;
        }

        container = document.createElement("div");
        container.className = "biblio-toast-container";
        container.setAttribute("data-toast-container", "true");
        document.body.appendChild(container);
        return container;
    }

    function show(message, type, durationMs) {
        var text = (message || "").trim();
        if (!text) {
            return;
        }

        var kind = type || "info";
        var timeoutMs = Number.isFinite(durationMs) ? durationMs : 2600;
        var container = ensureContainer();

        var toast = document.createElement("div");
        toast.className = "biblio-toast " + "biblio-toast-" + kind;
        toast.textContent = text;
        container.appendChild(toast);

        window.requestAnimationFrame(function () {
            toast.classList.add("is-visible");
        });

        window.setTimeout(function () {
            toast.classList.remove("is-visible");
            window.setTimeout(function () {
                toast.remove();
            }, 220);
        }, timeoutMs);
    }

    window.BiblioToast = {
        show: show
    };
})();
