using System;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;

[Table("fault_reports_with_votes")]
public partial class FaultReportWithVote
{
    [Column("report_id")]
    public int ReportId { get; set; }

    [Column("citizen_id")]
    public int CitizenId { get; set; }

    [Column("category_id")]
    public int CategoryId { get; set; }

    [Column("area_id")]
    public int? AreaId { get; set; }

    [Column("title")]
    public string Title { get; set; } = string.Empty;

    [Column("description")]
    public string Description { get; set; } = string.Empty;

    [Column("latitude")]
    public decimal Latitude { get; set; }

    [Column("longitude")]
    public decimal Longitude { get; set; }

    [Column("address_line")]
    public string? AddressLine { get; set; }

    [Column("status")]
    public string Status { get; set; } = string.Empty;

    [Column("priority_score")]
    public int? PriorityScore { get; set; }

    [Column("reference_number")]
    public string ReferenceNumber { get; set; } = string.Empty;

    [Column("verified_by")]
    public long? VerifiedBy { get; set; }

    [Column("verified_at")]
    public DateTime? VerifiedAt { get; set; }

    [Column("assigned_to")]
    public long? AssignedTo { get; set; }

    [Column("resolution_notes")]
    public string? ResolutionNotes { get; set; }

    [Column("resolved_at")]
    public DateTime? ResolvedAt { get; set; }

    [Column("is_duplicate")]
    public bool? IsDuplicate { get; set; }

    [Column("is_offline")]
    public bool? IsOffline { get; set; }

    [Column("sync_status")]
    public string? SyncStatus { get; set; }

    [Column("offline_id")]
    public string? OfflineId { get; set; }

    [Column("created_at")]
    public DateTime? CreatedAt { get; set; }

    [Column("updated_at")]
    public DateTime? UpdatedAt { get; set; }

    [Column("vote_count")]
    public int VoteCount { get; set; }
}