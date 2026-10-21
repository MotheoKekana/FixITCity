using FixITCity_Dashboard.Services;
using FixITCity_Dashboard.ViewModels.Dashboard;
using Microsoft.AspNetCore.Mvc.RazorPages;
using System.Threading.Tasks;

namespace FixITCity_Dashboard.Pages
{
    public class DashboardModel : PageModel
    {
        private readonly IDashboardService _dashboardService;

        public DashboardModel(IDashboardService dashboardService)
        {
            _dashboardService = dashboardService;
        }

        public DashboardViewModel DashboardData { get; set; } = new();

        public async Task OnGetAsync()
        {
            DashboardData = await _dashboardService.GetDashboardSummaryAsync();
        }
    }
}