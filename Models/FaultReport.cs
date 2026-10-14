using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;

[Table("fault_reports")]
public partial class FaultReport
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

    [Column("street_name")]
    public string? StreetName { get; set; }

    [Column("suburb_name")]
    public string? SuburbName { get; set; }

    [Column("city_name")]
    public string? CityName { get; set; }

    [Column("status")]
    public string? Status { get; set; }

    [Column("priority_score")]
    public int? PriorityScore { get; set; }

    [Column("reference_number")]
    public string? ReferenceNumber { get; set; }

    [Column("verified_by")]
    public int? VerifiedBy { get; set; }

    [Column("verified_at")]
    public DateTime? VerifiedAt { get; set; }

    [Column("assigned_to")]
    public int? AssignedTo { get; set; }

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
    public DateTime CreatedAt { get; set; }

    [Column("updated_at")]
    public DateTime? UpdatedAt { get; set; }

    // ─── Geocoding tracking ────────────────────────────────────
    [Column("geocoded_at")]
    public DateTime? GeocodedAt { get; set; }

    [Column("geocode_attempts")]
    public int GeocodeAttempts { get; set; }

    // ─── Navigation properties ─────────────────────────────────
    public virtual CitizenProfile? Citizen { get; set; }
    public virtual FaultCategory? Category { get; set; }
    public virtual ServiceArea? Area { get; set; }

    [ForeignKey(nameof(VerifiedBy))]
    public virtual MunicipalUser? VerifiedByNavigation { get; set; }

    [ForeignKey(nameof(AssignedTo))]
    public virtual MunicipalUser? AssignedToNavigation { get; set; }

    public virtual ICollection<ReportPhoto> ReportPhotos { get; set; } = new List<ReportPhoto>();
    public virtual ICollection<DuplicateReport> DuplicateReportOriginalReports { get; set; } = new List<DuplicateReport>();
    public virtual ICollection<DuplicateReport> DuplicateReportDuplicateReportNavigations { get; set; } = new List<DuplicateReport>();
    public virtual ICollection<PriorityScore> PriorityScores { get; set; } = new List<PriorityScore>();
    public virtual ICollection<FaultStatusHistory> FaultStatusHistories { get; set; } = new List<FaultStatusHistory>();
    public virtual ICollection<ReportFeedback> ReportFeedbacks { get; set; } = new List<ReportFeedback>();
    public virtual ICollection<Notification> Notifications { get; set; } = new List<Notification>();
}