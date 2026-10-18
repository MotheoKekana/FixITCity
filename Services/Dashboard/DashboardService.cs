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

            // Start all three requests before waiting for any of them.
            var statsTask = GetSafelyAsync<DashboardStatsApiResponse>(
                client, "Dashboard/stats");

            var categoriesTask = GetSafelyAsync<List<CategoryCountDto>>(
                client, "Dashboard/reports-by-category");

            var reportsTask = GetSafelyAsync<List<ReportListItemDto>>(
                client, "Reports");

            await Task.WhenAll(statsTask, categoriesTask, reportsTask);

            var statsResult = await statsTask;
            var categoriesResult = await categoriesTask;
            var reportsResult = await reportsTask;

            AddWarning(model, statsResult.Error, "Statistics unavailable");
            AddWarning(model, categoriesResult.Error, "Category breakdown unavailable");
            AddWarning(model, reportsResult.Error, "Report data unavailable");

            if (statsResult.Data is { } stats)
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

            if (categoriesResult.Data is { } categories)
            {
                model.Categories = new CategoryStatisticsViewModel
                {
                    ReportsByCategory = categories
                        .Where(c => !string.IsNullOrWhiteSpace(c.CategoryName))
                        .ToDictionary(c => c.CategoryName!, c => c.Count)
                };
            }

            if (reportsResult.Data is { } reports)
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
                    activity[label] = countsByDate.TryGetValue(day, out var count)
                        ? count
                        : 0;
                }

                model.Activity = new ActivityStatisticsViewModel
                {
                    ReportsByDay = activity
                };
            }

            return model;
        }

        private static async Task<(T? Data, string? Error)> GetSafelyAsync<T>(
            HttpClient client,
            string path)
            where T : class
        {
            try
            {
                var data = await client.GetFromJsonAsync<T>(path);
                return (data, null);
            }
            catch (Exception ex)
            {
                return (null, ex.Message);
            }
        }

            private static void AddWarning(
                DashboardViewModel model,
                string? error,
                string message)
            {
                if (error != null)
                {
                    model.Alerts.Add(new DashboardAlertViewModel
                    {
                        Message = $"{message}: {error}",
                        Type = "warning"
                    });
                }
                
            }
    }
}