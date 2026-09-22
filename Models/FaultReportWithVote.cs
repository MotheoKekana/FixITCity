using System;

namespace FixITCityAPI.Models;

public partial class FaultReportWithVote
{
    public long ReportId { get; set; }
    public long CitizenId { get; set; }
    public long CategoryId { get; set; }
    public long? AreaId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public decimal Latitude { get; set; }
    public decimal Longitude { get; set; }
    public string? AddressLine { get; set; }
    public string Status { get; set; } = string.Empty;
    public int? PriorityScore { get; set; }
    public string ReferenceNumber { get; set; } = string.Empty;
    public long? VerifiedBy { get; set; }
    public DateTime? VerifiedAt { get; set; }
    public long? AssignedTo { get; set; }
    public string? ResolutionNotes { get; set; }
    public DateTime? ResolvedAt { get; set; }
    public bool? IsDuplicate { get; set; }
    public bool? IsOffline { get; set; }
    public string? SyncStatus { get; set; }
    public string? OfflineId { get; set; }
    public DateTime? CreatedAt { get; set; }
    public DateTime? UpdatedAt { get; set; }
    public int VoteCount { get; set; }
}