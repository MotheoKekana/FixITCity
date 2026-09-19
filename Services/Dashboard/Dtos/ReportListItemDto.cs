namespace FixITCity_Dashboard.Services.Dashboard.Dtos
{
    public class ReportListItemDto
    {
        public int ReportId { get; set; }
        public string Title { get; set; } = string.Empty;
        public string Description { get; set; } = string.Empty;
        public string Status { get; set; } = string.Empty;
        public int? PriorityScore { get; set; }
        public string ReferenceNumber { get; set; } = string.Empty;
        public double Latitude { get; set; }
        public double Longitude { get; set; }
        public DateTime? CreatedAt { get; set; }
        public DateTime? ResolvedAt { get; set; }
        public string? CitizenName { get; set; }
        public string? CitizenPhone { get; set; }
        public string? CategoryName { get; set; }
        public string? AreaName { get; set; }
    }
}