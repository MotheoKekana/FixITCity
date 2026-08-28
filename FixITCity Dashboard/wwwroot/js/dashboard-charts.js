document.addEventListener("DOMContentLoaded", function () {
    // 1. Initialize Category Chart (Bar Chart)
    var ctxCategory = document.getElementById('categoryChart');
    if (ctxCategory && typeof categoryLabels !== 'undefined') {
        new Chart(ctxCategory, {
            type: 'bar',
            data: {
                labels: categoryLabels,
                datasets: [{
                    label: 'Reports',
                    data: categoryData,
                    backgroundColor: '#009688', // using primary sidebar color
                    borderColor: '#00796B',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    // 2. Initialize Activity Chart (Line Chart)
    var ctxActivity = document.getElementById('activityChart');
    if (ctxActivity && typeof activityLabels !== 'undefined') {
        new Chart(ctxActivity, {
            type: 'line',
            data: {
                labels: activityLabels,
                datasets: [{
                    label: 'Trend',
                    data: activityData,
                    backgroundColor: 'rgba(0, 150, 136, 0.2)',
                    borderColor: '#009688',
                    borderWidth: 2,
                    fill: true,
                    tension: 0.3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }
});