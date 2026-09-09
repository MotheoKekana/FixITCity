using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations.Schema;

namespace FixITCityAPI.Models;
[Table("report_photos")]
public partial class ReportPhoto
{
    public int PhotoId { get; set; }

    public int? ReportId { get; set; }

    public string? PhotoUrl { get; set; }

    public DateTime? UploadedAt { get; set; }

    public virtual FaultReport? Report { get; set; }
}
