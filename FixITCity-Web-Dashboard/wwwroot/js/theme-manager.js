
(() => {
    "use strict";

    const root = document.documentElement;
    const validPreferences = ["light", "dark", "system"];
    const systemQuery = window.matchMedia("(prefers-color-scheme: dark)");

    function resolveTheme(preference) {
        if (preference === "system") {
            return systemQuery.matches ? "dark" : "light";
        }

        return preference === "dark" ? "dark" : "light";
    }

    function applyPreference(preference) {
        const safePreference = validPreferences.includes(preference)
            ? preference
            : "light";

        const resolvedTheme = resolveTheme(safePreference);

        root.dataset.themePreference = safePreference;
        root.dataset.theme = resolvedTheme;

        document.dispatchEvent(new CustomEvent("fixitcity:themechange", {
            detail: {
                preference: safePreference,
                theme: resolvedTheme
            }
        }));
    }

    // Apply the preference supplied by the server-rendered layout.
    applyPreference(root.dataset.themePreference || "light");

    // React to operating-system changes while System mode is selected.
    systemQuery.addEventListener("change", () => {
        if (root.dataset.themePreference === "system") {
            applyPreference("system");
        }
    });

    // Allow Settings and other pages to preview/change the current theme.
    window.FixITCityTheme = {
        applyPreference,
        getPreference: () => root.dataset.themePreference,
        getResolvedTheme: () => root.dataset.theme
    };
})();
