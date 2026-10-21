(function () {
    const mapElement = document.getElementById("issuesMap");
    if (!mapElement) return;

    if (typeof L === "undefined") {
        return;
    }

    let points = [];
    try {
        points = JSON.parse(mapElement.dataset.points || "[]");
    } catch {
        points = [];
    }

    const centerLat = Number(mapElement.dataset.centerLat) || -33.9608;
    const centerLng = Number(mapElement.dataset.centerLng) || 25.6022;

    mapElement.innerHTML = "";

    const map = L.map(mapElement, {
        zoomControl: true,
        scrollWheelZoom: false
    }).setView([centerLat, centerLng], 12);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19,
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    const bounds = [];

    points.forEach(function (point) {
        const latitude = Number(point.latitude);
        const longitude = Number(point.longitude);

        if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return;

        const markerIcon = L.divIcon({
            className: "",
            html: `<div class="custom-map-marker${point.isEmergency ? " emergency" : ""}"></div>`,
            iconSize: [18, 18],
            iconAnchor: [9, 9]
        });

        const marker = L.marker([latitude, longitude], { icon: markerIcon }).addTo(map);
        const safeTitle = escapeHtml(point.title || "Reported issue");
        const safeDescription = escapeHtml(point.description || "No additional description provided.");

        marker.bindPopup(
            `<div class="fixit-popup"><strong>${safeTitle}</strong><span>${safeDescription}</span></div>`
        );

        bounds.push([latitude, longitude]);
    });

    if (bounds.length > 1) {
        map.fitBounds(bounds, { padding: [28, 28], maxZoom: 14 });
    } else if (bounds.length === 1) {
        map.setView(bounds[0], 14);
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }
})();
