using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;
[Table("citizen_profiles")]
public partial class CitizenProfile
{
    public int CitizenProfileId { get; set; }

    public int? UserId { get; set; }

    public string? FirstName { get; set; }

    public string? LastName { get; set; }

    public string? PhoneNumber { get; set; }

    public string? AddressLine { get; set; }

    public string? City { get; set; }

    public decimal? Latitude { get; set; }

    public decimal? Longitude { get; set; }

    public string NotificationPreference { get; set; }  // notification_preference
    

    public DateTime? CreatedAt { get; set; }            // created_at
    
    public DateTime? UpdatedAt { get; set; }            // updated_at

    public virtual ICollection<FaultReport> FaultReports { get; set; } = new List<FaultReport>();

    public virtual ICollection<Notification> Notifications { get; set; } = new List<Notification>();

    public virtual ICollection<ReportFeedback> ReportFeedbacks { get; set; } = new List<ReportFeedback>();

    public virtual User? User { get; set; }
}
