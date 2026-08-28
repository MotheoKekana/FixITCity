using System.Collections.Generic;

namespace FixITCity_Dashboard.ViewModels.Dashboard
{
    public class MapPointViewModel
    {
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public bool IsEmergency { get; set; }
    }

    public class MapSummaryViewModel
    {
        // Central coordinates for Port Elizabeth
        public double CenterLatitude { get; set; } = -33.9608;
        public double CenterLongitude { get; set; } = 25.6022;
        public List<MapPointViewModel> MapPoints { get; set; } = new();
    }
}