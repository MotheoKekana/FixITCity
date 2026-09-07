using System;
using System.Collections.Generic;

namespace FixITCityAPI.Models;

public partial class FaultReport
{
    public int ReportId { get; set; }
    public int CitizenId { get; set; }
    public int CategoryId { get; set; }
    public int? AreaId { get; set; }
    public string Title { get; set; }
    public string Description { get; set; }
    public decimal Latitude { get; set; }
    public decimal Longitude { get; set; }
    
    // ========== ADD THESE MISSING PROPERTIES ==========
    public string AddressLine { get; set; }          // address_line
    public string Status { get; set; }               // status
    public int? PriorityScore { get; set; }          // priority_score
    public string ReferenceNumber { get; set; }      // reference_number
    public int? VerifiedBy { get; set; }             // verified_by
    public DateTime? VerifiedAt { get; set; }        // verified_at
    public int? AssignedTo { get; set; }             // assigned_to
    public string ResolutionNotes { get; set; }      // resolution_notes
    public DateTime? ResolvedAt { get; set; }        // resolved_at
    public bool? IsDuplicate { get; set; }           // is_duplicate
    public bool? IsOffline { get; set; }             // is_offline
    public string SyncStatus { get; set; }           // sync_status
    public string OfflineId { get; set; }            // offline_id
    public DateTime? UpdatedAt { get; set; }         // updated_at
    // =================================================

    public DateTime CreatedAt { get; set; }

    // Navigation properties
    public virtual CitizenProfile Citizen { get; set; }
    public virtual FaultCategory Category { get; set; }
    public virtual ServiceArea Area { get; set; }
    public virtual MunicipalUser VerifiedByNavigation { get; set; }
    public virtual MunicipalUser AssignedToNavigation { get; set; }
    public virtual ICollection<ReportPhoto> ReportPhotos { get; set; }
    public virtual ICollection<DuplicateReport> DuplicateReportOriginalReports { get; set; }
    public virtual ICollection<DuplicateReport> DuplicateReportDuplicateReportNavigations { get; set; }
    public virtual ICollection<PriorityScore> PriorityScores { get; set; }
    public virtual ICollection<FaultStatusHistory> FaultStatusHistories { get; set; }
    public virtual ICollection<ReportFeedback> ReportFeedbacks { get; set; }
    public virtual ICollection<Notification> Notifications { get; set; }
}