using FixITCity_Dashboard.Services;
using FixITCity_Dashboard.Services.Dashboard;
using FixITCity_Dashboard.ViewModels.Dashboard;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.Globalization;

namespace FixITCity_Dashboard.Pages
{
    public class DashboardModel : PageModel
    {
        private readonly IDashboardService _dashboardService;

        public DashboardModel(IDashboardService dashboardService)
        {
            _dashboardService = dashboardService;
        }

        // Must match Model.DashboardData in Dashboard.cshtml
        public DashboardViewModel DashboardData { get; private set; } = new();

        // Name displayed in the dashboard greeting
        public string DisplayName { get; private set; } = "Admin";

        public async Task<IActionResult> OnGetAsync()
        {
            // Ensure the user is logged in
            if (string.IsNullOrEmpty(
                HttpContext.Session.GetString("JwtToken")))
            {
                return RedirectToPage("/Login");
            }

            // Read the latest name stored in the session
            DisplayName = BuildDisplayName();

            // Load dashboard statistics and report data
            DashboardData =
                await _dashboardService.GetDashboardSummaryAsync();

            return Page();
        }

        private string BuildDisplayName()
        {
            // Prefer the display name saved by Settings
            var fullName =
                HttpContext.Session.GetString("DisplayName");

            if (!string.IsNullOrWhiteSpace(fullName))
            {
                return fullName.Trim();
            }

            // Fall back to the full name saved by Settings
            fullName =
                HttpContext.Session.GetString("FullName");

            if (!string.IsNullOrWhiteSpace(fullName))
            {
                return fullName.Trim();
            }

            // Fall back to the username from the login session
            var username =
                HttpContext.Session.GetString("Username");

            if (string.IsNullOrWhiteSpace(username))
            {
                return "Admin";
            }

            var name = username
                .Split('@')[0]
                .Replace('.', ' ')
                .Replace('_', ' ')
                .Trim();

            if (name.Length == 0)
            {
                return "Admin";
            }

            return CultureInfo.CurrentCulture
                .TextInfo
                .ToTitleCase(name);
        }
    }
}
