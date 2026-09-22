using Microsoft.AspNetCore.Mvc.RazorPages;
using FixITCity_Dashboard.Services.Dashboard.Dtos;
using System.Net.Http.Json;

namespace FixITCity_Dashboard.Pages
{
    public class ReportsModel : PageModel
    {
        private readonly IHttpClientFactory _httpClientFactory;

        public ReportsModel(IHttpClientFactory httpClientFactory)
        {
            _httpClientFactory = httpClientFactory;
        }

        // Filters
        public string? SearchTerm { get; set; }
        public string? StatusFilter { get; set; }
        public string? CategoryFilter { get; set; }
        public string? DateSort { get; set; }

        // Report list
        public List<ReportListItemDto> Reports { get; set; } = new();

        // Statistics
        public int TotalReports { get; set; }
        public int TotalPending { get; set; }
        public int TotalInProgress { get; set; }
        public int TotalResolved { get; set; }

        // Pagination
        public int CurrentPage { get; set; } = 1;
        public int TotalPages { get; set; }
        public int PageSize { get; set; } = 10;

        // Categories for dropdown
        public List<string> Categories { get; set; } = new();

        public async Task OnGetAsync(
            string? searchTerm,
            string? statusFilter,
            string? categoryFilter,
            string? dateSort,
            int? page)
        {
            SearchTerm = searchTerm;
            StatusFilter = string.IsNullOrWhiteSpace(statusFilter) ? "All" : statusFilter;
            CategoryFilter = string.IsNullOrWhiteSpace(categoryFilter) ? "All" : categoryFilter;
            DateSort = string.IsNullOrWhiteSpace(dateSort) ? "Newest" : dateSort;
            CurrentPage = page ?? 1;

            // Fetch all reports from the API
            var allReports = new List<ReportListItemDto>();
            try
            {
                var client = _httpClientFactory.CreateClient("Api");
                var result = await client.GetFromJsonAsync<List<ReportListItemDto>>("Reports");
                if (result != null) allReports = result;
            }
            catch
            {
                // API down — page will show empty list
            }

            // Category dropdown
            Categories = allReports
                .Where(r => !string.IsNullOrWhiteSpace(r.CategoryName))
                .Select(r => r.CategoryName!)
                .Distinct()
                .OrderBy(c => c)
                .ToList();

            // Statistics (based on ALL reports)
            TotalReports = allReports.Count;
            TotalPending = allReports.Count(r => r.Status == "Pending");
            TotalInProgress = allReports.Count(r => r.Status == "InProgress");
            TotalResolved = allReports.Count(r => r.Status == "Resolved");

            // Apply filters + sort
            var filtered = ApplyFilters(allReports);

            var ordered = DateSort == "Oldest"
                ? filtered.OrderBy(r => r.CreatedAt).ThenByDescending(r => r.PriorityScore ?? 0).ToList()
                : filtered.OrderByDescending(r => r.CreatedAt).ThenByDescending(r => r.PriorityScore ?? 0).ToList();

            // Pagination
            TotalPages = (int)Math.Ceiling(ordered.Count / (double)PageSize);
            if (TotalPages < 1) TotalPages = 1;
            if (CurrentPage < 1) CurrentPage = 1;
            if (CurrentPage > TotalPages) CurrentPage = TotalPages;

            Reports = ordered
                .Skip((CurrentPage - 1) * PageSize)
                .Take(PageSize)
                .ToList();
        }

        private List<ReportListItemDto> ApplyFilters(List<ReportListItemDto> reports)
        {
            var filtered = reports.AsQueryable();

            if (!string.IsNullOrWhiteSpace(StatusFilter) && StatusFilter != "All")
                filtered = filtered.Where(r => r.Status == StatusFilter);

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
                    r.ReportId.ToString().Contains(search));
            }

            return filtered.ToList();
        }
    }
}