using System.Threading.Tasks;
using FixITCity_Dashboard.ViewModels.Dashboard;

namespace FixITCity_Dashboard.Services.Dashboard
{
    public interface IDashboardService
    {
        Task<DashboardViewModel> GetDashboardSummaryAsync();
    }
}