using System.Collections.Generic;

namespace FixITCity_Dashboard.ViewModels.Dashboard
{
    public class ActivityStatisticsViewModel
    {
        public Dictionary<string, int> ReportsByDay { get; set; } = new();
    }
}