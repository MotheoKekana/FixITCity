using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System;
using System.Collections.Generic;
using System.Linq;

namespace FixITCity_Dashboard.Pages
{
    public class ReportsModel : PageModel
    {
        // Filter properties
        public string? SearchTerm { get; set; }
        public string? StatusFilter { get; set; }
        public string? CategoryFilter { get; set; }

        // Report list
        public List<FaultReport> Reports { get; set; } = new List<FaultReport>();

        // Statistics
        public int TotalReports { get; set; }
        public int TotalOpen { get; set; }
        public int TotalAssigned { get; set; }
        public int TotalClosed { get; set; }

        // Pagination
        public int CurrentPage { get; set; } = 1;
        public int TotalPages { get; set; }
        public int PageSize { get; set; } = 10;

        // Categories for filter dropdown
        public List<string> Categories { get; set; } = new List<string>();

        public void OnGet(string? searchTerm, string? statusFilter, string? categoryFilter, int? page)
        {
            // Set filters from query string
            SearchTerm = searchTerm;
            StatusFilter = statusFilter;
            CategoryFilter = categoryFilter;
            CurrentPage = page ?? 1;

            // Get all reports (hardcoded for now)
            var allReports = GetSampleReports();

            // Apply filters
            var filteredReports = ApplyFilters(allReports);

            // Get unique categories for filter dropdown
            Categories = allReports.Select(r => r.Category).Distinct().OrderBy(c => c).ToList();

            // Calculate statistics from ALL reports (not filtered)
            TotalReports = allReports.Count;
            TotalOpen = allReports.Count(r => r.Status == "Open");
            TotalAssigned = allReports.Count(r => r.Status == "Assigned");
            TotalClosed = allReports.Count(r => r.Status == "Closed");

            // Order by priority (highest first)
            var orderedReports = filteredReports.OrderByDescending(r => r.PriorityScore).ToList();

            // Pagination
            TotalPages = (int)Math.Ceiling(orderedReports.Count / (double)PageSize);
            if (TotalPages == 0) TotalPages = 1;
            if (CurrentPage < 1) CurrentPage = 1;
            if (CurrentPage > TotalPages) CurrentPage = TotalPages;

            Reports = orderedReports
                .Skip((CurrentPage - 1) * PageSize)
                .Take(PageSize)
                .ToList();
        }

        private List<FaultReport> ApplyFilters(List<FaultReport> reports)
        {
            var filtered = reports.AsQueryable();

            // Status filter
            if (!string.IsNullOrEmpty(StatusFilter) && StatusFilter != "All")
            {
                filtered = filtered.Where(r => r.Status == StatusFilter);
            }

            // Category filter
            if (!string.IsNullOrEmpty(CategoryFilter) && CategoryFilter != "All")
            {
                filtered = filtered.Where(r => r.Category == CategoryFilter);
            }

            // Search filter
            if (!string.IsNullOrEmpty(SearchTerm))
            {
                var search = SearchTerm.ToLower();
                filtered = filtered.Where(r =>
                    r.Title.ToLower().Contains(search) ||
                    r.Category.ToLower().Contains(search) ||
                    r.Location.ToLower().Contains(search) ||
                    r.ReportID.ToString().Contains(search)
                );
            }

            return filtered.ToList();
        }

        private List<FaultReport> GetSampleReports()
        {
            // HARDCODED SAMPLE DATA - Replace with database later
            return new List<FaultReport>
            {
                new FaultReport
                {
                    ReportID = 101,
                    Title = "Burst pipe on Main Street",
                    Category = "Water",
                    Status = "Open",
                    PriorityScore = 95,
                    ReportedDate = DateTime.Now.AddHours(-2),
                    Location = "Main Street, Block 5"
                },
                new FaultReport
                {
                    ReportID = 102,
                    Title = "Large pothole on Oak Avenue",
                    Category = "Roads",
                    Status = "Open",
                    PriorityScore = 87,
                    ReportedDate = DateTime.Now.AddDays(-1),
                    Location = "Oak Avenue, near Shoprite"
                },
                new FaultReport
                {
                    ReportID = 103,
                    Title = "Power outage in Ward 3",
                    Category = "Electricity",
                    Status = "Assigned",
                    PriorityScore = 76,
                    ReportedDate = DateTime.Now.AddDays(-3),
                    Location = "Ward 3, Zone B"
                },
                new FaultReport
                {
                    ReportID = 104,
                    Title = "Streetlight not working",
                    Category = "Lighting",
                    Status = "Closed",
                    PriorityScore = 45,
                    ReportedDate = DateTime.Now.AddDays(-7),
                    Location = "Pine Road, Corner of 2nd"
                },
                new FaultReport
                {
                    ReportID = 105,
                    Title = "Sewage spill on Church Street",
                    Category = "Sanitation",
                    Status = "Open",
                    PriorityScore = 92,
                    ReportedDate = DateTime.Now.AddHours(-5),
                    Location = "Church Street, near Mall"
                },
                new FaultReport
                {
                    ReportID = 106,
                    Title = "Broken traffic light at intersection",
                    Category = "Traffic",
                    Status = "Assigned",
                    PriorityScore = 68,
                    ReportedDate = DateTime.Now.AddDays(-2),
                    Location = "Main & Oak Intersection"
                },
                new FaultReport
                {
                    ReportID = 107,
                    Title = "Water leak at community center",
                    Category = "Water",
                    Status = "Closed",
                    PriorityScore = 55,
                    ReportedDate = DateTime.Now.AddDays(-5),
                    Location = "Community Center, Zone A"
                },
                new FaultReport
                {
                    ReportID = 108,
                    Title = "Pothole on Nelson Mandela Blvd",
                    Category = "Roads",
                    Status = "Open",
                    PriorityScore = 82,
                    ReportedDate = DateTime.Now.AddHours(-8),
                    Location = "Nelson Mandela Blvd"
                },
                new FaultReport
                {
                    ReportID = 109,
                    Title = "Streetlight flickering",
                    Category = "Lighting",
                    Status = "Assigned",
                    PriorityScore = 58,
                    ReportedDate = DateTime.Now.AddDays(-4),
                    Location = "Maple Street"
                },
                new FaultReport
                {
                    ReportID = 110,
                    Title = "Electrical transformer issue",
                    Category = "Electricity",
                    Status = "Open",
                    PriorityScore = 88,
                    ReportedDate = DateTime.Now.AddHours(-12),
                    Location = "Industrial Area, Sector 3"
                },
                new FaultReport
                {
                    ReportID = 111,
                    Title = "Blocked storm drain",
                    Category = "Sanitation",
                    Status = "Assigned",
                    PriorityScore = 62,
                    ReportedDate = DateTime.Now.AddDays(-3),
                    Location = "River Road"
                },
                new FaultReport
                {
                    ReportID = 112,
                    Title = "Traffic light timing issue",
                    Category = "Traffic",
                    Status = "Closed",
                    PriorityScore = 48,
                    ReportedDate = DateTime.Now.AddDays(-6),
                    Location = "Church & Main Intersection"
                }
            };
        }
    }

    // Model class for FaultReport (if you don't have it in Models folder)
    public class FaultReport
    {
        public int ReportID { get; set; }
        public string Title { get; set; } = "";
        public string Category { get; set; } = "";
        public string Status { get; set; } = "";
        public int PriorityScore { get; set; }
        public DateTime ReportedDate { get; set; }
        public string Location { get; set; } = "";
    }
}