using FixITCity_Dashboard.ViewModels.Dashboard;


namespace FixITCity_Dashboard.Services
{
    public interface IDashboardService
    {

        Task<DashboardViewModel> GetDashboardSummaryAsync();
    }
}