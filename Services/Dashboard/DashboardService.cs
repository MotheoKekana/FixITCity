using FixITCity_Dashboard.ViewModels.Dashboard;
using System.Net.Http.Json;
using System.Text.Json.Serialization;

namespace FixITCity_Dashboard.Services.Dashboard
{
    public class DashboardService : IDashboardService
    {
        private readonly IHttpClientFactory _httpClientFactory;

        public DashboardService(IHttpClientFactory httpClientFactory)
        {
            _httpClientFactory = httpClientFactory;
        }

        public async Task<DashboardViewModel> GetDashboardSummaryAsync()
        {
            var model = new DashboardViewModel
            {
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
                        { "Roads", 120 }, { "Water", 90 }, { "Electricity", 65 }, { "Waste", 50 }, { "Street Lighting", 32 }
                    }
                },
                Activity = new ActivityStatisticsViewModel
                {
                    ReportsByDay = new Dictionary<string, int>
                    {
                        { "Mon", 12 }, { "Tue", 18 }, { "Wed", 15 }, { "Thu", 29 }, { "Fri", 21 }, { "Sat", 8 }, { "Sun", 5 }
                    }
                },
                Emergencies = new EmergencySummaryViewModel
                {
                    TotalEmergencies = 9,
                    EmergenciesByCategory = new Dictionary<string, int>
                    {
                        { "Water", 3 }, { "Electricity", 2 }, { "Roads", 2 }, { "Other", 2 }
                    }
                },
                Alerts = new List<DashboardAlertViewModel>(),
                Statistics = new DashboardStatisticsViewModel()
            };

            try
            {
                var client = _httpClientFactory.CreateClient("Api");
                var stats = await client.GetFromJsonAsync<DashboardStatsApiResponse>("Dashboard/stats");

                if (stats != null)
                {
                    model.Statistics = new DashboardStatisticsViewModel
                    {
                        ApprovedCount = stats.Verified,
                        PendingApprovalCount = stats.Pending,
                        UnresolvedCount = stats.InProgress,
                        EmergencyCount = 0
                    };
                }
            }
            catch (Exception ex)
            {
                model.Alerts.Add(new DashboardAlertViewModel
                {
                    Message = "Could not reach API: " + ex.Message,
                    Type = "warning"
                });
            }

            return model;
        }
    }

    public class DashboardStatsApiResponse
    {
        [JsonPropertyName("total")] public int Total { get; set; }
        [JsonPropertyName("pending")] public int Pending { get; set; }
        [JsonPropertyName("verified")] public int Verified { get; set; }
        [JsonPropertyName("inProgress")] public int InProgress { get; set; }
        [JsonPropertyName("resolved")] public int Resolved { get; set; }
        [JsonPropertyName("rejected")] public int Rejected { get; set; }
    }
}