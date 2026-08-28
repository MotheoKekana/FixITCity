using System.Collections.Generic;

namespace FixITCity_Dashboard.ViewModels.Dashboard
{
    public class CategoryStatisticsViewModel
    {
        public Dictionary<string, int> ReportsByCategory { get; set; } = new();
    }
}