namespace FixITCity_Dashboard.ViewModels
{
    public class DashboardViewModel
    {
        public DashboardStatisticsViewModel Statistics { get; set; } = new();
        public CategoryStatisticsViewModel Categories { get; set; } = new();
        public ActivityStatisticsViewModel Activity { get; set; } = new();
        public EmergencySummaryViewModel Emergencies { get; set; } = new();
        public List<DashboardAlertViewModel> Alerts { get; set; } = new();
        public List<MapSummaryViewModel> MapPoints { get; set; } = new();
    }

    public class DashboardStatisticsViewModel
    {
        public int ApprovedReports { get; set; }
        public int PendingApproval { get; set; }
        public int UnresolvedReports { get; set; }
        public int EmergencyReports { get; set; }
    }

    public class MapSummaryViewModel
    {
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string Type { get; set; } = string.Empty;
    }

    public class CategoryStatistic
    {
        public string Category { get; set; } = string.Empty;
        public int Count { get; set; }
    }

    public class CategoryStatisticsViewModel
    {
        public List<CategoryStatistic> Distribution { get; set; } = new();
    }

    public class ActivityStatistic
    {
        public string Date { get; set; } = string.Empty;
        public int Count { get; set; }
    }

    public class ActivityStatisticsViewModel
    {
        public List<ActivityStatistic> Trends { get; set; } = new();
    }

    public class EmergencySummaryViewModel
    {
        public int TotalEmergencies { get; set; }
        public Dictionary<string, int> BreakDown { get; set; } = new();
    }

    public class DashboardAlertViewModel
    {
        public string Message { get; set; } = string.Empty;
        public string Type { get; set; } = "Info"; // Info, Warning, Danger
    }
}