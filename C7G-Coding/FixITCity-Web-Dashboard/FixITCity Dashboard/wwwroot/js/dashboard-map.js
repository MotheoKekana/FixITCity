document.addEventListener("DOMContentLoaded", function () {
    if (document.getElementById('dashboard-map')) {
        var map = L.map('dashboard-map').setView(mapCenter, 13);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; OpenStreetMap contributors'
        }).addTo(map);

        // Add markers based on points passed from the model
        if (typeof mapPoints !== 'undefined' && mapPoints.length > 0) {
            mapPoints.forEach(function (point) {
                var color = point.IsEmergency ? 'red' : 'blue';

                var marker = L.circleMarker([point.Latitude, point.Longitude], {
                    color: color,
                    fillColor: color,
                    fillOpacity: 0.7,
                    radius: 8
                }).addTo(map);

                var popupContent = `
                    <div style="min-width: 150px;">
                        <strong>${point.Title}</strong><br/>
                        ${point.Description}<br/>
                        ${point.IsEmergency ? '<span style="color:red; font-weight:bold;">Emergency Status</span><br/>' : ''}
                        <br/>
                        <a href="/Reports" class="btn btn-sm btn-primary mt-1 py-0 px-2" style="font-size: 0.8rem;">View Reports</a>
                    </div>
                `;

                marker.bindPopup(popupContent);
            });
        }
    }
});