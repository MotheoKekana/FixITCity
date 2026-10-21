// =========================================================
// DEBOUNCED SEARCH
// =========================================================

    const searchInput = document.getElementById("reportSearch");

    let searchTimer;

    searchInput.addEventListener("input", function () {

        // Cancel the previous timer whenever the user types
        clearTimeout(searchTimer);

        // Wait 300ms after the user stops typing
        searchTimer = setTimeout(function () {

            // Reset to page 1 before submitting
            const form = searchInput.form;

            const pageInput = form.querySelector('[name="page"]');

            if (pageInput) {
                pageInput.value = "1";
            }

            form.submit();

        }, 300);

    });