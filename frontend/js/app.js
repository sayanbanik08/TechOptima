(() => {
    "use strict";

    const REQUEST_TIMEOUT_MS = 15000;
    const NOTIFICATION_TYPES = new Set(["loading", "success", "warning", "error"]);
    let notification;
    let dismissTimer;

    function notificationRegion() {
        let region = document.getElementById("notification-region");
        if (region) return region;

        region = document.createElement("div");
        region.id = "notification-region";
        region.className = "notification-region";
        region.setAttribute("aria-live", "polite");
        region.setAttribute("aria-atomic", "true");
        document.body.append(region);
        return region;
    }

    function removeNotification() {
        window.clearTimeout(dismissTimer);
        if (notification) notification.remove();
        notification = null;
    }

    function addCloseButton(element) {
        if (element.querySelector(".notification__close")) return;

        const closeButton = document.createElement("button");
        closeButton.className = "notification__close";
        closeButton.type = "button";
        closeButton.setAttribute("aria-label", "Close notification");
        closeButton.textContent = "×";
        closeButton.addEventListener("click", removeNotification);
        element.append(closeButton);
    }

    function showNotification(type, message) {
        const safeType = NOTIFICATION_TYPES.has(type) ? type : "warning";
        const region = notificationRegion();
        removeNotification();

        notification = document.createElement("div");
        notification.className = `notification notification--${safeType}`;
        notification.setAttribute("role", safeType === "error" ? "alert" : "status");

        const title = document.createElement("strong");
        title.textContent = `${safeType.charAt(0).toUpperCase()}${safeType.slice(1)}:`;
        const text = document.createElement("span");
        text.textContent = message;

        notification.append(title, text);
        addCloseButton(notification);
        region.append(notification);

        if (safeType !== "loading") {
            dismissTimer = window.setTimeout(removeNotification, 5000);
        }
    }

    async function serverErrorMessage(response) {
        const html = await response.text();
        const documentBody = new DOMParser().parseFromString(html, "text/html");
        const message = documentBody.querySelector("[data-server-message]")?.textContent.trim();

        return message || "The backend could not complete this request. Please try again.";
    }

    function lockForm(form) {
        const submitButton = form.querySelector("button[type='submit']");
        const originalLabel = submitButton?.textContent;

        form.dataset.submitting = "true";
        form.setAttribute("aria-busy", "true");

        if (submitButton) {
            submitButton.disabled = true;
            submitButton.textContent = submitButton.dataset.loadingLabel || "Saving…";
        }

        return () => {
            delete form.dataset.submitting;
            form.removeAttribute("aria-busy");

            if (submitButton) {
                submitButton.disabled = false;
                submitButton.textContent = originalLabel;
            }
        };
    }

    async function submitForm(form, event) {
        if (event.defaultPrevented) return;
        event.preventDefault();

        if (form.dataset.submitting === "true") {
            showNotification("warning", "This request is already being processed.");
            return;
        }

        if (!form.checkValidity()) return;

        const resetForm = lockForm(form);
        const controller = new AbortController();
        const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

        showNotification("loading", "Saving your changes. Please wait…");

        try {
            const response = await fetch(form.action, {
                method: form.method || "POST",
                headers: { "Accept": "text/html" },
                body: new URLSearchParams(new FormData(form)),
                signal: controller.signal
            });

            if (!response.ok) throw new Error(await serverErrorMessage(response));

            const responseUrl = new URL(response.url);
            const type = responseUrl.searchParams.get("feedback");
            const message = responseUrl.searchParams.get("message");

            if (type !== "success" || !message) {
                throw new Error("The backend did not confirm that the request was saved.");
            }

            window.location.assign(responseUrl.href);
        } catch (error) {
            const message = error.name === "AbortError"
                ? "The request timed out. Check that the backend is available and try again."
                : error.message || "The backend is unavailable. Please try again.";
            showNotification("error", message);
            resetForm();
        } finally {
            window.clearTimeout(timeout);
        }
    }

    async function openOptimization(link, event) {
        event.preventDefault();

        if (link.dataset.loading === "true") {
            showNotification("warning", "Optimization is already starting.");
            return;
        }

        const originalLabel = link.textContent;
        const controller = new AbortController();
        const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

        link.dataset.loading = "true";
        link.setAttribute("aria-disabled", "true");
        link.textContent = link.dataset.loadingLabel || "Loading…";
        showNotification("loading", "Preparing optimization. Please wait…");

        try {
            const response = await fetch(link.href, { signal: controller.signal });
            if (!response.ok) throw new Error(await serverErrorMessage(response));

            document.open();
            document.write(await response.text());
            document.close();
        } catch (error) {
            const message = error.name === "AbortError"
                ? "Optimization timed out. Check that the backend is available and try again."
                : error.message || "The backend is unavailable. Please try again.";
            showNotification("error", message);
            delete link.dataset.loading;
            link.removeAttribute("aria-disabled");
            link.textContent = originalLabel;
        } finally {
            window.clearTimeout(timeout);
        }
    }

    document.querySelectorAll(".notification").forEach((element) => {
        notification = element;
        addCloseButton(element);
    });

    const feedback = new URLSearchParams(window.location.search);
    const feedbackType = feedback.get("feedback");
    const feedbackMessage = feedback.get("message");
    if (feedbackType && feedbackMessage) {
        showNotification(feedbackType, feedbackMessage);
        window.history.replaceState({}, document.title, window.location.pathname);
    }

    document.querySelectorAll("form").forEach((form) => {
        form.addEventListener("invalid", () => {
            showNotification("error", "Please check the highlighted fields before submitting.");
        }, true);
        form.addEventListener("submit", (event) => submitForm(form, event));
    });

    const dependencies = document.getElementById("dependencies");
    if (dependencies) {
        dependencies.addEventListener("blur", () => {
            if (dependencies.value.trim() === "") {
                showNotification("warning", "No dependencies were added. This application will be treated as independent.");
            }
        });
    }

    document.querySelectorAll("a.action-link").forEach((link) => {
        link.addEventListener("click", (event) => openOptimization(link, event));
    });
})();
