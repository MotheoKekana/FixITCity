
(() => {
    "use strict";

    const hamburger = document.getElementById("hamburgerBtn");
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebarOverlay");

    // Stop if the required navigation elements do not exist.
    if (!hamburger || !sidebar || !overlay) {
        return;
    }

    const mobileQuery = window.matchMedia("(max-width: 768px)");
    const hamburgerIcon = hamburger.querySelector("i");

    // Open or close the mobile sidebar.
    function setSidebarOpen(isOpen, returnFocus = false) {
        const shouldOpen = Boolean(isOpen && mobileQuery.matches);

        sidebar.classList.toggle("open", shouldOpen);
        overlay.classList.toggle("active", shouldOpen);

        hamburger.setAttribute(
            "aria-expanded",
            String(shouldOpen)
        );

        hamburger.setAttribute(
            "aria-label",
            shouldOpen ? "Close navigation" : "Open navigation"
        );

        overlay.setAttribute(
            "aria-hidden",
            String(!shouldOpen)
        );

        overlay.tabIndex = shouldOpen ? 0 : -1;

        document.body.classList.toggle(
            "sidebar-open",
            shouldOpen
        );

        // Switch the hamburger icon between bars and a close icon.
        if (hamburgerIcon) {
            hamburgerIcon.classList.toggle("fa-bars", !shouldOpen);
            hamburgerIcon.classList.toggle("fa-xmark", shouldOpen);
        }

        // Return keyboard focus to the menu button when closing.
        if (returnFocus) {
            hamburger.focus();
        }
    }

    // Toggle the sidebar when the hamburger button is clicked.
    hamburger.addEventListener("click", () => {
        const isOpen = sidebar.classList.contains("open");
        setSidebarOpen(!isOpen);
    });

    // Close the sidebar when the backdrop is clicked.
    overlay.addEventListener("click", () => {
        setSidebarOpen(false, true);
    });

    // Close the sidebar when Escape is pressed.
    document.addEventListener("keydown", (event) => {
        if (
            event.key === "Escape" &&
            sidebar.classList.contains("open")
        ) {
            setSidebarOpen(false, true);
        }
    });

    // Close the sidebar after a navigation link is selected on mobile.
    sidebar.querySelectorAll("a").forEach((link) => {
        link.addEventListener("click", () => {
            if (mobileQuery.matches) {
                setSidebarOpen(false);
            }
        });
    });

    // Reset the sidebar when switching back to desktop width.
    mobileQuery.addEventListener("change", (event) => {
        if (!event.matches) {
            setSidebarOpen(false);
        }
    });
})();
