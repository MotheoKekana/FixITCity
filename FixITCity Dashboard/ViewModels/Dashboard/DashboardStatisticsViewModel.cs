namespace FixITCity_Dashboard.ViewModels.Dashboard
{
    public class DashboardStatisticsViewModel
    {
        public int ApprovedCount { get; set; }
        public int PendingApprovalCount { get; set; }
        public int UnresolvedCount { get; set; }
        public int EmergencyCount { get; set; }
    }
}