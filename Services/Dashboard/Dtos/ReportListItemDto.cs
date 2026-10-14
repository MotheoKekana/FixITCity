namespace FixITCity_Dashboard.Services.Dashboard.Dtos;

public class ReportListItemDto
{
    public int ReportId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string? CategoryName { get; set; }
    public string? Status { get; set; }
    public int? PriorityScore { get; set; }
    public DateTime? CreatedAt { get; set; }
    public string? ReferenceNumber { get; set; }

    // Location
    public string? AreaName { get; set; }        // existing
    public string? StreetName { get; set; }      // NEW
    public string? SuburbName { get; set; }      // NEW
    public string? CityName { get; set; }        // NEW
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public string? Description { get; set; }
}