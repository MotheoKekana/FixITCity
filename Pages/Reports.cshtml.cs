using Microsoft.AspNetCore.Mvc.RazorPages;
using FixITCity_Dashboard.Services.Dashboard.Dtos;
using System.Net.Http.Json;
using CsvHelper;
using Microsoft.AspNetCore.Mvc;
using System.Globalization;
using System.Text;

namespace FixITCity_Dashboard.Pages
{
    public class ReportsModel : PageModel
    {
        private readonly IHttpClientFactory _httpClientFactory;
        private readonly ILogger<ReportsModel> _logger;

        public ReportsModel(IHttpClientFactory httpClientFactory, ILogger<ReportsModel> logger)
        {
            _httpClientFactory = httpClientFactory;
            _logger = logger;
        }

        public static readonly (string Match, string Label, string CssClass)[] StatusDefinitions =
        {
            ("pending",     "Pending",      "stat-pending"),
            ("underreview", "Under Review", "stat-review"),
            ("accepted",    "Accepted",     "stat-accepted"),
            ("inprogress",  "In Progress",  "stat-progress"),
            ("fixed",       "Fixed",        "stat-fixed"),
            ("rejected",    "Rejected",     "stat-rejected"),
        };

        public IEnumerable<(string Match, string Label, string CssClass)> StatusOptions => StatusDefinitions;

        public static string NormalizeStatus(string? status)
        {
            if (string.IsNullOrWhiteSpace(status)) return string.Empty;

            var buffer = new StringBuilder(status.Length);
            foreach (var ch in status)
            {
                if (char.IsLetterOrDigit(ch))
                    buffer.Append(char.ToLowerInvariant(ch));
            }
            return buffer.ToString();
        }

        public record StatusCard(string Label, string CssClass, int Count);

        // Filters
        public string? SearchTerm { get; set; }
        public string? StatusFilter { get; set; }
        public string? CategoryFilter { get; set; }
        public string? DateSort { get; set; }

        // Report list
        public List<ReportListItemDto> Reports { get; set; } = new();

        // Statistics
        public int TotalReports { get; set; }
        public List<StatusCard> StatusCards { get; set; } = new();

        // Pagination
        public int CurrentPage { get; set; } = 1;
        public int TotalPages { get; set; }
        public int PageSize { get; set; } = 10;

        // Categories for dropdown
        public List<string> Categories { get; set; } = new();

        // Error flag
        public bool LoadFailed { get; set; }

        public async Task OnGetAsync(
            string? searchTerm,
            string? statusFilter,
            string? categoryFilter,
            string? dateSort,
            int? pageNumber)
        {
            SearchTerm = searchTerm;
            StatusFilter = string.IsNullOrWhiteSpace(statusFilter) ? "All" : statusFilter;
            CategoryFilter = string.IsNullOrWhiteSpace(categoryFilter) ? "All" : categoryFilter;
            DateSort = string.IsNullOrWhiteSpace(dateSort) ? "Newest" : dateSort;
            CurrentPage = pageNumber ?? 1;

            var allReports = await FetchAllReportsAsync();

            Categories = allReports
                .Where(r => !string.IsNullOrWhiteSpace(r.CategoryName))
                .Select(r => r.CategoryName!)
                .Distinct()
                .OrderBy(c => c)
                .ToList();

            TotalReports = allReports.Count;
            StatusCards = BuildStatusCards(allReports);

            var filtered = ApplyFilters(allReports);

            var ordered = DateSort == "Oldest"
                ? filtered.OrderBy(r => r.CreatedAt).ThenByDescending(r => r.PriorityScore ?? 0).ToList()
                : filtered.OrderByDescending(r => r.CreatedAt).ThenByDescending(r => r.PriorityScore ?? 0).ToList();

            TotalPages = (int)Math.Ceiling(ordered.Count / (double)PageSize);
            if (TotalPages < 1) TotalPages = 1;
            if (CurrentPage < 1) CurrentPage = 1;
            if (CurrentPage > TotalPages) CurrentPage = TotalPages;

            Reports = ordered
                .Skip((CurrentPage - 1) * PageSize)
                .Take(PageSize)
                .ToList();
        }

