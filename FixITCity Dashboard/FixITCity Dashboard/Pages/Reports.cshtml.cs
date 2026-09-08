using Microsoft.AspNetCore.Mvc.RazorPages;
using System;
using System.Collections.Generic;
using System.Linq;

namespace FixITCity_Dashboard.Pages
{
    public class ReportsModel : PageModel
    {
        // =========================================================
        // FILTER PROPERTIES
        // =========================================================

        public string? SearchTerm { get; set; }

        public string? StatusFilter { get; set; }

        public string? CategoryFilter { get; set; }

        public string? DateSort { get; set; }


        // =========================================================
        // REPORT LIST
        // =========================================================

        public List<FaultReport> Reports { get; set; } = new();


        // =========================================================
        // STATISTICS
        // =========================================================

        public int TotalReports { get; set; }

        public int TotalOpen { get; set; }

        public int TotalAssigned { get; set; }

        public int TotalClosed { get; set; }


        // =========================================================
        // PAGINATION
        // =========================================================

        public int CurrentPage { get; set; } = 1;

        public int TotalPages { get; set; }

        public int PageSize { get; set; } = 10;


        // =========================================================
        // CATEGORIES
        // =========================================================

        public List<string> Categories { get; set; } = new();


        // =========================================================
        // ON GET
        // =========================================================

        public void OnGet(
            string? searchTerm,
            string? statusFilter,
            string? categoryFilter,
            string? dateSort,
            int? page)
        {
            // Store values from query string
            SearchTerm = searchTerm;

            StatusFilter = string.IsNullOrWhiteSpace(statusFilter)
                ? "All"
                : statusFilter;

            CategoryFilter = string.IsNullOrWhiteSpace(categoryFilter)
                ? "All"
                : categoryFilter;

            // Default date sorting = newest first
            DateSort = string.IsNullOrWhiteSpace(dateSort)
                ? "Newest"
                : dateSort;

            CurrentPage = page ?? 1;


            // =====================================================
            // GET REPORTS
            // =====================================================

            var allReports = GetSampleReports();


            // =====================================================
            // POPULATE CATEGORY DROPDOWN
            // =====================================================

            Categories = allReports
                .Select(r => r.Category)
                .Distinct()
                .OrderBy(c => c)
                .ToList();


            // =====================================================
            // STATISTICS
            // Statistics are based on ALL reports
            // =====================================================

            TotalReports = allReports.Count;

            TotalOpen = allReports.Count(r =>
                r.Status == "Open");

            TotalAssigned = allReports.Count(r =>
                r.Status == "Assigned");

            TotalClosed = allReports.Count(r =>
                r.Status == "Closed");


            // =====================================================
            // APPLY ALL FILTERS SIMULTANEOUSLY
            // =====================================================

            var filteredReports = ApplyFilters(allReports);


            // =====================================================
            // APPLY DATE + PRIORITY SORTING
            // =====================================================

            List<FaultReport> orderedReports;

            if (DateSort == "Oldest")
            {
                // Oldest reports first.
                // If two reports have the same date/time,
                // higher priority appears first.
                orderedReports = filteredReports
                    .OrderBy(r => r.ReportedDate)
                    .ThenByDescending(r => r.PriorityScore)
                    .ToList();
            }
            else
            {
                // Default: newest reports first.
                // If two reports have the same date/time,
                // higher priority appears first.
                orderedReports = filteredReports
                    .OrderByDescending(r => r.ReportedDate)
                    .ThenByDescending(r => r.PriorityScore)
                    .ToList();
            }


            // =====================================================
            // PAGINATION
            // =====================================================

            TotalPages = (int)Math.Ceiling(
                orderedReports.Count / (double)PageSize
            );

            if (TotalPages < 1)
            {
                TotalPages = 1;
            }

            if (CurrentPage < 1)
            {
                CurrentPage = 1;
            }

            if (CurrentPage > TotalPages)
            {
                CurrentPage = TotalPages;
            }


            // Get only reports for current page
            Reports = orderedReports
                .Skip((CurrentPage - 1) * PageSize)
                .Take(PageSize)
                .ToList();
        }


        // =========================================================
        // APPLY FILTERS
        // =========================================================

        private List<FaultReport> ApplyFilters(
            List<FaultReport> reports)
        {
            var filtered = reports.AsQueryable();


            // =====================================================
            // STATUS FILTER
            // =====================================================

            if (!string.IsNullOrWhiteSpace(StatusFilter) &&
                StatusFilter != "All")
            {
                filtered = filtered.Where(r =>
                    r.Status == StatusFilter);
            }


            // =====================================================
            // CATEGORY FILTER
            // =====================================================

            if (!string.IsNullOrWhiteSpace(CategoryFilter) &&
                CategoryFilter != "All")
            {
                filtered = filtered.Where(r =>
                    r.Category == CategoryFilter);
            }


            // =====================================================
            // SEARCH FILTER
            // =====================================================

            if (!string.IsNullOrWhiteSpace(SearchTerm))
            {
                string search = SearchTerm.Trim().ToLower();

                filtered = filtered.Where(r =>
                    r.Title.ToLower().Contains(search) ||
                    r.Category.ToLower().Contains(search) ||
                    r.Status.ToLower().Contains(search) ||
                    r.Location.ToLower().Contains(search) ||
                    r.ReportID.ToString().Contains(search)
                );
            }


            return filtered.ToList();
        }


        // =========================================================
        // SAMPLE REPORT DATA
        // =========================================================

        private List<FaultReport> GetSampleReports()
        {
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


    // =============================================================
    // FAULT REPORT MODEL
    // =============================================================

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