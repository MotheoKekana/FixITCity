using System.Collections.Generic;

namespace FixITCity_Dashboard.ViewModels.Dashboard
{
    public class DashboardViewModel
    {
        public DashboardStatisticsViewModel Statistics { get; set; } = new();
        public MapSummaryViewModel Map { get; set; } = new();
        public CategoryStatisticsViewModel Categories { get; set; } = new();
        public ActivityStatisticsViewModel Activity { get; set; } = new();
        public EmergencySummaryViewModel Emergencies { get; set; } = new();
        public IEnumerable<DashboardAlertViewModel> Alerts { get; set; } = new List<DashboardAlertViewModel>();
    }
}