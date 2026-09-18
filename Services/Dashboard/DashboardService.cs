using FixITCity_Dashboard.ViewModels.Dashboard;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace FixITCity_Dashboard.Services.Dashboard
{
    public class DashboardService : IDashboardService
    {
        public Task<DashboardViewModel> GetDashboardSummaryAsync()
        {
            var model = new DashboardViewModel
            {
                Statistics = new DashboardStatisticsViewModel
                {
                    ApprovedCount = 132,
                    PendingApprovalCount = 45,
                    UnresolvedCount = 89,
                    EmergencyCount = 9
                },
                Map = new MapSummaryViewModel
                {
                    CenterLatitude = -33.9608,
                    CenterLongitude = 25.6022,
                    MapPoints = new List<MapPointViewModel>
                    {
                        new MapPointViewModel { Latitude = -33.9608, Longitude = 25.6022, Title = "Road issue", Description = "Pothole in Central PE", IsEmergency = false },
                        new MapPointViewModel { Latitude = -33.9555, Longitude = 25.5900, Title = "Water burst", Description = "Burst pipe", IsEmergency = true },
                        new MapPointViewModel { Latitude = -33.9800, Longitude = 25.6400, Title = "Electrical fault", Description = "Power outage", IsEmergency = true }
                    }
                },
                Categories = new CategoryStatisticsViewModel
                {
                    ReportsByCategory = new Dictionary<string, int>
                    {
                        { "Roads", 120 },
                        { "Water", 90 },
                        { "Electricity", 65 },
                        { "Waste", 50 },
                        { "Street Lighting", 32 }
                    }
                },
                Activity = new ActivityStatisticsViewModel
                {
                    ReportsByDay = new Dictionary<string, int>
                    {
                        { "Mon", 12 },
                        { "Tue", 18 },
                        { "Wed", 15 },
                        { "Thu", 29 },
                        { "Fri", 21 },
                        { "Sat", 8 },
                        { "Sun", 5 }
                    }
                },
                Emergencies = new EmergencySummaryViewModel
                {
                    TotalEmergencies = 9,
                    EmergenciesByCategory = new Dictionary<string, int>
                    {
                        { "Water", 3 },
                        { "Electricity", 2 },
                        { "Roads", 2 },
                        { "Other", 2 }
                    }
                },
                Alerts = new List<DashboardAlertViewModel>
                {
                    new DashboardAlertViewModel { Message = "9 emergency reports require attention", Type = "danger" },
                    new DashboardAlertViewModel { Message = "18 reports have been awaiting approval for more than 48 hours", Type = "warning" },
                    new DashboardAlertViewModel { Message = "Report volume has increased by 14% this week", Type = "info" }
                }
            };

            return Task.FromResult(model);
        }
    }
}