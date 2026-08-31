// dashboard-charts.js
// Category Chart (Doughnut)
var ctxCategory = document.getElementById('categoryChart').getContext('2d');
new Chart(ctxCategory, {
    type: 'doughnut',
    data: {
        labels: categoryLabels,
        datasets: [{
            data: categoryData,
            backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0', '#9966FF']
        }]
    },
    options: {
        responsive: true,
        plugins: {
            legend: { position: 'bottom' }
        }
    }
});

// Activity Chart (Bar)
var ctxActivity = document.getElementById('activityChart').getContext('2d');
new Chart(ctxActivity, {
    type: 'bar',
    data: {
        labels: activityLabels,
        datasets: [{
            label: 'Reports',
            data: activityData,
            backgroundColor: '#36A2EB'
        }]
    },
    options: {
        responsive: true,
        plugins: {
            legend: { display: false }
        },
        scales: {
            y: { beginAtZero: true }
        }
    }
});