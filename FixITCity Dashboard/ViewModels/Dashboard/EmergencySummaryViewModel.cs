using System.Collections.Generic;

namespace FixITCity_Dashboard.ViewModels.Dashboard
{
    public class EmergencySummaryViewModel
    {
        public int TotalEmergencies { get; set; }
        public Dictionary<string, int> EmergenciesByCategory { get; set; } = new();
    }
}