        private static List<StatusCard> BuildStatusCards(List<ReportListItemDto> reports)
        {
            var counts = reports
                .GroupBy(r => NormalizeStatus(r.Status))
                .ToDictionary(g => g.Key, g => g.Count(), StringComparer.Ordinal);

            return StatusDefinitions
                .Select(def => new StatusCard(
                    def.Label,
                    def.CssClass,
                    counts.TryGetValue(def.Match, out var count) ? count : 0))
                .ToList();
        }

        public async Task<IActionResult> OnGetExportCsvAsync(
            string? searchTerm,
            string? statusFilter,
            string? categoryFilter,
            string? dateSort)
        {
            SearchTerm = searchTerm;
            StatusFilter = string.IsNullOrWhiteSpace(statusFilter) ? "All" : statusFilter;
            CategoryFilter = string.IsNullOrWhiteSpace(categoryFilter) ? "All" : categoryFilter;
            DateSort = string.IsNullOrWhiteSpace(dateSort) ? "Newest" : dateSort;

            var allReports = await FetchAllReportsAsync();
            var matchingReports = ApplyFilters(allReports);

            var ordered = DateSort == "Oldest"
                ? matchingReports.OrderBy(r => r.CreatedAt).ThenByDescending(r => r.PriorityScore ?? 0).ToList()
                : matchingReports.OrderByDescending(r => r.CreatedAt).ThenByDescending(r => r.PriorityScore ?? 0).ToList();

            var rows = ordered.Select(report => new ReportExportRow
            {
                ReferenceNumber = report.ReferenceNumber,
                Title = report.Title,
                CategoryName = report.CategoryName ?? string.Empty,
                Status = report.Status,
                PriorityScore = report.PriorityScore,
                CreatedAt = report.CreatedAt
            }).ToList();

            using var writer = new StringWriter(CultureInfo.InvariantCulture);
            using (var csv = new CsvWriter(writer, CultureInfo.InvariantCulture))
            {
                csv.WriteRecords(rows);
            }

            var bytes = Encoding.UTF8.GetBytes(writer.ToString());
            return File(bytes, "text/csv; charset=utf-8", "FixITCity_Reports.csv");
        }

        private async Task<List<ReportListItemDto>> FetchAllReportsAsync()
        {
            try
            {
                var client = _httpClientFactory.CreateClient("Api");
                var result = await client.GetFromJsonAsync<List<ReportListItemDto>>("Reports");
                return result ?? new List<ReportListItemDto>();
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to load reports from API");
                LoadFailed = true;
                return new List<ReportListItemDto>();
            }
        }

        private List<ReportListItemDto> ApplyFilters(List<ReportListItemDto> reports)
        {
            var filtered = reports.AsQueryable();

            if (!string.IsNullOrWhiteSpace(StatusFilter) && StatusFilter != "All")
            {
                var target = NormalizeStatus(StatusFilter);
                filtered = filtered.Where(r => NormalizeStatus(r.Status) == target);
            }

            if (!string.IsNullOrWhiteSpace(CategoryFilter) && CategoryFilter != "All")
                filtered = filtered.Where(r => r.CategoryName == CategoryFilter);

            if (!string.IsNullOrWhiteSpace(SearchTerm))
            {
                var search = SearchTerm.Trim().ToLower();
                filtered = filtered.Where(r =>
                    (r.Title ?? "").ToLower().Contains(search) ||
                    (r.CategoryName ?? "").ToLower().Contains(search) ||
                    (r.Status ?? "").ToLower().Contains(search) ||
                    (r.AreaName ?? "").ToLower().Contains(search) ||
                    (r.StreetName ?? "").ToLower().Contains(search) ||
                    (r.SuburbName ?? "").ToLower().Contains(search) ||
                    (r.CityName ?? "").ToLower().Contains(search) ||
                    r.ReportId.ToString().Contains(search));
            }

            return filtered.ToList();
        }
    }
}