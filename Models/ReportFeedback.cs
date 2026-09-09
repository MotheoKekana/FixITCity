using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;
[Table("report_feedback")]
public partial class ReportFeedback
{
    public int FeedbackId { get; set; }

    public int? ReportId { get; set; }

    public int? CitizenId { get; set; }

    public int? Rating { get; set; }

    public string? Comment { get; set; }

    public DateTime? CreatedAt { get; set; }

    public virtual CitizenProfile? Citizen { get; set; }

    public virtual FaultReport? Report { get; set; }
}
