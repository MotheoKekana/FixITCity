namespace FixITCity_Dashboard.ViewModels.Dashboard
{
    public class DashboardAlertViewModel
    {
        public string Message { get; set; } = string.Empty;
        public string Type { get; set; } = "info"; // info, warning, danger
    }
}