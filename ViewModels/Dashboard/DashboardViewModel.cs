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
        public List<DashboardAlertViewModel> Alerts { get; set; } = new();
    }

    public class DashboardStatisticsViewModel
    {
        public int Total { get; set; }
        public int Pending { get; set; }
        public int UnderReview { get; set; }
        public int Accepted { get; set; }
        public int InProgress { get; set; }
        public int Fixed { get; set; }
        public int Rejected { get; set; }
    }

    public class MapSummaryViewModel
    {
        public List<MapPointViewModel> MapPoints { get; set; } = new();

        // IMPORTANT: Your view uses these for the map center, so we add them here:
        public double CenterLatitude { get; set; } = -33.9608;
        public double CenterLongitude { get; set; } = 25.6022;
    }

    public class MapPointViewModel
    {
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public string Title { get; set; }
        public string Description { get; set; }
        public bool IsEmergency { get; set; }
    }

    public class CategoryStatisticsViewModel
    {
        public Dictionary<string, int> ReportsByCategory { get; set; } = new();
    }

    public class ActivityStatisticsViewModel
    {
        public Dictionary<string, int> ReportsByDay { get; set; } = new();
    }

    public class EmergencySummaryViewModel
    {
        public int TotalEmergencies { get; set; }
        public Dictionary<string, int> EmergenciesByCategory { get; set; } = new();
    }

    public class DashboardAlertViewModel
    {
        public string Message { get; set; }
        public string Type { get; set; }
    }
}