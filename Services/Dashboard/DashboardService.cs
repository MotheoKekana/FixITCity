using FixITCity_Dashboard.Services.Dashboard.Dtos;
using FixITCity_Dashboard.ViewModels.Dashboard;
using System.Net.Http.Json;

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
            var model = new DashboardViewModel();
            var client = _httpClientFactory.CreateClient("Api");

            // Stats
            try
            {
                var stats = await client.GetFromJsonAsync<DashboardStatsApiResponse>("Dashboard/stats");
                if (stats != null)
                {
                    model.Statistics = new DashboardStatisticsViewModel
                    {
                        Total = stats.Total,
                        Pending = stats.Pending,
                        UnderReview = stats.UnderReview,
                        Accepted = stats.Accepted,
                        InProgress = stats.InProgress,
                        Fixed = stats.Fixed,
                        Rejected = stats.Rejected
                    };
                }
            }
            catch (Exception ex)
            {
                model.Alerts.Add(new DashboardAlertViewModel
                {
                    Message = "Statistics unavailable: " + ex.Message,
                    Type = "warning"
                });
            }

            // Categories
            try
            {
                var categories = await client.GetFromJsonAsync<List<CategoryCountDto>>("Dashboard/reports-by-category");
                if (categories != null)
                {
                    model.Categories = new CategoryStatisticsViewModel
                    {
                        ReportsByCategory = categories
                            .Where(c => !string.IsNullOrWhiteSpace(c.CategoryName))
                            .ToDictionary(c => c.CategoryName!, c => c.Count)
                    };
                }
            }
            catch (Exception ex)
            {
                model.Alerts.Add(new DashboardAlertViewModel
                {
                    Message = "Category breakdown unavailable: " + ex.Message,
                    Type = "warning"
                });
            }

            // Reports (feeds map + activity chart)
            try
            {
                var reports = await client.GetFromJsonAsync<List<ReportListItemDto>>("Reports");
                if (reports != null)
                {
                    model.Map.MapPoints = reports
                        .Where(r => r.Latitude != 0 && r.Longitude != 0)
                        .Select(r => new MapPointViewModel
                        {
                            Latitude = r.Latitude,
                            Longitude = r.Longitude,
                            Title = r.Title,
                            Description = r.Description,
                            IsEmergency = false
                        })
                        .ToList();

                    var today = DateTime.Today;
                    var startDate = today.AddDays(-6);

                    var countsByDate = reports
                        .Where(r => r.CreatedAt.HasValue)
                        .Select(r => r.CreatedAt!.Value.Date)
                        .Where(d => d >= startDate && d <= today)
                        .GroupBy(d => d)
                        .ToDictionary(g => g.Key, g => g.Count());

                    var activity = new Dictionary<string, int>();
                    for (int i = 0; i < 7; i++)
                    {
                        var day = startDate.AddDays(i);
                        var label = day.ToString("ddd");
                        activity[label] = countsByDate.TryGetValue(day, out var c) ? c : 0;
                    }

                    model.Activity = new ActivityStatisticsViewModel
                    {
                        ReportsByDay = activity
                    };
                }
            }
            catch (Exception ex)
            {
                model.Alerts.Add(new DashboardAlertViewModel
                {
                    Message = "Report data unavailable: " + ex.Message,
                    Type = "warning"
                });
            }

            return model;
        }
    }
}