// dashboard-map.js
var map = L.map('dashboard-map').setView(mapCenter, 13);

L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors'
}).addTo(map);

mapPoints.forEach(function (point) {
    var marker = L.marker([point.latitude, point.longitude]).addTo(map);
    var popupContent = '<strong>' + point.title + '</strong><br>' + point.description;
    if (point.isEmergency) {
        popupContent += '<br><span class="badge bg-danger">Emergency</span>';
    }
    marker.bindPopup(popupContent);
});